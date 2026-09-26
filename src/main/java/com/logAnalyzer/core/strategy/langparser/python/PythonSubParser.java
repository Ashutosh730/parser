package com.logAnalyzer.core.strategy.langparser.python;

public interface PythonSubParser {
    boolean canParse(String logEntry);
    void parse(String logEntry);
}
