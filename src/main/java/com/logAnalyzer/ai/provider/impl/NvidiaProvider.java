package com.logAnalyzer.ai.provider.impl;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import com.logAnalyzer.ai.model.LlmRequest;
import com.logAnalyzer.ai.provider.LLMProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class NvidiaProvider implements LLMProvider {

    private final ChatClient chatClient;

    @Value("${OPENAI_MODEL:nvidia/nemotron-3-super-120b-a12b}")
    private String model;

    @Override
    public String complete(LlmRequest request) {
        String effectiveModel = resolveEffectiveModel(request.getModel());

        var spec = chatClient.prompt();

        // Properly forward the system prompt as a system message
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            spec = spec.system(request.getSystemPrompt());
        }

        return spec
                .user(request.getUserPrompt())
                .options(ChatOptions.builder()
                        .model(effectiveModel)
                        .temperature(1.0)
                        .maxTokens(4096)
                        .build())
                .call()
                .content();
    }

    String resolveEffectiveModel(String requestedModel) {
        if (requestedModel == null || requestedModel.isBlank()) {
            return model;
        }
        String sanitized = requestedModel.trim();
        if (sanitized.toLowerCase(Locale.ROOT).contains(":free")) {
            return model;
        }
        return sanitized;
    }

    @Override
    public LlmProviderEnum getProviderName() {
        return LlmProviderEnum.NVIDIA;
    }
}