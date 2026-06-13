package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.ai.model.LlmRequest;
import com.logAnalyzer.parser.ai.model.SummaryResponse;
import com.logAnalyzer.parser.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs/ai")
@RequiredArgsConstructor
public class LogAIController {

    private final AIService aiService;

    @GetMapping("/summarise/{sessionId}")
    public ResponseEntity<SummaryResponse> summarizeLogs(@PathVariable String sessionId, @RequestBody LlmRequest request) {
        return ResponseEntity.ok(aiService.summarizeLogs(sessionId, request));
    }
}
