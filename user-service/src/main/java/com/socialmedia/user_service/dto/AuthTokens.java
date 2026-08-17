package com.socialmedia.user_service.dto;

public record AuthTokens(
        AuthResponse response,
        String refreshToken
) {}
