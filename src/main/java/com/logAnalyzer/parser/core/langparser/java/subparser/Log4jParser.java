package com.logAnalyzer.parser.core.langparser.java.subparser;

import com.logAnalyzer.parser.core.langparser.java.JavaSubParser;
import com.logAnalyzer.parser.model.parsed.ParsedLog;

public class Log4jParser implements JavaSubParser {
    @Override
    public boolean canParse(String logLine) {
        return logLine.matches(".*\\d{4}-\\d{2}-\\d{2}.*\\[.*\\].*-.*");
    }

    @Override
    public ParsedLog parse(String logLine) {
        // Implement Log4j specific parsing logic here
        // For demonstration, we will just return a ParsedLog with the raw log line
        return ParsedLog.builder()
                .rawLog(logLine)
                .build();
    }
}
