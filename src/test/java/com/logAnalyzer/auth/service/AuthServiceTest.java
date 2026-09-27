package com.logAnalyzer.auth.service;

import com.logAnalyzer.auth.dto.AuthResponse;
import com.logAnalyzer.auth.dto.LoginRequest;
import com.logAnalyzer.auth.dto.RegisterRequest;
import com.logAnalyzer.auth.entity.Role;
import com.logAnalyzer.auth.entity.User;
import com.logAnalyzer.auth.repository.UserRepository;
import com.logAnalyzer.core.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private Authentication authentication;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void register_shouldSaveUserAndReturnTokenResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("user@example.com");
        request.setPassword("plain-password");
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("user@example.com", saved.getEmail());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals("Test User", saved.getName());
        assertEquals(Role.USER, saved.getRole());
        assertEquals("jwt-token", response.getToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals("USER", response.getRole());
    }

    @Test
    void register_shouldRejectExistingEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request));

        assertEquals("Email already registered: existing@example.com", exception.getMessage());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_shouldAuthenticateAndReturnTokenResponse() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");
        User user = User.builder()
                .id("user-1")
                .email("user@example.com")
                .name("Test User")
                .role(Role.USER)
                .build();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("Test User", response.getName());
        assertEquals("USER", response.getRole());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void login_shouldConvertAuthenticationFailureToBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid"));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldRejectMissingUserAfterSuccessfulAuthentication() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("password");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> authService.login(request));
        verifyNoInteractions(jwtService);
    }
}
