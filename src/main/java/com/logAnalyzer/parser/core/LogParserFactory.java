package com.logAnalyzer.parser.core;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LogParserFactory {
    private final List<LogParser> parsers;

    public LogParser getParser(List<String> logEntries) {
        return parsers.stream()
                .filter(parser -> parser.isParsable(logEntries))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable parser found for log entry: " + logEntries));
    }
}
