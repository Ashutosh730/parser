package com.logAnalyzer.parser.core.langparser.java.subparser;

import com.logAnalyzer.parser.core.langparser.java.JavaSubParser;
import com.logAnalyzer.parser.enums.LogLevel;
import com.logAnalyzer.parser.model.ParsedLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static com.logAnalyzer.parser.util.LogSubParserUtil.parseTimestamp;

@Component
public class SimpleJavaParser implements JavaSubParser {

    @Value("${log.format.java.simple-java}")
    private String logFormat;

    @Override
    public boolean canParse(String logLine) {
        Pattern pattern = Pattern.compile(logFormat);
        return pattern.matcher(logLine).find();
    }

    @Override
    public ParsedLog parse(String logLine) {
        Pattern pattern = Pattern.compile(logFormat);
        var matcher = pattern.matcher(logLine);
        if(matcher.matches()) {
            Instant formattedTimeStamp = parseTimestamp(matcher.group("timestamp"),"");
            String level = matcher.group("level");
            String thread = matcher.group("thread").trim();
            String className = matcher.group("className");
            String pid = matcher.group("pid");
            String message = matcher.group("message");
            return ParsedLog.builder()
                    .timestamp(formattedTimeStamp)
                    .level(LogLevel.valueOf(level))
                    .thread(thread)
                    .className(className)
                    .message(message)
                    .pid(pid)
                    .build();
        }
        return ParsedLog.builder()
                .rawLog(logLine)
                .build();
    }
}