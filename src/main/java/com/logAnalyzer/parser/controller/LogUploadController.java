package com.logAnalyzer.parser.controller;

import com.logAnalyzer.parser.model.SessionResponse;
import com.logAnalyzer.parser.service.LogPipelineService;
import com.logAnalyzer.parser.service.impl.LocalStorageServiceImpl;
import com.logAnalyzer.parser.service.impl.LogSessionServiceImpl;
import com.logAnalyzer.parser.util.LogFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogUploadController {

    private final LocalStorageServiceImpl storageService;
    private final LogPipelineService pipelineService;
    private final LogSessionServiceImpl sessionService;

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(@RequestParam MultipartFile file) {
        String result = LogFileUtil.validateFile(file);
        if(result != null) {
            return ResponseEntity.badRequest().body(new SessionResponse(file.getName(), result, null));
        }
        Path logFilePath = storageService.upload(file);
        String sessionId = sessionService.create(file.getOriginalFilename(), logFilePath.getFileName().toString());
        pipelineService.processFile(sessionId, logFilePath);
        return ResponseEntity.accepted().body(new SessionResponse("File uploaded successfully", file.getOriginalFilename(), sessionId));
    }
}