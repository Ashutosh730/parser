package com.logAnalyzer.parser.core.langparser.java.subparser;

import com.logAnalyzer.parser.core.langparser.java.JavaSubParser;
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.model.parsed.ParsedLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SpringBootParser implements JavaSubParser {

    @Value("${log.format.java.spring-boot}")
    private String logFormat;

    @Override
    public boolean canParse(String logLine) {
        if (logLine == null) return false;
        String headerLine = extractHeaderLine(logLine);
        Pattern pattern = Pattern.compile(logFormat);
        return pattern.matcher(headerLine).find();
    }

    @Override
    public ParsedLog parse(String logLine) {
        if (logLine == null) 
            return ParsedLog.builder().rawLog(null).build();
        
        String headerLine = extractHeaderLine(logLine);
        String continuationLines = extractContinuationLines(logLine);
        Pattern pattern = Pattern.compile(logFormat);
        var matcher = pattern.matcher(headerLine);
        if (matcher.find()) {
            String timestamp = matcher.group("timestamp");
            String level = matcher.group("level");
            String thread = matcher.group("thread").trim();
            String className = matcher.group("className");
            String pid = matcher.group("pid");
            String message = matcher.group("message");
            if (!continuationLines.isEmpty()) {
                message = message + "\n" + continuationLines;
            }
            return ParsedLog.builder()
                    .timestamp(timestamp)
                    .level(LogLevel.valueOf(level))
                    .thread(thread)
                    .className(className)
                    .message(message)
                    .pid(pid)
                    .build();
        }
        return ParsedLog.builder().rawLog(logLine).build();
    }

    private String extractHeaderLine(String logLine) {
        if (logLine == null) return "";
        int newlineIndex = logLine.indexOf('\n');
        return newlineIndex < 0 ? logLine : logLine.substring(0, newlineIndex);
    }

    private String extractContinuationLines(String logLine) {
        if (logLine == null) return "";
        int newlineIndex = logLine.indexOf('\n');
        if (newlineIndex < 0 || newlineIndex >= logLine.length() - 1) return "";
        return logLine.substring(newlineIndex + 1);
    }
}
