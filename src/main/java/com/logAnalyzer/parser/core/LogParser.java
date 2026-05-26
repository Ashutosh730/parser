package com.logAnalyzer.parser.core;

import com.logAnalyzer.parser.model.parsed.ParsedLog;

import java.util.List;

public interface LogParser {
    boolean isParsable(List<String> logLines);
    ParsedLog parse(String logEntry);
    boolean isPrimaryLine(String line);
}
