package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.auth.repository.UserRepository;
import com.logAnalyzer.parser.entity.LogSession;
import com.logAnalyzer.parser.mapper.SessionMapper;
import com.logAnalyzer.parser.model.SessionResponse;
import com.logAnalyzer.parser.repository.LogSessionRepository;
import com.logAnalyzer.parser.service.LogPipelineService;
import com.logAnalyzer.parser.service.impl.LocalStorageServiceImpl;
import com.logAnalyzer.parser.service.impl.LogSessionServiceImpl;
import com.logAnalyzer.parser.util.LogFileUtil;
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

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogUploadController {

    private final LocalStorageServiceImpl storageService;
    private final LogPipelineService pipelineService;
    private final LogSessionServiceImpl sessionService;
    private final LogSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(@RequestParam MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {

        String result = LogFileUtil.validateFile(file);
        if (result != null) {
            return ResponseEntity.badRequest()
                    .body(new SessionResponse(result, file.getOriginalFilename(),null));
        }

        String userId = getUserId(userDetails);
        Path logFilePath = storageService.upload(file);
        String sessionId = sessionService.create(file.getOriginalFilename(), logFilePath.getFileName().toString(), userId);

        pipelineService.processFile(sessionId, logFilePath);
        return ResponseEntity.accepted()
                .body(new SessionResponse("File uploaded successfully", file.getOriginalFilename(), sessionId));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);

        List<LogSession> sessions = sessionRepository.findByUserId(userId);
        return ResponseEntity.ok(sessions.stream()
                .map(SessionMapper::toResponse)
                .toList());
    }

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();
    }
}