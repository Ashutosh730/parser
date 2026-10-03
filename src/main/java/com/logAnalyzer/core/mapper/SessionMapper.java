package com.logAnalyzer.core.mapper;

import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.model.SessionResponse;

public class SessionMapper {

    public static SessionResponse toResponse(LogSession session) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .fileName(session.getFileName())
                .uploadedAt(session.getUploadedAt())
                .status(session.getStatus())
                .completedAt(session.getCompletedAt())
                .totalLines(session.getTotalLines())
                .errorCount(session.getErrorCount())
                .warnCount(session.getWarnCount())
                .detectedLanguage(session.getDetectedLanguage())
                .detectedFramework(session.getDetectedFramework())
                .failureReason(session.getFailureReason())
                .build();
    }
}