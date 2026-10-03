package com.logAnalyzer.core.strategy;

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
                .orElse(null);
    }
}
