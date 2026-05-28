package com.logAnalyzer.parser.core.langparser.java.subparser;

import com.logAnalyzer.parser.model.parsed.ParsedLog;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SpringBootParserTest {

    private static final String SPRING_BOOT_REGEX =
            "^(?<timestamp>\\S+)\\s+(?<level>\\w+)\\s+(?<pid>\\d+)\\s+---\\s+\\[(?<context>.*?)\\]\\s+\\[(?<thread>.*?)\\]\\s+(?<className>\\S+)\\s+:\\s+(?<message>.*)$";

    @Test
    void parse_shouldKeepStackTraceWithPrimaryMessage() {
        SpringBootParser parser = new SpringBootParser();
        ReflectionTestUtils.setField(parser, "logFormat", SPRING_BOOT_REGEX);

        String entry = "2026-05-28T09:48:35.877Z  WARN 12044 --- [parser] [           main] org.hibernate.orm.jdbc.error             : HHH100046: Could not obtain connection"
                + "\n\tat org.hibernate.engine.jdbc.spi.SqlExceptionHelper.convert(SqlExceptionHelper.java:115)"
                + "\nCaused by: java.net.ConnectException: Connection refused"
                + "\n\t... 37 common frames omitted";

        ParsedLog parsed = parser.parse(entry);

        assertEquals("2026-05-28T09:48:35.877Z", parsed.getTimestamp());
        assertEquals("WARN", parsed.getLevel());
        assertEquals("12044", parsed.getPid());
        assertEquals("main", parsed.getThread());
        assertEquals("org.hibernate.orm.jdbc.error", parsed.getClassName());
        assertTrue(parsed.getMessage().contains("HHH100046: Could not obtain connection"));
        assertTrue(parsed.getMessage().contains("Caused by: java.net.ConnectException"));
        assertTrue(parsed.getMessage().contains("... 37 common frames omitted"));
        assertNull(parsed.getRawLog());
    }

    @Test
    void canParse_shouldReturnTrueForMultilineStackTraceEntry() {
        SpringBootParser parser = new SpringBootParser();
        ReflectionTestUtils.setField(parser, "logFormat", SPRING_BOOT_REGEX);

        String entry = "2026-05-28T09:48:35.877Z  WARN 12044 --- [parser] [           main] org.hibernate.orm.jdbc.error             : HHH100046"
                + "\n\tat org.hibernate.orm.SomeClass.someMethod(SomeClass.java:10)";

        assertTrue(parser.canParse(entry));
    }
}

