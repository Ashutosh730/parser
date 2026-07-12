package com.logAnalyzer.parser.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logAnalyzer.parser.ai.entity.AiResult;
import com.logAnalyzer.parser.ai.enums.AiFeatureEnum;
import com.logAnalyzer.parser.ai.enums.QueryIntent;
import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.DiagnosisResponse;
import com.logAnalyzer.parser.ai.model.InterpretedFilter;
import com.logAnalyzer.parser.ai.model.NlQueryResponse;
import com.logAnalyzer.parser.ai.model.SummaryResponse;
import com.logAnalyzer.parser.ai.provider.LLMProvider;
import com.logAnalyzer.parser.ai.provider.LlmFactory;
import com.logAnalyzer.parser.ai.repository.AiResultRepository;
import com.logAnalyzer.parser.ai.service.AIService;
import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.entity.LogSession;
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.exception.AiResponseParseException;
import com.logAnalyzer.parser.exception.SessionNotFoundException;
import com.logAnalyzer.parser.repository.LogEntryCustomEsRepository;
import com.logAnalyzer.parser.repository.LogEntryEsRepository;
import com.logAnalyzer.parser.repository.LogSessionRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final LlmFactory llmFactory;
    private final AiResultRepository aiResultRepository;
    private final LogSessionRepository sessionRepository;
    private final LogEntryEsRepository logEntryEsRepository;
    private final ObjectMapper objectMapper;
    private final LogEntryCustomEsRepository logEntryCustomEsRepository;

    private static final String SUMMARY_PROMPT = """
             You are an expert log analyst.\s
             Analyse the provided log errors and give a concise plain-English summary.
             Include: what went wrong, when it started, how many times it occurred.
             Keep it under 150 words. Be specific, not generic.
            \s""";

    private static final String DIAGNOSIS_SYSTEM_PROMPT = """
            You are a senior backend engineer analysing production log errors.
            Identify root causes AND provide actionable fixes for each.
            
            Respond ONLY in the following JSON format, no extra text:
            {
                "rootCauses": [
                    {
                        "cause": "specific root cause description",
                        "confidence": 0.9,
                        "affectedComponents": ["ServiceName", "ClassName"],
                        "firstOccurrence": "timestamp",
                        "frequency": 12,
                        "fixes": [
                            {
                                "suggestion": "specific actionable fix",
                                "category": "CONFIGURATION | CODE_CHANGE | INFRASTRUCTURE | MONITORING",
                                "priority": "HIGH | MEDIUM | LOW"
                            }
                        ]
                    }
                ],
                "timeline": "brief description of how the failure progressed",
                "impactSummary": "what was the impact on the system",
                "preventiveActions": ["general advice to prevent similar issues"]
            }
            
            Rules:
            - Maximum 3 root causes, ordered by confidence descending
            - Each root cause has 1-2 specific fixes directly addressing it
            - Be specific — "Increase HikariCP maximum-pool-size to 20" not "increase pool size"
            - Maximum 3 prevention tips
            """;

    private static final String NLQ_SYSTEM_PROMPT = """
            You are a query translator for a log search system.
            First determine the QUERY INTENT, then extract relevant fields.
            
            Current timestamp: %s
            
            Respond ONLY in this JSON format, no extra text:
            {
                "intent": "SEARCH" | "AGGREGATION | "UNSUPPORTED | "FACT_EXTRACTION",
                "keywords": ["specific", "search", "terms"],
                "levels": ["ERROR", "WARN", "INFO", "DEBUG", null],
                "timeFrom": "ISO-8601 timestamp or null",
                "timeTo": "ISO-8601 timestamp or null",
                "className": "specific class/service name or null",
                "aggregationType": "COUNT_BY_LEVEL" | "TOP_ERRORS" | "ERROR_TIMELINE" | null,
                "explanation": "one sentence describing your interpretation"
            }
            
            Intent rules:
            - SEARCH = user wants to see specific log entries
              e.g. "show me database errors", "find NPE exceptions", "errors from last night"
            - AGGREGATION = user wants counts, stats, or summaries
              e.g. "how many errors", "count of warn and error", "top errors", "error trend"
            - FACT_EXTRACTION = user wants a specific fact answered directly
                  e.g. "which port is running", "when did app start",
                       "what database is connected", "which Java version"
            - UNSUPPORTED = destructive action or out of scope
              e.g. "delete logs", "fix this error", "compare with yesterday's file"
            
            Keyword rules:
            - SEARCH intent: extract specific technical terms only
              e.g. "database", "timeout", "NullPointerException", "connection refused"
              do NOT include: "show", "find", "get", "errors", "logs", "warnings"
            - AGGREGATION intent: keywords must always be empty []
              aggregation queries don't filter by keyword — they count everything
            
            Levels rules:
            - Always return as array — ["ERROR"] not "ERROR"
            - Empty array [] if user didn't specify any level
            - Valid values: "ERROR", "WARN", "INFO", "DEBUG"
            - "errors and warnings" → ["ERROR", "WARN"]
            - "everything except debug" → ["ERROR", "WARN", "INFO"]
            - "all logs" or no mention → []
            - AGGREGATION intent: still extract levels if mentioned
              "count of errors and warnings" → levels: ["ERROR", "WARN"]
              "count of all levels" → levels: []
            
            Time rules:
            - "last night" = yesterday 18:00:00 to today 06:00:00
            - "today" = today 00:00:00 to current timestamp
            - "yesterday" = yesterday 00:00:00 to yesterday 23:59:59
            - "last hour" = current timestamp minus 1 hour to current timestamp
            - "last 30 minutes" = current timestamp minus 30 minutes to current timestamp
            - null if no time mentioned
            
            AggregationType rules (only when intent = AGGREGATION):
            - "COUNT_BY_LEVEL" = user wants counts grouped by level
              e.g. "count of errors and warnings", "how many errors", "breakdown of log levels"
            - "TOP_ERRORS" = user wants most frequent error messages
              e.g. "top errors", "most common errors", "frequent errors"
            - "ERROR_TIMELINE" = user wants error counts over time
              e.g. "error trend", "errors per hour", "error timeline"
            - null if intent is SEARCH or UNSUPPORTED
            
            className rules:
            - Extract only if user mentions a specific class or service name
              e.g. "UserService", "PaymentController", "com.example.OrderService"
            - null otherwise
            """;

    private final String NLQ_FACT_EXTRACTION_SYSTEM_PROMPT = """
            You are a log analyst.
            Answer the user's question directly.
            and concisely based only on the provided log entries.
            Give a one or two sentence direct answer.
            Include the specific value (port number, version, etc.) in your answer.
            If the answer is not found in the logs, say exactly:
            "This information was not found in the log entries."
            Do not guess or use information outside the provided logs.
            """;

    @Override
    public SummaryResponse summarizeLogs(String sessionId, String userId, LlmRequest request) {
        validateSessionOwnership(sessionId, userId);

        AiResult cache = getCached(sessionId, request, AiFeatureEnum.SUMMARY);
        if (cache != null) {
            return SummaryResponse.builder()
                    .sessionId(sessionId)
                    .summary(cache.getResult())
                    .cached(true)
                    .provider(cache.getProvider().name())
                    .model(cache.getModel())
                    .build();
        }

        List<LogEntryDocument> errors = logEntryEsRepository
                .findBySessionIdAndLevelIn(sessionId, List.of(LogLevel.ERROR), PageRequest.of(0, 50));
        if (errors.isEmpty()) {
            cacheAiResponse(sessionId, request, "No errors found in this log file.", AiFeatureEnum.SUMMARY);
            return SummaryResponse.builder()
                    .sessionId(sessionId)
                    .summary("No errors found in this log file.")
                    .cached(false)
                    .provider(request.getProvider().name())
                    .model(request.getModel())
                    .build();
        }

        request.setSystemPrompt(SUMMARY_PROMPT);
        request.setUserPrompt(buildUserPrompt(errors));
        request.setMaxTokens(300);
        request.setTemperature(0.3f);

        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String summary = provider.complete(request);

        cacheAiResponse(sessionId, request, summary, AiFeatureEnum.SUMMARY);
        return SummaryResponse.builder()
                .sessionId(sessionId)
                .summary(summary)
                .cached(false)
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    @Override
    public DiagnosisResponse analyse(String sessionId, String userId, LlmRequest request) throws AiResponseParseException {
        validateSessionOwnership(sessionId, userId);

        AiResult cache = getCached(sessionId, request, AiFeatureEnum.DIAGNOSIS);
        if (cache != null) {
            DiagnosisResponse response = deserialize(cache.getResult(), DiagnosisResponse.class);
            response.setSessionId(sessionId);
            response.setModel(request.getModel());
            response.setProvider(request.getProvider().name());
            response.setCached(true);
            return response;
        }

        List<LogEntryDocument> errors = logEntryEsRepository.findBySessionIdAndLevelIn(sessionId,
                List.of(LogLevel.ERROR, LogLevel.WARN), PageRequest.of(0, 80));
        if (errors.isEmpty()) {
            cacheAiResponse(sessionId, request, "No errors found in this log file.", AiFeatureEnum.DIAGNOSIS);
            return DiagnosisResponse.builder()
                    .rootCauses(List.of())
                    .timeline("No errors found in this log file.")
                    .impactSummary("No errors found in this log file.")
                    .cached(false)
                    .provider(request.getProvider().name())
                    .model(request.getModel())
                    .build();
        }

        request.setSystemPrompt(DIAGNOSIS_SYSTEM_PROMPT);
        request.setUserPrompt(buildUserPrompt(errors));
        request.setMaxTokens(500);      // more than summary — structured JSON needs space
        request.setTemperature(0.1f);   // very low — factual, deterministic output

        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String rawJson = provider.complete(request);
        DiagnosisResponse response = deserialize(rawJson, DiagnosisResponse.class);

        response.setSessionId(sessionId);
        response.setModel(request.getModel());
        response.setProvider(request.getProvider().name());
        response.setCached(false);

        cacheAiResponse(sessionId, request, rawJson, AiFeatureEnum.DIAGNOSIS);
        return response;
    }

    @Override
    public NlQueryResponse queryProcessor(String sessionId, String userId, LlmRequest request) throws AiResponseParseException {
        validateSessionOwnership(sessionId, userId);

        String systemPrompt = NLQ_SYSTEM_PROMPT.formatted(LocalDateTime.now());
        request.setSystemPrompt(systemPrompt);
        request.setMaxTokens(300);
        request.setTemperature(0.0f);   // fully deterministic — same query = same filter

        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String rawJson = provider.complete(request);
        InterpretedFilter filter = deserialize(rawJson, InterpretedFilter.class);

        return switch (filter.getIntent()) {
            case "SEARCH"             -> handleSearch(sessionId, request, filter);
            case "AGGREGATION"        -> handleAggregation(sessionId, request, filter);
            case "FACT_EXTRACTION"    -> handleFactExtraction(sessionId, request, filter);
            default                             -> handleUnsupported(request);
        };
    }

    private NlQueryResponse handleAggregation(String sessionId, LlmRequest request, InterpretedFilter filter) {
        log.info("User query interpreted as AGGREGATION: {}", filter);
        Object aggregationResult = switch (filter.getAggregationType()) {
            case "COUNT_BY_LEVEL" ->
                    logEntryCustomEsRepository.getLevelDistribution(sessionId, filter.getLevels());  // reuse Phase 1 API
//            case "TOP_ERRORS" -> logEntryCustomEsRepository.getTopErrors(sessionId, 10);          // reuse Phase 1 API
            case "ERROR_TIMELINE" -> logEntryCustomEsRepository.getErrorTimeline(sessionId);
            default -> logEntryCustomEsRepository.getLevelDistribution(sessionId, filter.getLevels());
        };

        return NlQueryResponse.builder()
                .sessionId(sessionId)
                .originalQuery(request.getUserPrompt())
                .interpretedFilter(filter)
                .intent(QueryIntent.AGGREGATION.name())
                .aggregationResult(aggregationResult)   // new field — holds counts/stats
                .results(null)                          // no documents for aggregation
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    private NlQueryResponse handleSearch(String sessionId, LlmRequest request, InterpretedFilter filter) {
        log.info("User query interpreted as SEARCH: {}", filter);
        SearchHits<LogEntryDocument> hits = logEntryCustomEsRepository.getSearchResult(sessionId, filter);
        return NlQueryResponse.builder()
                .sessionId(sessionId)
                .originalQuery(request.getUserPrompt())
                .interpretedFilter(filter)
                .intent(QueryIntent.SEARCH.name())
                .results(hits.stream().map(SearchHit::getContent).toList())
                .totalHits(hits.getTotalHits())
                .aggregationResult(null)
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    private NlQueryResponse handleFactExtraction(String sessionId, LlmRequest request, InterpretedFilter filter) {

        // 1. Search ES for relevant lines using keywords from filter
        //    If no keywords extracted, use broad search — first 30 INFO lines
        //    (startup info like port, PID, version is usually INFO level)
        List<LogEntryDocument> relevantLogs;

        if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
            relevantLogs = logEntryCustomEsRepository.findBySessionIdAndKeywords(sessionId, filter.getKeywords(), PageRequest.of(0, 20));
        } else {
            // fallback — fetch first 30 lines, fact is likely in startup logs
            relevantLogs = logEntryEsRepository.findBySessionIdOrderByLogTimestampAsc(sessionId, PageRequest.of(0, 30));
        }

        if (relevantLogs.isEmpty()) {
            return NlQueryResponse.builder()
                    .sessionId(sessionId)
                    .originalQuery(request.getUserPrompt())
                    .intent(QueryIntent.FACT_EXTRACTION.name())
                    .factAnswer("Could not find relevant log entries to answer this question.")
                    .provider(request.getProvider().name())
                    .model(request.getModel())
                    .build();
        }

        // 2. Build log context string
        String logContext = relevantLogs.stream()
                .map(log -> "[" + log.getLevel() + "] " + log.getMessage())
                .collect(Collectors.joining("\n"));

        String initialUserPrompt = request.getUserPrompt();
        request.setSystemPrompt(NLQ_FACT_EXTRACTION_SYSTEM_PROMPT);
        request.setMaxTokens(150);
        request.setTemperature(0.0f);
        request.setUserPrompt(request.getUserPrompt() + "\n\nLog Context:\n" + logContext);
        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String answer = provider.complete(request);

        return NlQueryResponse.builder()
                .sessionId(sessionId)
                .originalQuery(initialUserPrompt)
                .intent(QueryIntent.FACT_EXTRACTION.name())
                .interpretedFilter(filter)
                .factAnswer(answer)
                .results(null)
                .aggregationResult(null)
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    private NlQueryResponse handleUnsupported(LlmRequest request) {
        return NlQueryResponse.builder()
                .sessionId(null)
                .originalQuery(request.getUserPrompt())
                .intent("UNSUPPORTED")
                .interpretedFilter(null)
                .results(null)
                .aggregationResult(null)
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    private <T> T deserialize(String json, Class<T> targetClass) throws AiResponseParseException {
        try {
            // Strip markdown code blocks if LLM wraps response in ```json
            String clean = json.replaceAll("```json|```", "").trim();

            // This returns an instance of T, matching the new method return type
            return objectMapper.readValue(clean, targetClass);
        } catch (JsonProcessingException e) {
            throw new AiResponseParseException("Failed to parse root cause response", e);
        }
    }

    private void cacheAiResponse(String sessionId, LlmRequest request, String summary, AiFeatureEnum feature) {
        AiResult result = AiResult.builder()
                .sessionId(sessionId)
                .feature(feature)
                .provider(request.getProvider())
                .model(request.getModel())
                .result(summary)
                .tokensUsed(request.getMaxTokens())
                .build();
        aiResultRepository.save(result);
    }

    private AiResult getCached(String sessionId, LlmRequest request, AiFeatureEnum feature) {
        Optional<AiResult> cached = aiResultRepository.findBySessionIdAndFeature(sessionId, feature)
                .stream()
                .filter(aiResult ->
                        aiResult.getProvider().equals(request.getProvider()) && aiResult.getModel().equals(request.getModel())).findAny();
        return cached.orElse(null);
    }

    private String buildUserPrompt(List<LogEntryDocument> errors) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse these log errors:\n\n");

        errors.forEach(e -> {
            sb.append("LEVEL: ").append(e.getLevel()).append("\n");
            sb.append("TIME: ").append(e.getLogTimestamp()).append("\n");
            sb.append("MESSAGE: ").append(e.getMessage()).append("\n");
            sb.append("---\n");
        });

        return sb.toString();
    }

    private void validateSessionOwnership(String sessionId, String userId) {
        LogSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(
                        "Session not found: " + sessionId));
    }
}
