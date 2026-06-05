package com.logAnalyzer.parser.service;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.core.LogParserFactory;
import com.logAnalyzer.parser.enums.DetectedFramework;
import com.logAnalyzer.parser.enums.DetectedLanguage;
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.enums.LogSessionStatus;
import com.logAnalyzer.parser.entity.LogSession;
import com.logAnalyzer.parser.mapper.ParsedLogMapper;
import com.logAnalyzer.parser.entity.LogEntryDocument;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import com.logAnalyzer.parser.repository.LogEntryEsRepository;
import com.logAnalyzer.parser.service.impl.LogSessionServiceImpl;
import com.logAnalyzer.parser.util.ParserUtil;
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
    private final List<ParsedLog> parsedLogs = new ArrayList<>();
    private final LogEntryEsRepository logEntryEsRepository;

    public void processFile(String sessionId, Path logFilePath) {
        log.debug("Processing file for sessionId = {}, path = {}", sessionId, logFilePath);
        parsedLogs.clear();

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
                        for (String sampledLine : sampleLines) {
                            handleSampleLine(parser, prevPrimaryLine, sampledLine);
                        }
                    }
                    continue;
                }
            
                if (parser != null) {
                    handleSampleLine(parser, prevPrimaryLine, line);
                }
            }

            // Handle files with < 20 lines: get parser from available lines and flush final block
            if (parser == null && !sampleLines.isEmpty()) {
                parser = logParserFactory.getParser(sampleLines);
                for (String sampledLine : sampleLines) {
                    handleSampleLine(parser, prevPrimaryLine, sampledLine);
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

            processParsedLog(sessionId, framework, language);
            log.info("Finished processing file for sessionId = {}, \n total parsed logs = {}", sessionId, parsedLogs.size());
        } catch (IOException e) {
            log.error("Error processing log file: {}", e.getMessage());
        }
    }

    @Transactional
    private void processParsedLog(String sessionId, DetectedFramework framework, DetectedLanguage language) {
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

    private void handleSampleLine(LogParser parser, StringBuilder prevPrimaryLine, String sampledLine) {
        if (parser.isPrimaryLine(sampledLine)) {
            if (!prevPrimaryLine.isEmpty()) {
                ParsedLog parsedLog = parser.parse(prevPrimaryLine.toString());
                if (parsedLog != null) {
                    parsedLogs.add(parsedLog);
                    log.info("Parsed log: {}", parsedLog);
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
}