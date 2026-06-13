package com.logAnalyzer.parser.ai.service.impl;

import com.logAnalyzer.parser.ai.entity.AiResult;
import com.logAnalyzer.parser.ai.enums.AiFeatureEnum;
import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.SummaryResponse;
import com.logAnalyzer.parser.ai.provider.LLMProvider;
import com.logAnalyzer.parser.ai.provider.LlmFactory;
import com.logAnalyzer.parser.ai.repository.AiResultRepository;
import com.logAnalyzer.parser.ai.service.AIService;
import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.repository.LogEntryEsRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final LlmFactory llmFactory;
    private final AiResultRepository aiResultRepository;
    private final LogEntryEsRepository logEntryEsRepository;

    @Override
    public SummaryResponse summarizeLogs(String sessionId, LlmRequest request) {

        SummaryResponse cache = getCachedSummary(sessionId, request);
        if (cache != null) {
            return cache;
        }

        List<LogEntryDocument> errors = logEntryEsRepository
                .findBySessionIdAndLevel(sessionId, LogLevel.ERROR);

        if (errors.isEmpty()) {
            cacheAiSummary(sessionId, request, "No errors found in this log file.");
            return SummaryResponse.builder()
                    .sessionId(sessionId)
                    .summary("No errors found in this log file.")
                    .cached(false)
                    .provider(request.getProvider().name())
                    .model(request.getModel())
                    .build();
        }

        request.setSystemPrompt("""
                You are an expert log analyst.\s
                Analyse the provided log errors and give a concise plain-English summary.
                Include: what went wrong, when it started, how many times it occurred.
                Keep it under 150 words. Be specific, not generic.
               \s""");
        request.setUserPrompt(errors.stream().limit(50).map(LogEntryDocument::getMessage).reduce((a, b) -> a + "\n" + b).orElse(""));
        request.setMaxTokens(300);
        request.setTemperature(0.3f);

        LLMProvider provider = llmFactory.getProvider(request.getProvider());
        String summary = provider.complete(request);

        cacheAiSummary(sessionId, request, summary);
        return SummaryResponse.builder()
                .sessionId(sessionId)
                .summary(summary)
                .cached(false)
                .provider(request.getProvider().name())
                .model(request.getModel())
                .build();
    }

    private void cacheAiSummary(String sessionId, LlmRequest request, String summary) {
        AiResult result = AiResult.builder()
                .sessionId(sessionId)
                .feature(AiFeatureEnum.SUMMARY)
                .provider(request.getProvider())
                .model(request.getModel())
                .result(summary)
                .tokensUsed(request.getMaxTokens())
                .build();
        aiResultRepository.save(result);
    }

    @Transactional(readOnly = true)
    private SummaryResponse getCachedSummary(String sessionId, LlmRequest request) {
        Optional<AiResult> cache = aiResultRepository.findBySessionId(sessionId).stream().filter(aiResult ->
                aiResult.getProvider().equals(request.getProvider()) && aiResult.getModel().equals(request.getModel())).findAny();
        if (cache.isPresent()) {
            AiResult aiResult = cache.get();
            return SummaryResponse.builder()
                    .sessionId(sessionId)
                    .summary(aiResult.getResult())
                    .cached(true)
                    .provider(aiResult.getProvider().name())
                    .model(aiResult.getModel())
                    .build();
        }
        return null;
    }
}
