package com.logAnalyzer.ai.service;

import com.logAnalyzer.ai.model.LLMRequest;
import com.logAnalyzer.ai.model.DiagnosisResponse;
import com.logAnalyzer.ai.model.NlQueryResponse;
import com.logAnalyzer.ai.model.SummaryResponse;
import com.logAnalyzer.core.exception.AiResponseParseException;

public interface AIService {
    SummaryResponse summarizeLogs(String sessionId, String userId, LLMRequest request);
    DiagnosisResponse analyse(String sessionId, String userId, LLMRequest request) throws AiResponseParseException;
    NlQueryResponse queryProcessor(String sessionId, String userId, LLMRequest request) throws AiResponseParseException;
}
