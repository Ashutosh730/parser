package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.RootCauseResponse;
import com.logAnalyzer.parser.ai.model.SummaryResponse;
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

    @PostMapping("/root-cause/{sessionId}")
    public ResponseEntity<RootCauseResponse> rootCause(@PathVariable String sessionId, @RequestBody LlmRequest request) throws AiResponseParseException {
        return ResponseEntity.ok(aiService.analyse(sessionId, request));
    }
}
