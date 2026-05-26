package com.logAnalyzer.parser.core.langparser.python;

public interface PythonSubParser {
    boolean canParse(String logEntry);
    void parse(String logEntry);
}
