package com.logAnalyzer.ai.provider.impl;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import com.logAnalyzer.ai.model.LlmRequest;
import com.logAnalyzer.ai.provider.LLMProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NvidiaProvider implements LLMProvider {
    private final ChatClient chatClient;
    @Override
    public String complete(LlmRequest request) {
        String prompt = request.getSystemPrompt() + "\n" + request.getUserPrompt();
        return chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder()
                        .model("openai/gpt-oss-20b")
                        .temperature(1.0)
                        .maxTokens(4096)
                        .build())
                .call()
                .content();
    }

    @Override
    public LlmProviderEnum getProviderName() {
        return LlmProviderEnum.NVIDIA;
    }
}
