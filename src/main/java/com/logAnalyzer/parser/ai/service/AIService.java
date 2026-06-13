package com.logAnalyzer.parser.ai.service;

import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.RootCauseResponse;
import com.logAnalyzer.parser.ai.model.SummaryResponse;
import com.logAnalyzer.parser.exception.AiResponseParseException;

public interface AIService {
    SummaryResponse summarizeLogs(String sessionId, LlmRequest request);
    RootCauseResponse analyse(String sessionId, LlmRequest request) throws AiResponseParseException;
}
