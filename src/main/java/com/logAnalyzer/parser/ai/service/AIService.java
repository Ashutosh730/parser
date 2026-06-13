package com.logAnalyzer.parser.ai.service;

import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.SummaryResponse;

public interface AIService {
    SummaryResponse summarizeLogs(String sessionId, LlmRequest request);
}
