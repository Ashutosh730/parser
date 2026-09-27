package com.logAnalyzer.auth.controller;

import com.logAnalyzer.auth.dto.AuthResponse;
import com.logAnalyzer.auth.dto.LoginRequest;
import com.logAnalyzer.auth.dto.RegisterRequest;
import com.logAnalyzer.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserDetails userDetails;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
    }

    @Test
    void me_shouldReturnUsernameAndAuthorities() {
        when(userDetails.getUsername()).thenReturn("user@example.com");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
            .when(userDetails).getAuthorities();

        ResponseEntity<?> response = controller.me(userDetails);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("user@example.com [ROLE_USER]", response.getBody());
    }

    @Test
    void register_shouldReturnCreatedResponseFromService() {
        RegisterRequest request = new RegisterRequest();
        AuthResponse expected = AuthResponse.builder()
                .token("token")
                .email("user@example.com")
                .role("USER")
                .build();
        when(authService.register(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = controller.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(authService).register(request);
    }

    @Test
    void login_shouldReturnOkResponseFromService() {
        LoginRequest request = new LoginRequest();
        AuthResponse expected = AuthResponse.builder().token("token").build();
        when(authService.login(request)).thenReturn(expected);

        ResponseEntity<AuthResponse> response = controller.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());
        verify(authService).login(request);
    }
}
