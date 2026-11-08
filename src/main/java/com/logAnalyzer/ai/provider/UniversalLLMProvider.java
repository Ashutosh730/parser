package com.logAnalyzer.ai.provider;

import com.logAnalyzer.ai.model.LLMRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class UniversalLLMProvider {

    private final ChatClient chatClient;

    @Value("${OPENAI_MODEL:nvidia/nemotron-3.5-super-vl-120b-a12b}")
    private String model;

    public String complete(LLMRequest request) {

        var spec = chatClient.prompt();

        // Properly forward the system prompt as a system message
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            spec = spec.system(request.getSystemPrompt());
        }

        ChatClient.CallResponseSpec res =  spec
                .user(request.getUserPrompt())
                .options(ChatOptions.builder()
                        .model(model)
                        .temperature(request.getTemperature())
                        // Nemotron may consume tokens for reasoning before producing content.
                        .maxTokens(Math.max(request.getMaxTokens(), 8192))
                        .build())
                .call();
        String content = res.content();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException(
                    "LLM returned no text content; the model may have exhausted its token budget while reasoning");
        }
        return content;
    }
}