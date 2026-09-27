package com.logAnalyzer.core.strategy.langparser.java.subparser;

import com.logAnalyzer.core.enums.DetectedFramework;
import com.logAnalyzer.core.enums.LogLevel;
import com.logAnalyzer.core.model.ParsedLog;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSubParserTest {

    private static final String SPRING_BOOT_REGEX =
            "^(?<timestamp>\\S+)\\s+(?<level>\\w+)\\s+(?<pid>\\d+)\\s+---\\s+\\[(?<context>.*?)\\]\\s+\\[(?<thread>.*?)\\]\\s+(?<className>\\S+)\\s+:\\s+(?<message>.*)$";
    private static final String LOG4J_REGEX =
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2}[\\sT]\\d{2}:\\d{2}:\\d{2}[,\\.]\\d+)\\s+\\[(?<thread>.*?)\\]\\s+(?<level>\\w+)\\s+(?<className>[\\w\\.$]+)\\s+-\\s+(?<message>.*)$";
    private static final String SIMPLE_REGEX =
            "^(?<timestamp>\\w+\\s+\\d{1,2},\\s+\\d{4}\\s+\\d{1,2}:\\d{2}:\\d{2}\\s+[AP]M)\\s+(?<className>[\\w\\.]+)\\s+(?<method>\\w+)\\n(?<level>\\w+):\\s+(?<message>.*)$";

    @Test
    void springBootParser_shouldParseAndAppendContinuationLines() {
        SpringBootParser parser = new SpringBootParser();
        ReflectionTestUtils.setField(parser, "logFormat", SPRING_BOOT_REGEX);
        String entry = "2026-09-27T10:15:30Z ERROR 42 --- [app] [ main ] com.example.App : failure"
                + "\n\tat com.example.App.run(App.java:1)";

        assertTrue(parser.canParse(entry));
        ParsedLog result = parser.parse(entry);

        assertEquals(LogLevel.ERROR, result.getLevel());
        assertEquals("42", result.getPid());
        assertEquals("main", result.getThread());
        assertEquals("com.example.App", result.getClassName());
        assertEquals(DetectedFramework.SPRING_BOOT, result.getFramework());
        assertTrue(result.getMessage().contains("failure"));
        assertTrue(result.getMessage().contains("App.run"));
    }

    @Test
    void springBootParser_shouldReturnRawLogForNullAndUnmatchedInput() {
        SpringBootParser parser = new SpringBootParser();
        ReflectionTestUtils.setField(parser, "logFormat", SPRING_BOOT_REGEX);

        assertEquals(null, parser.parse(null).getRawLog());
        assertFalse(parser.canParse("not a spring boot line"));
        assertEquals("not a spring boot line", parser.parse("not a spring boot line").getRawLog());
    }

    @Test
    void log4jParser_shouldParseCommaAndDotTimestampFormats() {
        Log4jParser parser = new Log4jParser();
        ReflectionTestUtils.setField(parser, "logFormat", LOG4J_REGEX);

        ParsedLog comma = parser.parse("2026-09-27 10:15:30,123 [main] WARN com.example.App - warning");

        assertEquals(LogLevel.WARN, comma.getLevel());
        assertEquals(DetectedFramework.LOG4J, comma.getFramework());
        assertThrows(java.time.format.DateTimeParseException.class, () ->
            parser.parse("2026-09-27T10:15:30.123 [main] ERROR com.example.App - failure"));
        assertTrue(parser.canParse("2026-09-27 10:15:30,123 [main] WARN com.example.App - warning"));
        assertFalse(parser.canParse(null));
    }

    @Test
    void log4jParser_shouldReturnRawLogForNullAndUnmatchedInput() {
        Log4jParser parser = new Log4jParser();
        ReflectionTestUtils.setField(parser, "logFormat", LOG4J_REGEX);

        assertEquals(null, parser.parse(null).getRawLog());
        assertEquals("raw", parser.parse("raw").getRawLog());
    }

    @Test
    void simpleJavaParser_shouldDetectInputAndReturnRawLogForUnmatchedInput() {
        SimpleJavaParser parser = new SimpleJavaParser();
        ReflectionTestUtils.setField(parser, "logFormat", SIMPLE_REGEX);
        String entry = "Sep 27, 2026 10:15:30 AM Example run\nERROR: failure";

        assertTrue(parser.canParse(entry));
        assertThrows(java.time.format.DateTimeParseException.class, () -> parser.parse(entry));
        assertEquals("raw", parser.parse("raw").getRawLog());
    }
}
