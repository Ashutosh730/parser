package com.logAnalyzer.ai.provider;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LLMFactory {
    private final List<LLMProvider> providers;
    public LLMProvider getProvider(LlmProviderEnum providerName) {
        return providers.stream()
                .filter(provider -> provider.getProviderName().equals(providerName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable LLM provider found with name: " + providerName));
    }

    public static LlmProviderEnum getDefaultProvider(){
        return LlmProviderEnum.NVIDIA;
    }

    public static String getDefaultModel(){
        return "nvidia/nemotron-3-super-120b-a12b";
    }
}
