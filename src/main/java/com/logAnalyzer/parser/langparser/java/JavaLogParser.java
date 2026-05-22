package com.logAnalyzer.parser.langparser.java;

import com.logAnalyzer.parser.core.LogParser;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JavaLogParser implements LogParser {

    private final List<JavaSubParser> javaSubParser;

    @Override
    public boolean canParse(String logLine) {

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
        if (logLine.matches(".*\\d{4}-\\d{2}-\\d{2}.*\\[.*\\].*-.*")) {
            return true;
        }

        return false;
    }

    @Override
    public ParsedLog parse(String logLine) {
        return javaSubParser.stream()
                .filter(subParser -> subParser.canParse(logLine))
                .findFirst()
                .orElse(fallBackParse(logLine))
                .parse(logLine);
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
