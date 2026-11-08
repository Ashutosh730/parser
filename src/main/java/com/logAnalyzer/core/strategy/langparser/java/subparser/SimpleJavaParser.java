package com.logAnalyzer.core.strategy.langparser.java.subparser;

import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.strategy.langparser.java.JavaSubParser;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.model.ParsedLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.logAnalyzer.core.service.LogParserConfigService;

import java.time.Instant;
import java.util.regex.Pattern;

import static com.logAnalyzer.core.util.LogSubParserUtil.parseTimestamp;

@Component
@RequiredArgsConstructor
public class SimpleJavaParser implements JavaSubParser {

    private final LogParserConfigService configService;

    @Override
    public boolean canParse(String logLine) {
        Pattern pattern = getPattern();
        return pattern.matcher(logLine).find();
    }

    @Override
    public ParsedLog parse(String logLine) {
        Pattern pattern = getPattern();
        var matcher = pattern.matcher(logLine);
        if(matcher.matches()) {
            Instant formattedTimeStamp = parseTimestamp(matcher.group("timestamp"),"yyyy-MM-dd HH:mm:ss");
            String level = matcher.group("level");
            String thread = matcher.group("thread").trim();
            String className = matcher.group("className");
            String message = matcher.group("message");
            return ParsedLog.builder()
                    .timestamp(formattedTimeStamp)
                    .level(LogLevel.valueOf(level))
                    .thread(thread)
                    .className(className)
                    .framework(DetectedFramework.SIMPLE_JAVA)
                    .message(message)
                    .build();
        }
        return ParsedLog.builder()
                .rawLog(logLine)
                .build();
    }

    private Pattern getPattern() {
        return configService.getFrameworkPattern("java", "simple-java");
    }
}