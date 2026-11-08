package com.logAnalyzer.core.strategy.langparser.java;

import com.logAnalyzer.core.strategy.LogParser;
import com.logAnalyzer.core.model.ParsedLog;
import com.logAnalyzer.core.service.LogParserConfigService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Component
public class JavaLogParser implements LogParser {

    private final List<JavaSubParser> javaSubParser;
    private final LogParserConfigService configService;

    @Autowired
    public JavaLogParser(List<JavaSubParser> javaSubParser, LogParserConfigService configService) {
        this.javaSubParser = javaSubParser;
        this.configService = configService;
    }

    @Override
    public boolean isParsable(List<String> logLines) {
        for(String line : logLines) {
            if (canParse(line)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean canParse(String logLine) {
        if (logLine == null) return false;

        return configService.getLanguagePatterns("java").stream()
                .anyMatch(pattern -> pattern.matcher(logLine).matches());
    }

    @Override
    public ParsedLog parse(String logLine) {
        return javaSubParser.stream()
                .filter(subParser -> subParser.canParse(logLine))
                .findFirst()
                .orElse(fallBackParse(logLine))
                .parse(logLine);
    }

    @Override
    public boolean isPrimaryLine(String line) {
        if (line == null || line.isBlank()) return false;
        // Stack trace continuation — definitely NOT a primary line
        if (line.startsWith("\tat ")) return false;
        if (line.startsWith("Caused by:")) return false;
        if (line.startsWith("\t... ")) return false;
        // If it matches a timestamp at the start — it's a new log event
        return canParse(line);
    }

    private JavaSubParser fallBackParse(String logLine) {
        return new JavaSubParser() {
            @Override
            public boolean canParse(String logLine) {
                return true;
            }

            @Override
            public ParsedLog parse(String logLine) {
                return ParsedLog.builder().rawLog(logLine).build();
            }
        };
    }
}
