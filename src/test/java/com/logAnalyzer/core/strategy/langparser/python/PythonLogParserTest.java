package com.logAnalyzer.core.strategy.langparser.python;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class PythonLogParserTest {

    private final PythonLogParser parser = new PythonLogParser();

    @Test
    void pythonParser_shouldCurrentlyReturnUnsupportedDefaults() {
        assertFalse(parser.isParsable(List.of("2026-09-27 ERROR failure")));
        assertFalse(parser.isPrimaryLine("2026-09-27 ERROR failure"));
        assertNull(parser.parse("2026-09-27 ERROR failure"));
    }
}
