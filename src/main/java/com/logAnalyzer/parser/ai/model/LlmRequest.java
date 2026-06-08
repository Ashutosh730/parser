package com.logAnalyzer.parser.ai.model;

import com.logAnalyzer.parser.ai.enums.LlmProviderEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmRequest {
    private String systemPrompt;
    private String userPrompt;
    private int maxTokens;
    private float temperature;
    private LlmProviderEnum provider;
}
