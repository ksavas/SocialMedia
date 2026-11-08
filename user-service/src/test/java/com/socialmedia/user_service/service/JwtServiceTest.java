package com.socialmedia.user_service.service;

import com.socialmedia.user_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.setSecretKey("testsecretkeytestsecretkeytestsecretkey12345678");
        jwtService.setExpiration(86400000L);
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenValidToken() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}