package com.logAnalyzer.core.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSubParserUtilTest {

    @Test
    void extractHeaderLine_shouldHandleNullSingleAndMultilineInput() {
        assertEquals("", LogSubParserUtil.extractHeaderLine(null));
        assertEquals("header", LogSubParserUtil.extractHeaderLine("header"));
        assertEquals("header", LogSubParserUtil.extractHeaderLine("header\ncontinuation"));
    }

    @Test
    void extractContinuationLines_shouldHandleNullMissingAndTrailingNewline() {
        assertEquals("", LogSubParserUtil.extractContinuationLines(null));
        assertEquals("", LogSubParserUtil.extractContinuationLines("header"));
        assertEquals("", LogSubParserUtil.extractContinuationLines("header\n"));
        assertEquals("one\ntwo", LogSubParserUtil.extractContinuationLines("header\none\ntwo"));
    }

    @Test
    void parseTimestamp_shouldParseIsoWhenPatternIsEmpty() {
        Instant expected = Instant.parse("2026-09-27T10:15:30Z");

        assertEquals(expected, LogSubParserUtil.parseTimestamp(expected.toString(), ""));
        assertNull(LogSubParserUtil.parseTimestamp(null, "yyyy-MM-dd"));
    }

    @Test
    void parseTimestamp_shouldParseConfiguredLocalDateTimePattern() {
        Instant result = LogSubParserUtil.parseTimestamp(
                "2026-09-27 10:15:30",
                "yyyy-MM-dd HH:mm:ss");

        assertEquals("2026-09-27T10:15:30Z", result.toString());
    }
}
