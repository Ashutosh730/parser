package com.logAnalyzer.parser.ai.provider.impl;

import com.logAnalyzer.parser.ai.enums.LlmProviderEnum;
import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.provider.LLMProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiProvider implements LLMProvider {

    private final OpenAiChatModel client;

    @Override
    public String complete(LlmRequest request) {
        String prompt = request.getSystemPrompt() + "\n" + request.getUserPrompt();
        return client.call(prompt);
    }

    @Override
    public LlmProviderEnum getProviderName() {
        return LlmProviderEnum.OPENAI;
    }
}
