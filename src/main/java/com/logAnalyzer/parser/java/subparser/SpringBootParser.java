package com.logAnalyzer.parser.java.subparser;

import com.logAnalyzer.parser.java.JavaSubParser;
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
        Pattern pattern = Pattern.compile(logFormat);
        return pattern.matcher(logLine).find();
    }

    @Override
    public ParsedLog parse(String logLine) {
        Pattern pattern = Pattern.compile(logFormat);
        var matcher = pattern.matcher(logLine);
        if(matcher.matches()) {
            String timestamp = matcher.group("timestamp");
            String level = matcher.group("level");
            String thread = matcher.group("thread").trim();
            String className = matcher.group("className");
            String pid = matcher.group("pid");
            String message = matcher.group("message");
            return ParsedLog.builder()
                    .timestamp(timestamp)
                    .level(level)
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
