package com.socialmedia.user_service.dto;

public record AuthResponse(
        String token,
        String username
) {}