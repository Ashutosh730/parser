package com.logAnalyzer.parser.langparser.python;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PythonLogParser implements LogParser {

    @Override
    public boolean canParse(String logEntry) {
        return false;
    }

    @Override
    public ParsedLog parse(String logEntry) {
        return null;
    }
}
