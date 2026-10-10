package com.logAnalyzer.core.service;

import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.LogSessionStatus;
import com.logAnalyzer.core.model.ParsedLog;
import com.logAnalyzer.core.repository.LogEntryEsRepository;
import com.logAnalyzer.core.strategy.LogParser;
import com.logAnalyzer.core.strategy.LogParserFactory;
import com.logAnalyzer.core.service.impl.LogSessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogPipelineServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void processFile_shouldGroupContinuationLinesAndPersistCompletedSession() throws IOException {
        LogParserFactory factory = mock(LogParserFactory.class);
        LogParser parser = mock(LogParser.class);
        LogSessionServiceImpl sessionService = mock(LogSessionServiceImpl.class);
        LogEntryEsRepository repository = mock(LogEntryEsRepository.class);
        when(factory.getParser(anyList())).thenReturn(parser);
        when(parser.isPrimaryLine(anyString())).thenAnswer(invocation ->
                invocation.getArgument(0, String.class).startsWith("2026-"));
        when(parser.parse(anyString())).thenReturn(ParsedLog.builder()
                .framework(DetectedFramework.SPRING_BOOT)
                .build());

        Path logFile = tempDir.resolve("application.log");
        List<String> lines = List.of(
                "2026-09-27 INFO first message",
                "stack trace line",
                "2026-09-27 ERROR second message"
        );
        Files.write(logFile, lines);

        createService(factory, sessionService, repository).processFile("session-1", logFile);

        ArgumentCaptor<String> parsedChunks = ArgumentCaptor.forClass(String.class);
        verify(parser, org.mockito.Mockito.times(2)).parse(parsedChunks.capture());
        assertEquals(lines.get(0) + "\n" + lines.get(1), parsedChunks.getAllValues().get(0));
        assertEquals(lines.get(2), parsedChunks.getAllValues().get(1));

        ArgumentCaptor<LogSession> sessionCaptor = ArgumentCaptor.forClass(LogSession.class);
        verify(sessionService).updateStatus(sessionCaptor.capture());
        assertEquals(LogSessionStatus.COMPLETED, sessionCaptor.getValue().getStatus());
        assertEquals(2, sessionCaptor.getValue().getTotalLines());
        assertEquals(DetectedFramework.SPRING_BOOT, sessionCaptor.getValue().getDetectedFramework());
        verify(repository).saveAll(anyList());
    }

    @Test
    void processFile_shouldMarkEmptyFileCompletedWithoutPersistence() throws IOException {
        LogParserFactory factory = mock(LogParserFactory.class);
        LogSessionServiceImpl sessionService = mock(LogSessionServiceImpl.class);
        LogEntryEsRepository repository = mock(LogEntryEsRepository.class);
        Path emptyFile = Files.createFile(tempDir.resolve("empty.log"));

        createService(factory, sessionService, repository).processFile("session-2", emptyFile);

        ArgumentCaptor<LogSession> sessionCaptor = ArgumentCaptor.forClass(LogSession.class);
        verify(sessionService).updateStatus(sessionCaptor.capture());
        assertEquals(LogSessionStatus.COMPLETED, sessionCaptor.getValue().getStatus());
        verify(factory, never()).getParser(anyList());
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void processFile_shouldMarkSessionFailedWhenPersistenceFails() throws IOException {
        LogParserFactory factory = mock(LogParserFactory.class);
        LogParser parser = mock(LogParser.class);
        LogSessionServiceImpl sessionService = mock(LogSessionServiceImpl.class);
        LogEntryEsRepository repository = mock(LogEntryEsRepository.class);
        when(factory.getParser(anyList())).thenReturn(parser);
        when(parser.isPrimaryLine(anyString())).thenReturn(true);
        when(parser.parse(anyString())).thenReturn(ParsedLog.builder().build());
        doThrow(new RuntimeException("Elasticsearch unavailable")).when(repository).saveAll(anyList());
        Path logFile = tempDir.resolve("failed.log");
        Files.writeString(logFile, "2026-09-27 ERROR failed");

        createService(factory, sessionService, repository).processFile("session-3", logFile);

        ArgumentCaptor<LogSession> sessionCaptor = ArgumentCaptor.forClass(LogSession.class);
        verify(sessionService).updateStatus(sessionCaptor.capture());
        assertEquals(LogSessionStatus.FAILED, sessionCaptor.getValue().getStatus());
        assertEquals("Error persisting parsed logs: Elasticsearch unavailable", sessionCaptor.getValue().getFailureReason());
    }

    @Test
    void processFile_shouldPersistLargeFilesInFixedSizeBatches() throws IOException {
        LogParserFactory factory = mock(LogParserFactory.class);
        LogParser parser = mock(LogParser.class);
        LogSessionServiceImpl sessionService = mock(LogSessionServiceImpl.class);
        LogEntryEsRepository repository = mock(LogEntryEsRepository.class);
        when(factory.getParser(anyList())).thenReturn(parser);
        when(parser.isPrimaryLine(anyString())).thenReturn(true);
        when(parser.parse(anyString())).thenReturn(ParsedLog.builder().build());
        List<Integer> batchSizes = new ArrayList<>();
        doAnswer(invocation -> {
            batchSizes.add(invocation.getArgument(0, List.class).size());
            return null;
        }).when(repository).saveAll(anyList());

        Path logFile = tempDir.resolve("large.log");
        Files.write(logFile, java.util.stream.IntStream.range(0, 1_001)
                .mapToObj(index -> "2026-09-27 INFO line-" + index)
                .toList());

        createService(factory, sessionService, repository).processFile("session-4", logFile);

        verify(repository, org.mockito.Mockito.times(3)).saveAll(anyList());
        assertEquals(List.of(500, 500, 1), batchSizes);
    }

    private LogPipelineService createService(LogParserFactory factory,
                                              LogSessionServiceImpl sessionService,
                                              LogEntryEsRepository repository) {
        LogPipelineService service = new LogPipelineService();
        ReflectionTestUtils.setField(service, "logParserFactory", factory);
        ReflectionTestUtils.setField(service, "logSessionService", sessionService);
        ReflectionTestUtils.setField(service, "logEntryEsRepository", repository);
        return service;
    }
}