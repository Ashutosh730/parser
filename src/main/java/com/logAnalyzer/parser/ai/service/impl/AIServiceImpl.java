package com.logAnalyzer.parser.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logAnalyzer.parser.ai.entity.AiResult;
import com.logAnalyzer.parser.ai.enums.AiFeatureEnum;
import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.DiagnosisResponse;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final LlmFactory llmFactory;
    private final AiResultRepository aiResultRepository;
    private final LogEntryEsRepository logEntryEsRepository;
    private final ObjectMapper objectMapper;

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
            DiagnosisResponse response = deserialize(cache.getResult());
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

        request.setSystemPrompt(DIAGNOSIS_SYSTEM_PROMPT );
        request.setUserPrompt(buildUserPrompt(errors));
        request.setMaxTokens(500);      // more than summary — structured JSON needs space
        request.setTemperature(0.1f);   // very low — factual, deterministic output

        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String rawJson = provider.complete(request);
        DiagnosisResponse response = deserialize(rawJson);

        response.setSessionId(sessionId);
        response.setModel(request.getModel());
        response.setProvider(request.getProvider().name());
        response.setCached(false);

        cacheAiResponse(sessionId, request, rawJson, AiFeatureEnum.DIAGNOSIS);
        return response;
    }

    private DiagnosisResponse deserialize(String json) throws AiResponseParseException {
        try {
            // Strip markdown code blocks if LLM wraps response in ```json
            String clean = json.replaceAll("```json|```", "").trim();
            return objectMapper.readValue(clean, DiagnosisResponse.class);
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
