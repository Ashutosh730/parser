package com.logAnalyzer.core.strategy.langparser.java;

import com.logAnalyzer.core.model.ParsedLog;

public interface JavaSubParser {
    boolean canParse(String logEntry);
    ParsedLog parse(String logEntry);
}
