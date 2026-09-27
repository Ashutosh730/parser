package com.logAnalyzer.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldRejectNullEmptyAndPathLikeNames() {
        assertEquals("File is empty", LogFileUtil.validateFile(null));
        assertEquals("Invalid file", LogFileUtil.validateFileName(null));
        assertEquals("Invalid file", LogFileUtil.validateFileName("folder/") );
        assertNull(LogFileUtil.validateFileName("C:/logs/app.log"));
        assertEquals("Invalid file", LogFileUtil.validateFileName(".log"));
        assertEquals("Invalid file", LogFileUtil.validateFileName("app."));
    }

    @Test
    void shouldGenerateUniqueNamesWithSanitizedPathAndExtension() {
        String generated = LogFileUtil.generateUniqueFileName("/logs/application.log?download=true");

        assertTrue(generated.startsWith("application_"));
        assertTrue(generated.endsWith(".log"));
        assertNotEquals(generated,
                LogFileUtil.generateUniqueFileName("/logs/application.log"));
    }
}
