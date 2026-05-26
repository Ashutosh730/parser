package com.logAnalyzer.parser.core.langparser.java;

import com.logAnalyzer.parser.model.parsed.ParsedLog;

public interface JavaSubParser {
    boolean canParse(String logEntry);
    ParsedLog parse(String logEntry);
}
