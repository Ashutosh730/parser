package com.logAnalyzer.parser.service.impl;

import com.logAnalyzer.parser.enums.LogSessionStatus;
import com.logAnalyzer.parser.entity.LogSession;
import com.logAnalyzer.parser.repository.SessionRepository;
import com.logAnalyzer.parser.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogSessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    public String create(String originalFilename, String storagePath) {
        LogSession session = LogSession.builder()
                .id(java.util.UUID.randomUUID().toString())
                .fileName(originalFilename)
                .storagePath(storagePath)
                .status(LogSessionStatus.PENDING)
                .uploadedAt(LocalDateTime.now())
                .build();
        sessionRepository.save(session);
        return session.getId();
    }

    public void updateStatus(LogSession logSession) {
        sessionRepository.findById(logSession.getId()).ifPresent(session -> {
            switch (logSession.getStatus()) {
                case IN_PROGRESS -> {
                    session.setStatus(LogSessionStatus.IN_PROGRESS);
                    log.info("Session {} is now IN_PROGRESS", session.getId());
                }
                case COMPLETED -> {
                    session.setStatus(LogSessionStatus.COMPLETED);
                    session.setTotalLines(logSession.getTotalLines());
                    session.setErrorCount(logSession.getErrorCount());
                    session.setWarnCount(logSession.getWarnCount());
                    session.setDetectedFramework(logSession.getDetectedFramework());
                    session.setDetectedLanguage(logSession.getDetectedLanguage());
                    log.info("Session {} is now COMPLETED with totalLines={}, errorCount={}, warnCount={}, detectedFramework={}, detectedLanguage={}",
                            session.getId(), session.getTotalLines(), session.getErrorCount(), session.getWarnCount(), session.getDetectedFramework(), session.getDetectedLanguage());
                }
                case FAILED -> {
                    session.setStatus(LogSessionStatus.FAILED);
                    session.setFailureReason(logSession.getFailureReason());
                    log.info("Session {} FAILED with reason: {}", session.getId(), logSession.getFailureReason());
                }
            }
            session.setCompletedAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }
}