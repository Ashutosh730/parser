package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.model.SessionResponse;
import com.logAnalyzer.parser.service.LogPipelineService;
import com.logAnalyzer.parser.service.impl.LocalStorageServiceImpl;
import com.logAnalyzer.parser.service.impl.LogSessionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LogUploadController {

    private final LocalStorageServiceImpl storageService;
    private final LogPipelineService pipelineService;
    private final LogSessionServiceImpl sessionService;

    @PostMapping(value = "/logs/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(@RequestParam MultipartFile file) {
        String logFilePath = storageService.upload(file);
        String sessionId = sessionService.create(file.getOriginalFilename(), logFilePath);
        pipelineService.processFile(sessionId, logFilePath);
        return ResponseEntity.accepted().body(new SessionResponse("File uploaded successfully", file.getOriginalFilename(), sessionId));
    }
}