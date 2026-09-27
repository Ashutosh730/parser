package com.logAnalyzer.core.strategy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogParserFactoryTest {

    @Test
    void getParser_shouldReturnFirstParsableParser() {
        LogParser first = mock(LogParser.class);
        LogParser second = mock(LogParser.class);
        List<String> lines = List.of("line");
        when(first.isParsable(lines)).thenReturn(false);
        when(second.isParsable(lines)).thenReturn(true);

        LogParser result = new LogParserFactory(List.of(first, second)).getParser(lines);

        assertSame(second, result);
    }

    @Test
    void getParser_shouldThrowWhenNoParserMatches() {
        LogParser parser = mock(LogParser.class);
        List<String> lines = List.of("unsupported");
        when(parser.isParsable(lines)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new LogParserFactory(List.of(parser)).getParser(lines));

        assertEquals("No suitable parser found for log entry: [unsupported]", exception.getMessage());
    }
}
