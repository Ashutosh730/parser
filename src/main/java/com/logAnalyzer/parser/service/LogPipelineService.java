package com.logAnalyzer.parser.service;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.core.LogParserFactory;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogPipelineService {

    private final LogParserFactory logParserFactory;
    private final List<ParsedLog> parsedLogs = new ArrayList<>();

    public void processFile(String sessionId, Path logFilePath) {
        log.debug("Processing file for sessionId = {}, path = {}", sessionId, logFilePath);

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

                        // Process the sample lines we already collected
                        for (String sampledLine : sampleLines) {
                            handleSampledLine(parser, prevPrimaryLine, sampledLine);
                        }
                    }
                    continue; // Don't process sample lines again
                }
            
                if (parser != null) {
                    handleSampledLine(parser, prevPrimaryLine, line);
                }
            }

            // Handle files with < 20 lines: get parser from available lines and flush final block
            if (parser == null && !sampleLines.isEmpty()) {
                parser = logParserFactory.getParser(sampleLines);
                for (String sampledLine : sampleLines) {
                    handleSampledLine(parser, prevPrimaryLine, sampledLine);
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
            log.info("Finished processing file for sessionId = {}, \n total parsed logs = {}", sessionId, parsedLogs.size());
        } catch (IOException e) {
            log.error("Error processing log file: {}", e.getMessage());
        }
    }

    private void handleSampledLine(LogParser parser, StringBuilder prevPrimaryLine, String sampledLine) {
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
            // Ignore continuation/orphan lines until the first primary log event starts.
            if (prevPrimaryLine.isEmpty()) {
                return;
            }
            prevPrimaryLine.append('\n');
            prevPrimaryLine.append(sampledLine);
        }
    }
}