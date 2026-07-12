package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.ai.model.*;
import com.logAnalyzer.parser.ai.service.AIService;
import com.logAnalyzer.parser.auth.repository.UserRepository;
import com.logAnalyzer.parser.exception.AiResponseParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs/ai")
@RequiredArgsConstructor
public class LogAIController {

    private final AIService aiService;
    private final UserRepository userRepository;

    @PostMapping("/summarise/{sessionId}")
    public ResponseEntity<SummaryResponse> summarizeLogs(@PathVariable String sessionId, @RequestBody LlmRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(aiService.summarizeLogs(sessionId, userId, request));
    }

    @PostMapping("/diagnose/{sessionId}")
    public ResponseEntity<DiagnosisResponse> rootCause(@PathVariable String sessionId, @RequestBody LlmRequest request, @AuthenticationPrincipal UserDetails userDetails) throws AiResponseParseException {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(aiService.analyse(sessionId, userId, request));
    }

    @PostMapping("/query/{sessionId}")
    public ResponseEntity<NlQueryResponse> search(@PathVariable String sessionId, @RequestBody LlmRequest request, @AuthenticationPrincipal UserDetails userDetails) throws AiResponseParseException {
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(aiService.queryProcessor(sessionId, userId, request));
    }

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();
    }
}