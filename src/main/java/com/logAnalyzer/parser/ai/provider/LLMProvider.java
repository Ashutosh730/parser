package com.logAnalyzer.parser.ai.provider;

import com.logAnalyzer.parser.ai.enums.LlmProviderEnum;
import com.logAnalyzer.parser.ai.model.LlmRequest;
import org.springframework.stereotype.Component;

@Component
public interface LLMProvider {
    String complete(LlmRequest request);
    LlmProviderEnum getProviderName();
}
