package com.logAnalyzer.auth.filter;

import com.logAnalyzer.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    private TestableJwtAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new TestableJwtAuthFilter(jwtService, userDetailsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        filter.run(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractEmail("token");
    }

    @Test
    void doFilter_shouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        request.addHeader("Authorization", "Basic credentials");

        filter.run(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractEmail("credentials");
    }

    @Test
    void doFilter_shouldSetAuthenticationForValidToken() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        UserDetails details = User.withUsername("user@example.com")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
        when(jwtService.extractEmail("token")).thenReturn("user@example.com");
        when(userDetailsService.loadUserByUsername("user@example.com"))
                .thenReturn(details);
        when(jwtService.isTokenValid("token", "user@example.com")).thenReturn(true);

        filter.run(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertSame(details, authentication.getPrincipal());
        assertEquals("user@example.com", authentication.getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldContinueWithoutAuthenticationForInvalidToken() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractEmail("token")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("token", "user@example.com")).thenReturn(false);

        filter.run(request, response, filterChain);

        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername("user@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldContinueWithoutAuthenticationForMalformedToken() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        when(jwtService.extractEmail("token"))
                .thenThrow(new JwtException("expired"));

        filter.run(request, response, filterChain);

        assertEquals(null, SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername("user@example.com");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_shouldNotReplaceExistingAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        Authentication existing = org.mockito.Mockito.mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existing);
        when(jwtService.extractEmail("token")).thenReturn("user@example.com");

        filter.run(request, response, filterChain);

        assertSame(existing, SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername("user@example.com");
        verify(filterChain).doFilter(request, response);
    }

    private static class TestableJwtAuthFilter extends JwtAuthFilter {
        TestableJwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
            super(jwtService, userDetailsService);
        }

        void run(MockHttpServletRequest request, MockHttpServletResponse response,
                 FilterChain filterChain) throws Exception {
            doFilterInternal(request, response, filterChain);
        }
    }
}
