package com.logAnalyzer.core.strategy.langparser.java;

import com.logAnalyzer.core.strategy.LogParser;
import com.logAnalyzer.core.model.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class JavaLogParser implements LogParser {

    private static final List<String> JAVA_LOG_REGEXES = List.of(
            "(?s).*---.*\\[.*",
            "^\\s*\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\s+[\\w.$]+\\s+[\\w$<>]+\\s*$",
            "(?s).*\\w{3} \\d{2}, \\d{4} \\d{2}:\\d{2}:\\d{2} [AP]M .*\\n.*",
            ".*\\d{4}-\\d{2}-\\d{2}.*\\[.*].*-.*"
    );

    private static final List<Pattern> JAVA_LOG_PATTERNS = JAVA_LOG_REGEXES.stream()
            .map(Pattern::compile)
            .toList();

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

        return JAVA_LOG_PATTERNS.stream()
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
