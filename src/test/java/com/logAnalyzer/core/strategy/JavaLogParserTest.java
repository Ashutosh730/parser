package com.logAnalyzer.core.strategy;

import com.logAnalyzer.core.model.ParsedLog;
import com.logAnalyzer.core.strategy.langparser.java.JavaLogParser;
import com.logAnalyzer.core.strategy.langparser.java.JavaSubParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JavaLogParserTest {

    @Test
    void isParsable_shouldReturnTrueWhenAnyLineMatchesJavaHint() {
        JavaLogParser parser = new JavaLogParser(List.of());

        assertTrue(parser.isParsable(List.of("2026-09-27 INFO --- [main]")));
        assertTrue(parser.isParsable(List.of("Sep 27, 2026 10:15:30 AM Example method\nERROR: failure")));
        assertTrue(parser.isParsable(List.of("2026-09-27 [main] - message")));
    }

    @Test
    void isParsable_shouldReturnFalseWhenNoLineMatches() {
        JavaLogParser parser = new JavaLogParser(List.of());

        assertFalse(parser.isParsable(List.of("plain text")));
    }

    @Test
    void isPrimaryLine_shouldRejectContinuationLinesAndAcceptPrimaryLines() {
        JavaLogParser parser = new JavaLogParser(List.of());

        assertFalse(parser.isPrimaryLine(null));
        assertFalse(parser.isPrimaryLine("   "));
        assertFalse(parser.isPrimaryLine("\tat Example.method(Example.java:1)"));
        assertFalse(parser.isPrimaryLine("Caused by: failure"));
        assertFalse(parser.isPrimaryLine("\t... 3 common frames omitted"));
        assertTrue(parser.isPrimaryLine("2026-09-27 INFO --- [main]"));
    }

    @Test
    void parse_shouldUseFirstMatchingSubParser() {
        JavaSubParser first = mock(JavaSubParser.class);
        JavaSubParser second = mock(JavaSubParser.class);
        ParsedLog expected = ParsedLog.builder().rawLog("parsed").build();
        when(first.canParse("entry")).thenReturn(false);
        when(second.canParse("entry")).thenReturn(true);
        when(second.parse("entry")).thenReturn(expected);

        ParsedLog result = new JavaLogParser(List.of(first, second)).parse("entry");

        assertSame(expected, result);
    }

    @Test
    void parse_shouldFallbackToRawLogWhenNoSubParserMatches() {
        JavaSubParser parser = mock(JavaSubParser.class);
        when(parser.canParse("entry")).thenReturn(false);

        ParsedLog result = new JavaLogParser(List.of(parser)).parse("entry");

        org.junit.jupiter.api.Assertions.assertEquals("entry", result.getRawLog());
    }
}
