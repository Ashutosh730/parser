package com.logAnalyzer.parser.ai.service;

import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.DiagnosisResponse;
import com.logAnalyzer.parser.ai.model.NlQueryResponse;
import com.logAnalyzer.parser.ai.model.SummaryResponse;
import com.logAnalyzer.parser.exception.AiResponseParseException;

public interface AIService {
    SummaryResponse summarizeLogs(String sessionId, LlmRequest request);
    DiagnosisResponse analyse(String sessionId, LlmRequest request) throws AiResponseParseException;
    NlQueryResponse queryProcessor(String sessionId, LlmRequest request) throws AiResponseParseException;
    }
