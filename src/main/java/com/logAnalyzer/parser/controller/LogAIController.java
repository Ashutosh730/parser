package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.ai.model.*;
import com.logAnalyzer.parser.ai.service.AIService;
import com.logAnalyzer.parser.exception.AiResponseParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs/ai")
@RequiredArgsConstructor
public class LogAIController {

    private final AIService aiService;

    @PostMapping("/summarise/{sessionId}")
    public ResponseEntity<SummaryResponse> summarizeLogs(@PathVariable String sessionId, @RequestBody LlmRequest request) {
        return ResponseEntity.ok(aiService.summarizeLogs(sessionId, request));
    }

    @PostMapping("/diagnose/{sessionId}")
    public ResponseEntity<DiagnosisResponse> rootCause(@PathVariable String sessionId, @RequestBody LlmRequest request) throws AiResponseParseException {
        return ResponseEntity.ok(aiService.analyse(sessionId, request));
    }

    @PostMapping("/query/{sessionId}")
    public ResponseEntity<NlQueryResponse> search(@PathVariable String sessionId, @RequestBody LlmRequest request) throws AiResponseParseException {
        return ResponseEntity.ok(aiService.queryProcessor(sessionId, request));
    }
}
