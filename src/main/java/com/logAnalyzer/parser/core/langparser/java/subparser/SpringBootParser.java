package com.logAnalyzer.parser.core.langparser.java.subparser;

import com.logAnalyzer.parser.core.langparser.java.JavaSubParser;
import com.logAnalyzer.parser.enums.DetectedFramework;
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.model.ParsedLog;
import com.logAnalyzer.parser.util.LogSubParserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Component
public class SpringBootParser implements JavaSubParser {

    @Value("${log.format.java.spring-boot}")
    private String logFormat;

    @Override
    public boolean canParse(String logLine) {
        if (logLine == null) return false;
        String headerLine = LogSubParserUtil.extractHeaderLine(logLine);
        Pattern pattern = Pattern.compile(logFormat);
        return pattern.matcher(headerLine).find();
    }

    @Override
    public ParsedLog parse(String logLine) {
        if (logLine == null) 
            return ParsedLog.builder().rawLog(null).build();
        
        String headerLine = LogSubParserUtil.extractHeaderLine(logLine);
        String continuationLines = LogSubParserUtil.extractContinuationLines(logLine);
        Pattern pattern = Pattern.compile(logFormat);
        var matcher = pattern.matcher(headerLine);
        if (matcher.find()) {
            String timestamp = matcher.group("timestamp");
            LocalDateTime formattedTimeStamp = LocalDateTime.parse(timestamp, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
            String level = matcher.group("level");
            String thread = matcher.group("thread").trim();
            String className = matcher.group("className");
            String pid = matcher.group("pid");
            String message = matcher.group("message");
            if (!continuationLines.isEmpty()) {
                message = message + "\n" + continuationLines;
            }
            return ParsedLog.builder()
                    .timestamp(formattedTimeStamp)
                    .level(LogLevel.valueOf(level))
                    .thread(thread)
                    .className(className)
                    .message(message)
                    .pid(pid)
                    .framework(DetectedFramework.SPRING_BOOT)
                    .build();
        }
        return ParsedLog.builder().rawLog(logLine).build();
    }
}
