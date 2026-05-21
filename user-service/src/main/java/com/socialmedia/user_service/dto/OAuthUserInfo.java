package com.socialmedia.user_service.dto;

public record OAuthUserInfo(
        String providerId,
        String email,
        String name,
        boolean emailVerified
) {}