package com.logAnalyzer.core.service.impl;

import com.logAnalyzer.core.model.StorageProperties;
import com.logAnalyzer.core.record.StoreRequest;
import com.logAnalyzer.core.record.StoredObject;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalStorageServiceImplTest {

    @Test
    void store_shouldCreateConfiguredDirectoryAndCopyFileWithUniqueName() throws IOException {
        StorageProperties props = new StorageProperties();
        props.setLocalBaseDir("target/test-uploads-" + UUID.randomUUID());
        props.setKeyPrefix("logs");
        byte[] content = "INFO application started".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "application.log", "text/plain", content);

        StoredObject stored = new LocalStorageServiceImpl(props).store(new StoreRequest(
                file.getInputStream(), file.getSize(), file.getContentType(), "user-1", "session-1", file.getOriginalFilename(), java.util.Map.of()
        ));

        Path storedPath = Path.of(System.getProperty("user.dir"), props.getLocalBaseDir(), "logs", "user-1", "session-1", "application.log").normalize();
        assertTrue(Files.exists(storedPath));
        assertTrue(storedPath.getFileName().toString().endsWith(".log"));
        assertArrayEquals(content, Files.readAllBytes(storedPath));
        assertEquals("logs/user-1/session-1/application.log", stored.key());
    }

    @Test
    void store_shouldWrapInputStreamFailure() throws IOException {
        StorageProperties props = new StorageProperties();
        props.setLocalBaseDir("target/test-uploads-" + UUID.randomUUID());
        org.springframework.web.multipart.MultipartFile file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("application.log");
        when(file.getInputStream()).thenThrow(new IOException("read failed"));

        assertThrows(IOException.class, () -> new LocalStorageServiceImpl(props).store(new StoreRequest(
                file.getInputStream(), 0L, "text/plain", "user-1", "session-1", file.getOriginalFilename(), java.util.Map.of()
        )));
    }
}