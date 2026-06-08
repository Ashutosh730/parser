package com.logAnalyzer.parser.ai.service.impl;

import com.logAnalyzer.parser.ai.model.SummaryResponse;
import com.logAnalyzer.parser.ai.service.AIService;

public class AIServiceImpl implements AIService {
    @Override
    public SummaryResponse summarizeLogs(String sessionId, String logs) {
        // Placeholder implementation
        return SummaryResponse.builder()
                .summary("This is a summary of the logs for session: " + sessionId)
                .cached(false)
                .provider("MockProvider")
                .build();
    }
}
