package com.logAnalyzer.core.controller;

import com.logAnalyzer.core.exception.EmailAlreadyExistsException;
import com.logAnalyzer.core.exception.SessionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailAlreadyExists_shouldReturnConflict() {
        ResponseEntity<?> response = handler.handleEmailAlreadyExists(
                new EmailAlreadyExistsException("Email already exists"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertError(response, 409, "Email already exists");
    }

    @Test
    void handleBadCredentials_shouldReturnConflictWithBadRequestBodyStatus() {
        ResponseEntity<?> response = handler.handleBadCredentials(
                new BadCredentialsException("Invalid credentials"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertError(response, 400, "Invalid credentials");
    }

    @Test
    void handleSessionNotFound_shouldReturnConflictWithBadRequestBodyStatus() {
        ResponseEntity<?> response = handler.handleBadCredentials(
                new SessionNotFoundException("Session not found"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertError(response, 400, "Session not found");
    }

    private void assertError(ResponseEntity<?> response, int status, String message) {
        assertNotNull(response.getBody());
        com.logAnalyzer.core.model.ErrorResponse error =
                (com.logAnalyzer.core.model.ErrorResponse) response.getBody();
        assertEquals(status, error.getStatus());
        assertEquals(message, error.getMessage());
    }
}
