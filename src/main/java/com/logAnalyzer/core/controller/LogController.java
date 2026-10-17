package com.logAnalyzer.core.controller;

import com.logAnalyzer.core.model.StorageProperties;
import com.logAnalyzer.auth.repository.UserRepository;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.exception.StorageException;
import com.logAnalyzer.core.mapper.SessionMapper;
import com.logAnalyzer.core.model.PagedResponse;
import com.logAnalyzer.core.model.SessionResponse;
import com.logAnalyzer.core.record.StoreRequest;
import com.logAnalyzer.core.record.StoredObject;
import com.logAnalyzer.core.repository.LogSessionRepository;
import com.logAnalyzer.core.service.LogPipelineService;
import com.logAnalyzer.core.service.SessionService;
import com.logAnalyzer.core.service.StorageService;
import com.logAnalyzer.core.util.LogFileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LogController {

    private final StorageService storageService;
    private final LogPipelineService pipelineService;
    private final SessionService sessionService;
    private final LogSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final StorageProperties storageProperties;

    @PostMapping(value = "/log", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SessionResponse> upload(@RequestParam MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {

        String validationError = LogFileUtil.validateFile(file);
        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(SessionResponse.builder()
                            .message(validationError)
                            .fileName(file.getOriginalFilename())
                            .build());
        }

        if (file.getSize() > storageProperties.getMaxObjectSizeBytes()) {
            return ResponseEntity.badRequest()
                    .body(SessionResponse.builder()
                            .message("File size exceeds the maximum allowed size of " + storageProperties.getMaxObjectSizeBytes() + " bytes.")
                            .fileName(file.getOriginalFilename())
                            .build());
        }

        String userId = getUserId(userDetails);
        String sessionId = UUID.randomUUID().toString();

        StoredObject stored;
        try (InputStream in = file.getInputStream()) {
            stored = storageService.store(new StoreRequest(in, file.getSize(), file.getContentType(), userId, sessionId, file.getOriginalFilename(), Map.of("uploaded-by", userId)));
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }

        sessionService.create(file.getOriginalFilename(), stored.key(), userId, sessionId);
        pipelineService.enqueueFile(sessionId, stored.key());

        return ResponseEntity.accepted()
                .body(SessionResponse.builder()
                        .message("File uploaded successfully")
                        .fileName(file.getOriginalFilename())
                        .sessionId(sessionId)
                        .build());
    }

    @GetMapping("/log")
    public ResponseEntity<StreamingResponseBody> download(@RequestParam String key, @RequestParam long exp, @RequestParam String sig) {

        if (exp < Instant.now().getEpochSecond()) {
            return ResponseEntity.status(410).build();
        }

        InputStream in = storageService.openStream(key);
        StreamingResponseBody body = out -> in.transferTo(out);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }

    @GetMapping("/sessions")
    public ResponseEntity<PagedResponse<SessionResponse>> getSessions(@AuthenticationPrincipal UserDetails userDetails, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        // Guard against abuse: cap page size, reject negative values
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "uploadedAt"));
        String userId = getUserId(userDetails);
        return ResponseEntity.ok(sessionService.getSessions(userId, pageable));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionResponse> getSessionById(@PathVariable String sessionId, @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<LogSession> session = sessionRepository.findByIdAndUserId(sessionId, userId);
        return session.map(logSession -> ResponseEntity.ok(SessionMapper.toResponse(logSession))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId, @AuthenticationPrincipal UserDetails userDetails) {
        String userId = getUserId(userDetails);
        sessionService.delete(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    private String getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"))
                .getId();
    }
}