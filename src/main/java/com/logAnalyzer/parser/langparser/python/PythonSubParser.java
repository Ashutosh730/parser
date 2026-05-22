package com.logAnalyzer.parser.langparser.python;

public interface PythonSubParser {
    boolean canParse(String logEntry);
    void parse(String logEntry);
}
