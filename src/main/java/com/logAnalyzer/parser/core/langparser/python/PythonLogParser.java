package com.logAnalyzer.parser.core.langparser.python;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PythonLogParser implements LogParser {

    @Override
    public boolean isParsable(List<String> logLines) {
        return false;
    }

    @Override
    public ParsedLog parse(String logEntry) {
        return null;
    }

    @Override
    public boolean isPrimaryLine(String line) {
        return false;
    }
}
