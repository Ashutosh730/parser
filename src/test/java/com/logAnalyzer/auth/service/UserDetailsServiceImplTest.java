package com.logAnalyzer.auth.service;

import com.logAnalyzer.auth.entity.Role;
import com.logAnalyzer.auth.entity.User;
import com.logAnalyzer.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_shouldMapStoredUserToSpringUserDetails() {
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.ADMIN)
                .build();
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails details = new UserDetailsServiceImpl(userRepository)
                .loadUserByUsername("user@example.com");

        assertEquals("user@example.com", details.getUsername());
        assertEquals("encoded-password", details.getPassword());
        assertEquals("ROLE_ADMIN", details.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenEmailIsMissing() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> new UserDetailsServiceImpl(userRepository)
                        .loadUserByUsername("missing@example.com"));

        assertEquals("User not found: missing@example.com", exception.getMessage());
    }
}
