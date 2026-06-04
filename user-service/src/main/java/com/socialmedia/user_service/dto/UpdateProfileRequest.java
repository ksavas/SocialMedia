package com.socialmedia.user_service.dto;

public record UpdateProfileRequest(
        String username,
        String password,
        String email
) {}
