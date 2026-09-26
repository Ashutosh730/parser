package com.logAnalyzer.ai.provider;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import com.logAnalyzer.ai.model.LlmRequest;
import org.springframework.stereotype.Component;

@Component
public interface LLMProvider {
    String complete(LlmRequest request);
    LlmProviderEnum getProviderName();
}
