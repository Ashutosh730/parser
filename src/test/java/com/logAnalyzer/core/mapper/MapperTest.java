package com.logAnalyzer.core.mapper;

import com.logAnalyzer.core.entity.LogEntryDocument;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.DetectedLanguage;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.enums.LogSessionStatus;
import com.logAnalyzer.core.model.ParsedLog;
import com.logAnalyzer.core.model.SessionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapperTest {

    @Test
    void parsedLogMapper_shouldMapFieldsAndInitializeEntity() {
        Instant timestamp = Instant.parse("2026-09-27T10:15:30Z");
        ParsedLog parsedLog = ParsedLog.builder()
                .timestamp(timestamp)
                .level(LogLevel.ERROR)
                .thread("main")
                .className("Example")
                .message("failure")
                .pid("42")
                .rawLog("raw")
                .build();

        LogEntryDocument result = ParsedLogMapper.toEntity(parsedLog, "session-1");

        assertEquals("session-1", result.getSessionId());
        assertEquals(timestamp, result.getLogTimestamp());
        assertEquals(LogLevel.ERROR, result.getLevel());
        assertEquals("main", result.getThread());
        assertEquals("Example", result.getClassName());
        assertEquals("failure", result.getMessage());
        assertEquals("42", result.getPid());
        assertEquals("raw", result.getRawLog());
        assertNotNull(result.getId());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void parsedLogMapper_shouldReturnNullForNullInput() {
        assertNull(ParsedLogMapper.toEntity(null, "session-1"));
    }

    @Test
    void sessionMapper_shouldMapAllResponseFields() {
        LocalDateTime uploaded = LocalDateTime.of(2026, 9, 27, 10, 0);
        LocalDateTime completed = uploaded.plusMinutes(2);
        LogSession session = LogSession.builder()
                .id("session-1")
                .fileName("app.log")
                .uploadedAt(uploaded)
                .completedAt(completed)
                .status(LogSessionStatus.COMPLETED)
                .totalLines(10)
                .errorCount(2)
                .warnCount(3)
                .detectedLanguage(DetectedLanguage.JAVA)
                .detectedFramework(DetectedFramework.SPRING_BOOT)
                .build();

        SessionResponse result = SessionMapper.toResponse(session);

        assertEquals("session-1", result.getSessionId());
        assertEquals("app.log", result.getFileName());
        assertEquals(uploaded, result.getUploadedAt());
        assertEquals(completed, result.getCompletedAt());
        assertEquals(LogSessionStatus.COMPLETED, result.getStatus());
        assertEquals(10, result.getTotalLines());
        assertEquals(2, result.getErrorCount());
        assertEquals(3, result.getWarnCount());
        assertEquals(DetectedLanguage.JAVA, result.getDetectedLanguage());
        assertEquals(DetectedFramework.SPRING_BOOT, result.getDetectedFramework());
    }
}
