package com.logAnalyzer.parser.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logAnalyzer.parser.ai.entity.AiResult;
import com.logAnalyzer.parser.ai.enums.AiFeatureEnum;
import com.logAnalyzer.parser.ai.enums.LlmProviderEnum;
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
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.exception.AiResponseParseException;
import com.logAnalyzer.parser.repository.LogEntryEsRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final LlmFactory llmFactory;
    private final AiResultRepository aiResultRepository;
    private final LogEntryEsRepository logEntryEsRepository;
    private final ObjectMapper objectMapper;
    private final ElasticsearchOperations elasticsearchOperations;

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
            Convert the user's natural language query into structured filters.
        
            Current timestamp: %s
        
            Respond ONLY in this JSON format, no extra text:
            {
                "keywords": ["specific", "search", "terms"],
                "level": ["ERROR" , "WARN" , "INFO" , "DEBUG" , null],
                "timeFrom": "ISO-8601 timestamp or null",
                "timeTo": "ISO-8601 timestamp or null",
                "className": "specific class/service name or null",
                "explanation": "one sentence describing your interpretation"
            }
        
            Rules:
            - "last night" = yesterday 18:00 to today 06:00
            - "today" = today 00:00 to current time
            - "yesterday" = yesterday 00:00 to yesterday 23:59
            - keywords = specific technical terms only (e.g. "database", "timeout", "NullPointerException")
              do NOT include common words like "show", "find", "errors", "logs"
            - level = null if user didn't specify a severity
            - className = null unless user mentions a specific class or service name
            """;

    @Override
    public SummaryResponse summarizeLogs(String sessionId, LlmRequest request) {

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
    public DiagnosisResponse analyse(String sessionId, LlmRequest request) throws AiResponseParseException {
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
    public NlQueryResponse search(String sessionId, LlmRequest request) throws AiResponseParseException{
        String systemPrompt = NLQ_SYSTEM_PROMPT.formatted(LocalDateTime.now());
        request.setSystemPrompt(systemPrompt);
        request.setMaxTokens(300);
        request.setTemperature(0.0f);   // fully deterministic — same query = same filter

        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String rawJson = provider.complete(request);
        InterpretedFilter filter = deserialize(rawJson, InterpretedFilter.class);

        Query esQuery = buildElasticsearchQuery(sessionId, filter);

        SearchHits<LogEntryDocument> hits = elasticsearchOperations.search(esQuery, LogEntryDocument.class);

        return NlQueryResponse.builder()
                .sessionId(sessionId)
                .originalQuery(request.getUserPrompt())
                .interpretedFilter(filter)
                .results(hits.stream().map(SearchHit::getContent).toList())
                .totalHits(hits.getTotalHits())
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    private Query buildElasticsearchQuery(String sessionId, InterpretedFilter filter) {

        Criteria criteria = Criteria.where("sessionId").is(sessionId);

        // Level filter — validate against enum first, ignore if invalid
        if (filter.getLevel() != null) {
            for(String level : filter.getLevel()) {
                if(isValidLevel(level))
                criteria = criteria.and("level").is(level);
            }
        }

        // Keyword search — OR across message field
        if (filter.getKeywords() != null && !filter.getKeywords().isEmpty()) {
            Criteria keywordCriteria = null;
            for (String keyword : filter.getKeywords()) {
                Criteria c = Criteria.where("message").matches(keyword);
                keywordCriteria = (keywordCriteria == null) ? c : keywordCriteria.or(c);
            }
            criteria = criteria.and(keywordCriteria);
        }

        // Time range
        if (filter.getTimeFrom() != null && filter.getTimeTo() != null) {
            criteria = criteria.and("logTimestamp")
                    .greaterThanEqual(filter.getTimeFrom())
                    .lessThanEqual(filter.getTimeTo());
        }

        // Class/service name
        if (filter.getClassName() != null) {
            criteria = criteria.and("className").matches(filter.getClassName());
        }

        return new CriteriaQuery(criteria).setPageable(PageRequest.of(0, 100));
    }

    private boolean isValidLevel(String level) {
        return Set.of("ERROR", "WARN", "INFO", "DEBUG").contains(level.toUpperCase());
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
}
