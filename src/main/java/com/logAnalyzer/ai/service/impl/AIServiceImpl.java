package com.logAnalyzer.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logAnalyzer.ai.entity.AiResult;
import com.logAnalyzer.ai.enums.AiFeatureEnum;
import com.logAnalyzer.ai.enums.AiPromptKey;
import com.logAnalyzer.ai.enums.QueryIntent;
import com.logAnalyzer.ai.model.LLMRequest;
import com.logAnalyzer.ai.model.DiagnosisResponse;
import com.logAnalyzer.ai.model.InterpretedFilter;
import com.logAnalyzer.ai.model.NlQueryResponse;
import com.logAnalyzer.ai.model.SummaryResponse;
import com.logAnalyzer.ai.provider.LLMFactory;
import com.logAnalyzer.ai.provider.UniversalLLMProvider;
import com.logAnalyzer.ai.repository.AiResultRepository;
import com.logAnalyzer.ai.service.AIService;
import com.logAnalyzer.ai.service.AiPromptConfigService;
import com.logAnalyzer.core.entity.LogEntryDocument;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.exception.AiResponseParseException;
import com.logAnalyzer.core.exception.SessionNotFoundException;
import com.logAnalyzer.core.repository.LogEntryCustomEsRepository;
import com.logAnalyzer.core.repository.LogEntryEsRepository;
import com.logAnalyzer.core.repository.LogSessionRepository;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.logAnalyzer.ai.enums.QueryIntent.NOT_RECOGNIZED;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final AiResultRepository aiResultRepository;
    private final LogSessionRepository sessionRepository;
    private final LogEntryEsRepository logEntryEsRepository;
    private final ObjectMapper objectMapper;
    private final LogEntryCustomEsRepository logEntryCustomEsRepository;
    private final UniversalLLMProvider universalLLMProvider;
    private final AiPromptConfigService promptConfigService;

    @Override
    public SummaryResponse summarizeLogs(String sessionId, String userId, LLMRequest request) {
        validateSessionOwnership(sessionId, userId);
        request = validateLlmRequest(request);

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

        request.setSystemPrompt(promptConfigService.getPrompt(AiPromptKey.SUMMARY));
        request.setUserPrompt(buildUserPrompt(errors));
        request.setMaxTokens(300);
        request.setTemperature(0.3);

        String summary = universalLLMProvider.complete(request);

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
    public DiagnosisResponse analyse(String sessionId, String userId, LLMRequest request) throws AiResponseParseException {
        validateSessionOwnership(sessionId, userId);
        request = validateLlmRequest(request);

        AiResult cache = getCached(sessionId, request, AiFeatureEnum.DIAGNOSIS);
        if (cache != null) {
            if(!doesHaveDiagnosisResult(cache.getResult())) {
                return DiagnosisResponse.builder()
                        .sessionId(sessionId)
                        .impactSummary(cache.getResult())
                        .rootCauses(new ArrayList<>())
                        .preventiveActions(new ArrayList<>())
                        .cached(true)
                        .provider(cache.getProvider().name())
                        .model(cache.getModel())
                        .build();
            }

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

        request.setSystemPrompt(promptConfigService.getPrompt(AiPromptKey.DIAGNOSIS));
        request.setUserPrompt(buildUserPrompt(errors));
        request.setMaxTokens(1500);      // more than summary — structured JSON needs space
        request.setTemperature(0.5);

        String rawJson = universalLLMProvider.complete(request);
        DiagnosisResponse response = deserialize(rawJson, DiagnosisResponse.class);

        response.setSessionId(sessionId);
        response.setModel(request.getModel());
        response.setProvider(request.getProvider().name());
        response.setCached(false);

        cacheAiResponse(sessionId, request, rawJson, AiFeatureEnum.DIAGNOSIS);
        return response;
    }

    @Override
    public NlQueryResponse queryProcessor(String sessionId, String userId, LLMRequest request) throws AiResponseParseException {
        validateSessionOwnership(sessionId, userId);
        request = validateLlmRequest(request);

        String systemPrompt = promptConfigService.getPrompt(AiPromptKey.NLQ)
                .formatted(LocalDateTime.now());
        request.setSystemPrompt(systemPrompt);
        request.setMaxTokens(300);
        request.setTemperature(0.2);   // fully deterministic — same query = same filter

        String rawJson = universalLLMProvider.complete(request);
        InterpretedFilter filter = deserialize(rawJson, InterpretedFilter.class);
        if(filter.getIntent() == null) {
            filter.setIntent(String.valueOf(NOT_RECOGNIZED));
        }

        return switch (filter.getIntent()) {
            case "SEARCH"             -> handleSearch(sessionId, request, filter);
            case "AGGREGATION"        -> handleAggregation(sessionId, request, filter);
            case "FACT_EXTRACTION"    -> handleFactExtraction(sessionId, request, filter);
            default                             -> handleUnsupported(request);
        };
    }

    private NlQueryResponse handleAggregation(String sessionId, LLMRequest request, InterpretedFilter filter) {
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

    private NlQueryResponse handleSearch(String sessionId, LLMRequest request, InterpretedFilter filter) {
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

    private NlQueryResponse handleFactExtraction(String sessionId, LLMRequest request, InterpretedFilter filter) {

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
        request.setSystemPrompt(promptConfigService.getPrompt(AiPromptKey.NLQ_FACT_EXTRACTION));
        request.setMaxTokens(150);
        request.setTemperature(0.0);
        request.setUserPrompt(request.getUserPrompt() + "\n\nLog Context:\n" + logContext);
        String answer = universalLLMProvider.complete(request);

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

    private NlQueryResponse handleUnsupported(LLMRequest request) {
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
            if(json != null) {
                String clean = json.replaceAll("```json|```", "").trim();
                // This returns an instance of T, matching the new method return type
                return objectMapper.readValue(clean, targetClass);
            }else{
                return (T) InterpretedFilter.builder()
                        .intent(NOT_RECOGNIZED.name())
                        .explanation("Not Result found").build();
            }
        } catch (JsonProcessingException e) {
            throw new AiResponseParseException("Failed to parse root cause response", e);
        }
    }

    private void cacheAiResponse(String sessionId, LLMRequest request, String summary, AiFeatureEnum feature) {
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

    private AiResult getCached(String sessionId, LLMRequest request, AiFeatureEnum feature) {
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
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException(
                        "SessionId not found: " + sessionId));
    }

    private LLMRequest validateLlmRequest(LLMRequest request) {
        if(request != null && request.getProvider() != null && request.getModel() != null) {
            return request;
        }
        return LLMRequest.builder()
                .provider(LLMFactory.getDefaultProvider())
                .model(LLMFactory.getDefaultModel())
                .build();
    }

    private boolean doesHaveDiagnosisResult(String cacheResult){
        return cacheResult.contains("rootCauses") && cacheResult.contains("preventiveActions");
    }
}
