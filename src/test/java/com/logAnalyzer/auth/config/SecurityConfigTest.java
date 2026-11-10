package com.logAnalyzer.auth.config;

import com.logAnalyzer.auth.filter.JwtAuthFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = {SecurityConfig.class, SecurityConfigTest.FilterConfig.class})
class SecurityConfigTest {

    @Autowired
    private FilterChainProxy securityFilterChain;

    @Test
    void asyncDispatchForAuthenticatedApiRequestIsNotRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/sessions/session-1/download");
        request.setDispatcherType(DispatcherType.ASYNC);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain terminalChain = (servletRequest, servletResponse) -> {
        };

        assertDoesNotThrow(() -> securityFilterChain.doFilter(request, response, terminalChain));
    }

    @TestConfiguration
    @EnableWebMvc
    static class FilterConfig {

        @Bean
        JwtAuthFilter jwtAuthFilter() throws Exception {
            JwtAuthFilter filter = mock(JwtAuthFilter.class);
            doAnswer(invocation -> {
                FilterChain chain = invocation.getArgument(2);
                chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(filter).doFilter(any(), any(), any());
            return filter;
        }
    }
}
