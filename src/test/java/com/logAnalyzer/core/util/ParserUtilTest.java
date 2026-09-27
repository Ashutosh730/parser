package com.logAnalyzer.core.util;

import com.logAnalyzer.core.enums.DetectedLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParserUtilTest {

    @Test
    void getDetectedLanguage_shouldDetectSupportedLanguagesCaseInsensitively() {
        assertEquals(DetectedLanguage.JAVA, ParserUtil.getDetectedLanguage("JavaLogParser"));
        assertEquals(DetectedLanguage.PYTHON, ParserUtil.getDetectedLanguage("pythonParser"));
        assertEquals(DetectedLanguage.JAVA, ParserUtil.getDetectedLanguage("JavaScriptParser"));
    }

    @Test
    void getDetectedLanguage_shouldReturnUnknownForOtherNames() {
        assertEquals(DetectedLanguage.UNKNOWN, ParserUtil.getDetectedLanguage("RubyParser"));
    }
}
