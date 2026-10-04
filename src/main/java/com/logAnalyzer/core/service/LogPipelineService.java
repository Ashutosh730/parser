package com.logAnalyzer.core.service;

import com.logAnalyzer.core.strategy.LogParser;
import com.logAnalyzer.core.strategy.LogParserFactory;
import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.DetectedLanguage;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.enums.LogSessionStatus;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.mapper.ParsedLogMapper;
import com.logAnalyzer.core.entity.LogEntryDocument;
import com.logAnalyzer.core.model.ParsedLog;
import com.logAnalyzer.core.repository.LogEntryEsRepository;
import com.logAnalyzer.core.service.impl.LogSessionServiceImpl;
import com.logAnalyzer.core.util.ParserUtil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class LogPipelineService {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private LogParserFactory logParserFactory;
    @Autowired
    private LogSessionServiceImpl logSessionService;
    @Autowired
    private LogEntryEsRepository logEntryEsRepository;
    @Autowired
    @Qualifier("logProcessingExecutor")
    private TaskExecutor logProcessingExecutor;

    public void enqueueFile(String sessionId, Path logFilePath) {
        try {
            logProcessingExecutor.execute(() -> processFile(sessionId, logFilePath));
        } catch (RejectedExecutionException e) {
            log.error("Log processing queue is full for sessionId = {}", sessionId, e);
            handleProcessingError(sessionId, "Log processing queue is full");
        }
    }

    public void processFile(String sessionId, Path logFilePath) {
        log.debug("Processing file for sessionId = {}, path = {}", sessionId, logFilePath);
        List<LogEntryDocument> batch = new ArrayList<>(BATCH_SIZE);
        ProcessingStats stats = new ProcessingStats();

        LogParser parser = null;
        try (BufferedReader reader = Files.newBufferedReader(logFilePath)) {
            String line;
            List<String> sampleLines = new ArrayList<>();
            StringBuilder prevPrimaryLine = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                // Collect first 20 lines as sample for detection
                if (sampleLines.size() < 20) {
                    sampleLines.add(line);

                    // Once we have enough — detect language and get detector
                    if (sampleLines.size() == 20) {
                        parser = logParserFactory.getParser(sampleLines);
                        if(parser == null){
                            log.error("No suitable parser found for the provided log entries: {}", sampleLines);
                            handleProcessingError(sessionId,"No suitable parser found for the provided log file");
                            return;
                        }
                        for (String sampledLine : sampleLines) {
                            handleSampleLine(parser, prevPrimaryLine, sampledLine, sessionId, batch, stats);
                        }
                    }
                    continue;
                }
            
                if (parser != null) {
                    handleSampleLine(parser, prevPrimaryLine, line, sessionId, batch, stats);
                }
            }

            // Handle files with < 20 lines: get parser from available lines and flush final block
            if (parser == null && !sampleLines.isEmpty()) {
                parser = logParserFactory.getParser(sampleLines);
                if(parser == null){
                    log.error("No suitable parser found for the provided log entries: {}", sampleLines);
                    handleProcessingError(sessionId,"No suitable parser found for the provided log file");
                    return;
                }
                for (String sampledLine : sampleLines) {
                    handleSampleLine(parser, prevPrimaryLine, sampledLine, sessionId, batch, stats);
                }
            }
            
            // Flush the last accumulated primary line block
            if (parser != null && !prevPrimaryLine.isEmpty()) {
                ParsedLog parsedLog = parser.parse(prevPrimaryLine.toString());
                if (parsedLog != null) {
                        addParsedLog(sessionId, parsedLog, batch, stats);
                    log.info("Final parsed log {}", parsedLog);
                }
            }
                    flushBatch(batch, stats);
            DetectedLanguage language = (parser != null) 
                    ? ParserUtil.getDetectedLanguage(parser.getClass().getSimpleName())
                    : DetectedLanguage.UNKNOWN;
                    DetectedFramework framework = Objects.requireNonNullElse(stats.framework, DetectedFramework.UNKNOWN);

                    completeSession(sessionId, framework, language, stats);
                    log.info("Finished processing file for sessionId = {}, total parsed logs = {}", sessionId, stats.totalLines);
        } catch (IllegalArgumentException e) {
            log.error("No suitable parser found for log file: {}", e.getMessage());
            handleProcessingError(sessionId, e.getMessage());
        } catch (RuntimeException e) {
            log.error("Error processing file for sessionId = {}", sessionId, e);
            handlePersistenceError(sessionId, e);
        } catch (IOException e) {
            log.error("Error processing log file: {}", e.getMessage());
            handleProcessingError(sessionId, e.getMessage());
        }
    }

    private void completeSession(String sessionId, DetectedFramework framework, DetectedLanguage language, ProcessingStats stats) {
        LogSession logSession = LogSession.builder()
                .id(sessionId)
                .totalLines(stats.totalLines)
                .errorCount(stats.errorCount)
                .warnCount(stats.warnCount)
                .status(LogSessionStatus.COMPLETED)
                .detectedFramework(framework)
                .detectedLanguage(language)
                .build();
        logSessionService.updateStatus(logSession);
    }

    private void handleSampleLine(LogParser parser, StringBuilder prevPrimaryLine, String sampledLine,
                                  String sessionId, List<LogEntryDocument> batch, ProcessingStats stats) {
        if (parser.isPrimaryLine(sampledLine)) {
            if (!prevPrimaryLine.isEmpty()) {
                ParsedLog parsedLog = parser.parse(prevPrimaryLine.toString());
                if (parsedLog != null) {
                    addParsedLog(sessionId, parsedLog, batch, stats);
                }
            }
            prevPrimaryLine.setLength(0);
            prevPrimaryLine.append(sampledLine);
        } else {
            if (prevPrimaryLine.isEmpty()) {
                return;
            }
            prevPrimaryLine.append('\n');
            prevPrimaryLine.append(sampledLine);
        }
    }

    private void addParsedLog(String sessionId, ParsedLog parsedLog, List<LogEntryDocument> batch, ProcessingStats stats) {
        LogEntryDocument entry = ParsedLogMapper.toEntity(parsedLog, sessionId);
        batch.add(entry);
        if (stats.framework == null && parsedLog.getFramework() != null) {
            stats.framework = parsedLog.getFramework();
        }
        if (batch.size() == BATCH_SIZE) {
            flushBatch(batch, stats);
        }
    }

    private void flushBatch(List<LogEntryDocument> batch, ProcessingStats stats) {
        if (batch.isEmpty()) {
            return;
        }
        logEntryEsRepository.saveAll(batch);
        stats.totalLines += batch.size();
        stats.errorCount += (int) batch.stream().filter(e -> LogLevel.ERROR.equals(e.getLevel())).count();
        stats.warnCount += (int) batch.stream().filter(e -> LogLevel.WARN.equals(e.getLevel())).count();
        log.info("Persisted batch of {} parsed logs", batch.size());
        batch.clear();
    }

    private void handlePersistenceError(String sessionId, RuntimeException exception) {
        LogSession logSession = LogSession.builder()
                .id(sessionId)
                .failureReason("Error persisting parsed logs: " + exception.getMessage())
                .status(LogSessionStatus.FAILED)
                .build();
        logSessionService.updateStatus(logSession);
    }

    private static class ProcessingStats {
        private int totalLines;
        private int errorCount;
        private int warnCount;
        private DetectedFramework framework;
    }

    private void handleProcessingError(String sessionId, String msg) {
        LogSession logSession = LogSession.builder()
                .id(sessionId)
                .failureReason(msg)
                .status(LogSessionStatus.FAILED)
                .build();
        logSessionService.updateStatus(logSession);
    }
}