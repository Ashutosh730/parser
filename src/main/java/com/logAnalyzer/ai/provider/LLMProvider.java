package com.logAnalyzer.ai.provider;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import com.logAnalyzer.ai.model.LLMRequest;
import org.springframework.stereotype.Component;

@Component
public interface LLMProvider {
    String complete(LLMRequest request);
    LlmProviderEnum getProviderName();
}
