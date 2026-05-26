package com.logAnalyzer.parser.service;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.core.LogParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogPipelineService {

    private final LogParserFactory logParserFactory;

    public void processFile(String sessionId, String logFilePath) {
        log.debug("Processing file for sessionId={}, path={}", sessionId, logFilePath);

        LogParser parser = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
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
            }
            
            // Flush the last accumulated primary line block
            if (parser != null && !prevPrimaryLine.isEmpty()) {
                parser.parse(prevPrimaryLine.toString());
            }
        } catch (IOException e) {
            log.error("Error processing log file: {}", e.getMessage());
        }
    }

    /**
     * Handle a single sampled line: if it's a primary line, flush the previous
     * accumulated primary block to the parser and start a new one. Otherwise,
     * append to the current primary block, separating lines with a newline.
     */
    private void handleSampledLine(LogParser parser, StringBuilder prevPrimaryLine, String sampledLine) {
        if (parser.isPrimaryLine(sampledLine)) {
            if (!prevPrimaryLine.isEmpty()) {
                parser.parse(prevPrimaryLine.toString());
            }
            prevPrimaryLine.setLength(0);
            prevPrimaryLine.append(sampledLine);
        } else {
            if (!prevPrimaryLine.isEmpty()) {
                prevPrimaryLine.append('\n');
            }
            prevPrimaryLine.append(sampledLine);
        }
    }
}