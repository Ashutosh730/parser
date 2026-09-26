package com.logAnalyzer.core.mapper;

import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.model.SessionResponse;

public class SessionMapper {

    public static SessionResponse toResponse(LogSession session) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .fileName(session.getFileName())
                .build();
    }
}