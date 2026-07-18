package com.logAnalyzer.core.strategy;

import com.logAnalyzer.core.model.ParsedLog;

import java.util.List;

public interface LogParser {
    boolean isParsable(List<String> logLines);
    ParsedLog parse(String logEntry);
    boolean isPrimaryLine(String line);
}
