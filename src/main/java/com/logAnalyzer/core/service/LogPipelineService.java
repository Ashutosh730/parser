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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogPipelineService {

    private final LogParserFactory logParserFactory;
    private final LogSessionServiceImpl logSessionService;
    private final LogEntryEsRepository logEntryEsRepository;

    public void processFile(String sessionId, Path logFilePath) {
        log.debug("Processing file for sessionId = {}, path = {}", sessionId, logFilePath);
        List<ParsedLog> parsedLogs = new ArrayList<>();

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
                            handleSampleLine(parser, prevPrimaryLine, sampledLine, parsedLogs);
                        }
                    }
                    continue;
                }
            
                if (parser != null) {
                    handleSampleLine(parser, prevPrimaryLine, line, parsedLogs);
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
                    handleSampleLine(parser, prevPrimaryLine, sampledLine, parsedLogs);
                }
            }
            
            // Flush the last accumulated primary line block
            if (parser != null && !prevPrimaryLine.isEmpty()) {
                ParsedLog parsedLog = parser.parse(prevPrimaryLine.toString());
                if (parsedLog != null) {
                    parsedLogs.add(parsedLog);
                    log.info("Final parsed log {}", parsedLog);
                }
            }
            DetectedLanguage language = (parser != null) 
                    ? ParserUtil.getDetectedLanguage(parser.getClass().getSimpleName())
                    : DetectedLanguage.UNKNOWN;
            DetectedFramework framework = parsedLogs.stream()
                    .map(ParsedLog::getFramework)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(DetectedFramework.UNKNOWN);

            processParsedLog(sessionId, framework, language, parsedLogs);
            log.info("Finished processing file for sessionId = {}, \n total parsed logs = {}", sessionId, parsedLogs.size());
        } catch (IllegalArgumentException e) {
            log.error("No suitable parser found for log file: {}", e.getMessage());
            handleProcessingError(sessionId, e.getMessage());
        } catch (IOException e) {
            log.error("Error processing log file: {}", e.getMessage());
        }
    }

    @Transactional
    private void processParsedLog(String sessionId, DetectedFramework framework, DetectedLanguage language, List<ParsedLog> parsedLogs) {
        try {
            if (parsedLogs.isEmpty()) {
                log.warn("No parsed logs found for sessionId = {}", sessionId);
                LogSession logSession = LogSession.builder()
                        .id(sessionId)
                        .status(LogSessionStatus.COMPLETED)
                        .build();
                logSessionService.updateStatus(logSession);
                return;
            }

            List<LogEntryDocument> logEntries = parsedLogs.stream()
                    .map(p -> ParsedLogMapper.toEntity(p, sessionId))
                    .collect(Collectors.toList());
            
            // Batch save to Elasticsearch
            log.info("logTimestamp={}", logEntries.getFirst().getLogTimestamp());
            log.info("createdAt={}", logEntries.getFirst().getCreatedAt());
            logEntryEsRepository.saveAll(logEntries);
            log.info("Successfully persisted {} parsed logs to Elasticsearch for sessionId = {}", logEntries.size(), sessionId);

            LogSession logSession = LogSession.builder()
                    .id(sessionId)
                    .totalLines(logEntries.size())
                    .errorCount((int) logEntries.stream().filter(e -> LogLevel.ERROR.equals(e.getLevel())).count())
                    .warnCount((int) logEntries.stream().filter(e -> LogLevel.WARN.equals(e.getLevel())).count())
                    .status(LogSessionStatus.COMPLETED)
                    .detectedFramework(framework)
                    .detectedLanguage(language)
                    .build();
            logSessionService.updateStatus(logSession);
        } catch (Exception e) {
            log.error("Error persisting parsed logs for sessionId = {}: {}", sessionId, e.getMessage(), e);
            LogSession logSession = LogSession.builder()
                    .id(sessionId)
                    .failureReason("Error persisting parsed logs: " + e.getMessage())
                    .status(LogSessionStatus.FAILED)
                    .build();
            logSessionService.updateStatus(logSession);
        }
    }

    private void handleSampleLine(LogParser parser, StringBuilder prevPrimaryLine, String sampledLine, List<ParsedLog> parsedLogs) {
        if (parser.isPrimaryLine(sampledLine)) {
            if (!prevPrimaryLine.isEmpty()) {
                ParsedLog parsedLog = parser.parse(prevPrimaryLine.toString());
                if (parsedLog != null) {
                    parsedLogs.add(parsedLog);
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

    private void handleProcessingError(String sessionId, String msg) {
        LogSession logSession = LogSession.builder()
                .id(sessionId)
                .failureReason(msg)
                .status(LogSessionStatus.FAILED)
                .build();
        logSessionService.updateStatus(logSession);
    }
}