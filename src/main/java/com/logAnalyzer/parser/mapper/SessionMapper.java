package com.logAnalyzer.parser.mapper;

import com.logAnalyzer.parser.entity.LogSession;
import com.logAnalyzer.parser.model.SessionResponse;
import org.springframework.stereotype.Component;

public class SessionMapper {

    public static SessionResponse toResponse(LogSession session) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .fileName(session.getFileName())
                .build();
    }
}