package com.logAnalyzer.core.controller;

import com.logAnalyzer.auth.repository.UserRepository;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.mapper.SessionMapper;
import com.logAnalyzer.core.model.SessionResponse;
import com.logAnalyzer.core.repository.LogSessionRepository;
import com.logAnalyzer.core.service.LogPipelineService;
import com.logAnalyzer.core.service.SessionService;
import com.logAnalyzer.core.service.StorageService;
import com.logAnalyzer.core.util.LogFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final StorageService storageService;
    private final LogPipelineService pipelineService;
    private final SessionService sessionService;
    private final LogSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(@RequestParam MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {

        String result = LogFileUtil.validateFile(file);
        if (result != null) {
            return ResponseEntity.badRequest()
                    .body(SessionResponse.builder()
                            .message(result)
                            .fileName(file.getOriginalFilename())
                            .build());
        }

        String userId = getUserId(userDetails);
        Path logFilePath = storageService.upload(file);
        String sessionId = sessionService.create(file.getOriginalFilename(), logFilePath.getFileName().toString(), userId);

        pipelineService.processFile(sessionId, logFilePath);
        return ResponseEntity.accepted()
                .body(SessionResponse.builder()
                        .message("File uploaded successfully")
                        .fileName(file.getOriginalFilename())
                        .sessionId(sessionId)
                        .build());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);

        List<LogSession> sessions = sessionRepository.findByUserId(userId);
        return ResponseEntity.ok(sessions.stream()
                .map(SessionMapper::toResponse)
                .toList());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable String sessionId, @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);

        if(userId == null || userId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<LogSession> session = sessionRepository.findByIdAndUserId(sessionId, userId);
        return ResponseEntity.ok(SessionMapper.toResponse(session.orElseThrow(() -> new RuntimeException("Session not found"))));
    }

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();
    }
}