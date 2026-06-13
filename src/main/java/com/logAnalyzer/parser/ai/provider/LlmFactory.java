package com.logAnalyzer.parser.ai.provider;

import com.logAnalyzer.parser.ai.enums.LlmProviderEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmFactory {
    private final List<LLMProvider> providers;
    public LLMProvider getProvider(LlmProviderEnum providerName) {
        return providers.stream()
                .filter(provider -> provider.getProviderName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable LLM provider found with name: " + providerName));
    }
}
