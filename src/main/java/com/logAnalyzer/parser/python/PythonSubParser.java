package com.logAnalyzer.parser.python;

public interface PythonSubParser {
    boolean canParse(String logEntry);
    void parse(String logEntry);
}
