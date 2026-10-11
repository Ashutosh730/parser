package com.logAnalyzer.core.controller;

import com.logAnalyzer.core.model.StorageProperties;
import com.logAnalyzer.auth.entity.User;
import com.logAnalyzer.auth.repository.UserRepository;
import com.logAnalyzer.core.entity.LogSession;
import com.logAnalyzer.core.model.SessionResponse;
import com.logAnalyzer.core.record.StoreRequest;
import com.logAnalyzer.core.record.StoredObject;
import com.logAnalyzer.core.repository.LogSessionRepository;
import com.logAnalyzer.core.service.LogPipelineService;
import com.logAnalyzer.core.service.impl.LocalStorageServiceImpl;
import com.logAnalyzer.core.service.impl.LogSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogUploadControllerTest {

    @Mock private LocalStorageServiceImpl storageService;
    @Mock private LogPipelineService pipelineService;
    @Mock private LogSessionServiceImpl sessionService;
    @Mock private LogSessionRepository sessionRepository;
    @Mock private StorageProperties storageProperties;
    @Mock private UserRepository userRepository;
    @Mock private MultipartFile file;
    @Mock private UserDetails userDetails;
    @Mock private User user;
    @Mock private LogSession session;

    private LogController controller;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties();
        props.setMaxObjectSizeBytes(10L * 1024 * 1024);
        controller = new LogController(storageService, pipelineService, sessionService, sessionRepository, userRepository, props);
    }

    @Test
    void upload_shouldReturnBadRequestForInvalidFile() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("invalid.exe");

        ResponseEntity<SessionResponse> response = controller.upload(file, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Only .log and .txt files are allowed", response.getBody().getMessage());
        assertEquals("invalid.exe", response.getBody().getFileName());
        verifyNoInteractions(storageService, pipelineService, sessionService, userRepository);
    }

    @Test
    void upload_shouldReturnBadRequestForEmptyFile() {
        when(file.isEmpty()).thenReturn(true);
        when(file.getOriginalFilename()).thenReturn("application.log");

        ResponseEntity<SessionResponse> response = controller.upload(file, userDetails);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("File is empty", response.getBody().getMessage());
        verifyNoInteractions(storageService, pipelineService, sessionService, userRepository);
    }

    @Test
    void upload_shouldStoreCreateAndProcessSession() throws Exception {
        String content = "INFO startup\n";
        StoredObject storedObject = new StoredObject("logs/application.log", content.getBytes(StandardCharsets.UTF_8).length, "text/plain", "local");
        givenValidFileAndUser();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        when(file.getSize()).thenReturn((long) content.getBytes(StandardCharsets.UTF_8).length);
        when(file.getContentType()).thenReturn("text/plain");
        when(storageService.store(any(StoreRequest.class))).thenReturn(storedObject);

        ResponseEntity<SessionResponse> response = controller.upload(file, userDetails);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("File uploaded successfully", response.getBody().getMessage());
        assertEquals("application.log", response.getBody().getFileName());
        assertNotNull(response.getBody().getSessionId());

        InOrder order = inOrder(storageService, sessionService, pipelineService);
        order.verify(storageService).store(any(StoreRequest.class));
        order.verify(sessionService).create(anyString(), eq("logs/application.log"), eq("user-1"), anyString());
        order.verify(pipelineService).enqueueFile(anyString(), eq("logs/application.log"));
    }

    @Test
    void upload_shouldRejectUnknownUser() {
        givenValidFile();
        when(userDetails.getUsername()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> controller.upload(file, userDetails));
        verifyNoInteractions(storageService, pipelineService, sessionService);
    }

    @Test
    void upload_shouldPropagateStorageFailure() throws Exception {
        givenValidFileAndUser();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("INFO startup\n".getBytes(StandardCharsets.UTF_8)));
        when(file.getSize()).thenReturn(12L);
        when(file.getContentType()).thenReturn("text/plain");
        when(storageService.store(any(StoreRequest.class))).thenThrow(new RuntimeException("Storage failure"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> controller.upload(file, userDetails));

        assertEquals("Storage failure", exception.getMessage());
        verify(sessionService, never()).create(anyString(), anyString(), anyString(), anyString());
        verifyNoInteractions(pipelineService);
    }

    @Test
    void upload_shouldReturnAcceptedWhenProcessingIsEnqueued() throws Exception {
        givenValidFileAndUser();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("INFO startup\n".getBytes(StandardCharsets.UTF_8)));
        when(file.getSize()).thenReturn(12L);
        when(file.getContentType()).thenReturn("text/plain");
        when(storageService.store(any(StoreRequest.class))).thenReturn(new StoredObject("logs/application.log", 12L, "text/plain", "local"));
        when(sessionService.create(anyString(), eq("logs/application.log"), eq("user-1"), anyString())).thenReturn("session-1");

        ResponseEntity<SessionResponse> response = controller.upload(file, userDetails);

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        verify(pipelineService).enqueueFile(anyString(), eq("logs/application.log"));
    }

    @Test
    void getSessions_shouldReturnMappedSessions() {
        givenUser();
        when(sessionRepository.findByUserIdAndDeletedAtIsNull("user-1")).thenReturn(List.of(session));

        ResponseEntity<List<SessionResponse>> response = controller.getSessions(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(sessionRepository).findByUserIdAndDeletedAtIsNull("user-1");
    }

    @Test
    void getSessions_shouldReturnEmptyListWhenNoSessionsExist() {
        givenUser();
        when(sessionRepository.findByUserIdAndDeletedAtIsNull("user-1")).thenReturn(List.of());

        ResponseEntity<List<SessionResponse>> response = controller.getSessions(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getSessionById_shouldReturnMappedSessionWhenFound() {
        givenUser();
        when(sessionRepository.findByIdAndUserId("session-1", "user-1"))
                .thenReturn(Optional.of(session));

        ResponseEntity<SessionResponse> response =
                controller.getSessionById("session-1", userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getSessionById_shouldReturnNotFoundWhenSessionIsMissing() {
        givenUser();
        when(sessionRepository.findByIdAndUserId("session-1", "user-1"))
                .thenReturn(Optional.empty());

        ResponseEntity<SessionResponse> response = controller.getSessionById("session-1", userDetails);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getSessionById_shouldReturnNotFoundWhenUserIdIsEmpty() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn("");

        ResponseEntity<SessionResponse> response =
                controller.getSessionById("session-1", userDetails);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void getSessionById_shouldRejectUnknownUser() {
        when(userDetails.getUsername()).thenReturn("missing@example.com");
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> controller.getSessionById("session-1", userDetails));
        verifyNoInteractions(sessionRepository);
    }

    private void givenValidFile() {
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("application.log");
    }

    private void givenUser() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(user.getId()).thenReturn("user-1");
    }

    private void givenValidFileAndUser() {
        givenValidFile();
        givenUser();
    }
}
