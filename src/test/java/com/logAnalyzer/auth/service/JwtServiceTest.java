package com.logAnalyzer.auth.service;

import com.logAnalyzer.auth.entity.Role;
import com.logAnalyzer.auth.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET =
            "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdTZWNyZXRLZXlGb3JUZXN0aW5nMTIzNDU2Nzg=";

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 60_000L);
        user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .role(Role.ADMIN)
                .build();
    }

    @Test
    void generateToken_shouldContainUserClaims() {
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("user@example.com", jwtService.extractEmail(token));
        assertEquals("user-1", jwtService.extractUserId(token));
        assertTrue(jwtService.isTokenValid(token, "user@example.com"));
    }

    @Test
    void isTokenValid_shouldRejectDifferentEmailAndMalformedToken() {
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, "other@example.com"));
        assertFalse(jwtService.isTokenValid("not-a-jwt", "user@example.com"));
        assertThrows(JwtException.class, () -> jwtService.extractEmail("not-a-jwt"));
    }

    @Test
    void isTokenValid_shouldRejectExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1L);
        String token = jwtService.generateToken(user);

        assertFalse(jwtService.isTokenValid(token, "user@example.com"));
    }
}
