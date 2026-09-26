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
public class Log4jParser implements JavaSubParser {

    @Value("${log.format.java.log4j}")
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
            String dateTimePattern =  timestamp.contains(",") ? "yyyy-MM-dd HH:mm:ss,SSS" : "yyyy-MM-dd HH:mm:ss.SSS";
            Instant formattedTimeStamp = parseTimestamp(timestamp,dateTimePattern);

            String level = matcher.group("level");
            String thread = matcher.group("thread").trim();
            String className = matcher.group("className");
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
                    .framework(DetectedFramework.LOG4J)
                    .build();
        }
        return ParsedLog.builder().rawLog(logLine).build();
    }
}
