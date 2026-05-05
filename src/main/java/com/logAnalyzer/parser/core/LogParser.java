package com.logAnalyzer.parser.core;

import com.logAnalyzer.parser.model.parsed.ParsedLog;

public interface LogParser {
    boolean canParse(String logEntry);
    ParsedLog parse(String logEntry);
}
