package com.logAnalyzer.parser.core.langparser.java;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.model.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JavaLogParser implements LogParser {

    private final List<JavaSubParser> javaSubParser;

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

        // Spring Boot pattern hint
        if (logLine.contains("---") && logLine.contains("[")) {
            return true;
        }

        // Simple Java pattern
        if(logLine.matches("(?s).*\\w{3} \\d{2}, \\d{4} \\d{2}:\\d{2}:\\d{2} [AP]M .*\\n.*")) {
            return true;
        }

        // Log4j style
        return logLine.matches(".*\\d{4}-\\d{2}-\\d{2}.*\\[.*].*-.*");

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
