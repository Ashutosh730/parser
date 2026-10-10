package com.logAnalyzer.ai.model;

import com.logAnalyzer.ai.enums.LlmProviderEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmRequest {
    private String systemPrompt;
    private String userPrompt;
    private int maxTokens;
    private Double temperature;
    private LlmProviderEnum provider;
    private String model;
}
