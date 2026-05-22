package com.logAnalyzer.parser.service.impl;

import com.logAnalyzer.parser.enums.LogSessionStatus;
import com.logAnalyzer.parser.model.LogSession;
import com.logAnalyzer.parser.repository.SessionRepository;
import com.logAnalyzer.parser.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogSessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;

    public String create(@Nullable String originalFilename, String storagePath) {
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
}