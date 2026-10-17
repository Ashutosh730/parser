package com.logAnalyzer.core.service.impl;

import com.logAnalyzer.ai.repository.AiResultRepository;
import com.logAnalyzer.core.enums.LogSessionStatus;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.exception.SessionNotFoundException;
import com.logAnalyzer.core.model.PagedResponse;
import com.logAnalyzer.core.model.SessionResponse;
import com.logAnalyzer.core.repository.LogEntryCustomEsRepository;
import com.logAnalyzer.core.repository.LogSessionRepository;
import com.logAnalyzer.core.service.SessionService;
import com.logAnalyzer.core.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.logAnalyzer.core.enums.LogSessionStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogSessionServiceImpl implements SessionService {

    private final LogSessionRepository logSessionRepository;
    private final AiResultRepository aiResultRepository;
    private final LogEntryCustomEsRepository logEntryCustomEsRepository;
    private final StorageService storageService;

    private static final int DEFAULT_PAGE_SIZE = 10;

    public String create(String originalFilename, String storagePath, String userId, String sessionId) {
        LogSession session = LogSession.builder()
                .id(sessionId)
                .userId(userId)
                .fileName(originalFilename)
                .storagePath(storagePath)
                .status(LogSessionStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();
        logSessionRepository.save(session);
        return session.getId();
    }

    public void updateStatus(LogSession logSession) {
        logSessionRepository.findById(logSession.getId()).ifPresent(session -> {
            switch (logSession.getStatus()) {
                case IN_PROGRESS -> {
                    session.setStatus(IN_PROGRESS);
                    log.info("Session {} is now IN_PROGRESS", session.getId());
                }
                case COMPLETED -> {
                    session.setStatus(COMPLETED);
                    session.setTotalLines(logSession.getTotalLines());
                    session.setErrorCount(logSession.getErrorCount());
                    session.setWarnCount(logSession.getWarnCount());
                    session.setDetectedFramework(logSession.getDetectedFramework());
                    session.setDetectedLanguage(logSession.getDetectedLanguage());
                    session.setCompletedAt(LocalDateTime.now());
                    log.info("Session {} is now COMPLETED with totalLines={}, errorCount={}, warnCount={}, detectedFramework={}, detectedLanguage={}",
                            session.getId(), session.getTotalLines(), session.getErrorCount(), session.getWarnCount(), session.getDetectedFramework(), session.getDetectedLanguage());
                }
                case FAILED -> {
                    session.setStatus(FAILED);
                    session.setFailureReason(logSession.getFailureReason());
                    session.setCompletedAt(LocalDateTime.now());
                    log.info("Session {} FAILED with reason: {}", session.getId(), logSession.getFailureReason());
                }
            }
            logSessionRepository.save(session);
        });
    }

    @Transactional
    public void delete(String sessionId, String userId) {
        LogSession session = logSessionRepository
                .findByIdAndUserIdAndDeletedAtIsNull(sessionId, userId)
                .orElseThrow(() -> new SessionNotFoundException("Not found: " + sessionId));

        session.setDeletedAt(LocalDateTime.now());
        session.setDeletedBy(userId);
        logSessionRepository.save(session);

        storageService.delete(session.getStoragePath());
        aiResultRepository.deleteBySessionId(sessionId);
        logEntryCustomEsRepository.deleteBySessionId(sessionId);
        // optionally: storageService.delete(session.getStoragePath());
    }

    @Override
    public PagedResponse<SessionResponse> getSessions(String userId, Pageable pageable) {
        Page<SessionResponse> sessionPage = logSessionRepository
                .findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(com.logAnalyzer.core.mapper.SessionMapper::toResponse);
        return PagedResponse.of(sessionPage);
    }
}