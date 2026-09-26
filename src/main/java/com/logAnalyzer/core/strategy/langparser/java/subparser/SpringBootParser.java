package com.logAnalyzer.core.strategy.langparser.java.subparser;

import com.logAnalyzer.core.strategy.langparser.java.JavaSubParser;
import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.model.ParsedLog;
import com.logAnalyzer.core.util.LogSubParserUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Pattern;

import static com.logAnalyzer.core.util.LogSubParserUtil.parseTimestamp;

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
            Instant formattedTimeStamp = parseTimestamp(matcher.group("timestamp"),"");
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
