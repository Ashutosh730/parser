package com.logAnalyzer.core.strategy.langparser.python;

import com.logAnalyzer.core.strategy.LogParser;
import com.logAnalyzer.core.model.ParsedLog;
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
