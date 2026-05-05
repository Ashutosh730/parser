package com.logAnalyzer.parser.java;

import com.logAnalyzer.parser.model.parsed.ParsedLog;

public interface JavaSubParser {
    boolean canParse(String logEntry);
    ParsedLog parse(String logEntry);
}
