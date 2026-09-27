package com.logAnalyzer.core.service.impl;

import com.logAnalyzer.core.config.FileStorageConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalStorageServiceImplTest {

    @Test
    void upload_shouldCreateConfiguredDirectoryAndCopyFileWithUniqueName() throws IOException {
        FileStorageConfig config = new FileStorageConfig();
        config.setUploadDir("target/test-uploads-" + UUID.randomUUID());
        byte[] content = "INFO application started".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "application.log", "text/plain", content);

        Path storedPath = new LocalStorageServiceImpl(config).upload(file);

        assertTrue(Files.exists(storedPath));
        assertTrue(storedPath.getFileName().toString().endsWith(".log"));
        assertArrayEquals(content, Files.readAllBytes(storedPath));
    }

    @Test
    void upload_shouldWrapInputStreamFailure() throws IOException {
        FileStorageConfig config = new FileStorageConfig();
        config.setUploadDir("target/test-uploads-" + UUID.randomUUID());
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("application.log");
        when(file.getInputStream()).thenThrow(new IOException("read failed"));

        assertThrows(RuntimeException.class, () -> new LocalStorageServiceImpl(config).upload(file));
    }
}