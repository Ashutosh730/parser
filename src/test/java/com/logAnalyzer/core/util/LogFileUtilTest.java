package com.logAnalyzer.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogFileUtilTest {

    @Test
    void shouldAcceptValidLogNames() {
        assertNull(LogFileUtil.validateFileName("app.log"));
        assertNull(LogFileUtil.validateFileName("https://example.com/logs/application.txt?download=1"));
    }

    @Test
    void shouldRejectImageUrlsAndInvalidNames() {
        assertEquals("Invalid file", LogFileUtil.validateFileName(""));
        assertEquals("Only .log and .txt files are allowed", LogFileUtil.validateFileName("https://example.com/images/screenshot.png"));
        assertEquals("Only .log and .txt files are allowed", LogFileUtil.validateFileName("archive.csv"));
    }

    @Test
    void shouldValidateMultipartFilesUsingNormalizedFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "https://example.com/logs/application.txt?download=1",
                "text/plain",
                "INFO Test".getBytes()
        );

        assertNull(LogFileUtil.validateFile(file));
    }
}
