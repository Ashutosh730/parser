package com.logAnalyzer.core.service.impl;

import com.logAnalyzer.ai.repository.AiResultRepository;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.DetectedLanguage;
import com.logAnalyzer.core.enums.LogSessionStatus;
import com.logAnalyzer.core.repository.LogEntryCustomEsRepository;
import com.logAnalyzer.core.repository.LogSessionRepository;
import com.logAnalyzer.core.service.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogSessionServiceImplTest {

    @Test
    void create_shouldSavePendingSessionAndReturnGeneratedId() {
        LogSessionRepository repository = mock(LogSessionRepository.class);
        LogSessionServiceImpl service = new LogSessionServiceImpl(repository,
            mock(AiResultRepository.class), mock(LogEntryCustomEsRepository.class), mock(StorageService.class));

        String id = service.create("application.log", "/uploads/application.log", "user-1");

        ArgumentCaptor<LogSession> captor = ArgumentCaptor.forClass(LogSession.class);
        verify(repository).save(captor.capture());
        LogSession saved = captor.getValue();
        assertEquals(saved.getId(), id);
        assertNotNull(saved.getId());
        assertEquals("application.log", saved.getFileName());
        assertEquals("/uploads/application.log", saved.getStoragePath());
        assertEquals("user-1", saved.getUserId());
        assertEquals(LogSessionStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getUploadedAt());
    }

    @Test
    void updateStatus_shouldApplyCompletedStatisticsAndDetection() {
        LogSessionRepository repository = mock(LogSessionRepository.class);
        LogSession stored = LogSession.builder().id("session-1").build();
        when(repository.findById("session-1")).thenReturn(Optional.of(stored));

        new LogSessionServiceImpl(repository,
            mock(AiResultRepository.class), mock(LogEntryCustomEsRepository.class), mock(StorageService.class)).updateStatus(LogSession.builder()
                .id("session-1")
                .status(LogSessionStatus.COMPLETED)
                .totalLines(12)
                .errorCount(2)
                .warnCount(3)
                .detectedFramework(DetectedFramework.SPRING_BOOT)
                .detectedLanguage(DetectedLanguage.JAVA)
                .build());

        assertEquals(LogSessionStatus.COMPLETED, stored.getStatus());
        assertEquals(12, stored.getTotalLines());
        assertEquals(2, stored.getErrorCount());
        assertEquals(3, stored.getWarnCount());
        assertEquals(DetectedFramework.SPRING_BOOT, stored.getDetectedFramework());
        assertEquals(DetectedLanguage.JAVA, stored.getDetectedLanguage());
        assertNotNull(stored.getCompletedAt());
        verify(repository).save(stored);
    }

    @Test
    void updateStatus_shouldApplyInProgressAndFailedStates() {
        LogSessionRepository repository = mock(LogSessionRepository.class);
        LogSession stored = LogSession.builder().id("session-1").build();
        when(repository.findById("session-1")).thenReturn(Optional.of(stored));
        LogSessionServiceImpl service = new LogSessionServiceImpl(repository,
            mock(AiResultRepository.class), mock(LogEntryCustomEsRepository.class), mock(StorageService.class));

        service.updateStatus(LogSession.builder().id("session-1").status(LogSessionStatus.IN_PROGRESS).build());
        assertEquals(LogSessionStatus.IN_PROGRESS, stored.getStatus());

        service.updateStatus(LogSession.builder()
                .id("session-1")
                .status(LogSessionStatus.FAILED)
                .failureReason("parser failed")
                .build());
        assertEquals(LogSessionStatus.FAILED, stored.getStatus());
        assertEquals("parser failed", stored.getFailureReason());
        assertNotNull(stored.getCompletedAt());
        verify(repository, org.mockito.Mockito.times(2)).save(stored);
    }
}