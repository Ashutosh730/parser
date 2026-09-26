package com.logAnalyzer.core.strategy.langparser.java.subparser;

import com.logAnalyzer.core.strategy.langparser.java.JavaSubParser;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.model.ParsedLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Pattern;

import static com.logAnalyzer.core.util.LogSubParserUtil.parseTimestamp;

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