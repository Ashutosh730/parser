package com.logAnalyzer.parser.core;

import com.logAnalyzer.parser.model.parsed.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LogParserFactory {
    private final List<LogParser> parsers;

    public LogParser getParser(String logEntry) {
        return parsers.stream()
                .filter(parser -> parser.canParse(logEntry))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No suitable parser found for log entry: " + logEntry));
    }

    public ParsedLog parse(String logLine) {
        return getParser(logLine).parse(logLine);
    }
}
