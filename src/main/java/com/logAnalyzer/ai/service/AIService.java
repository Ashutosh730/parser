package com.logAnalyzer.ai.service;

import com.logAnalyzer.ai.model.LlmRequest;
import com.logAnalyzer.ai.model.DiagnosisResponse;
import com.logAnalyzer.ai.model.NlQueryResponse;
import com.logAnalyzer.ai.model.SummaryResponse;
import com.logAnalyzer.core.exception.AiResponseParseException;

public interface AIService {
    SummaryResponse summarizeLogs(String sessionId, String userId, LlmRequest request);
    DiagnosisResponse analyse(String sessionId, String userId, LlmRequest request) throws AiResponseParseException;
    NlQueryResponse queryProcessor(String sessionId, String userId, LlmRequest request) throws AiResponseParseException;
}
