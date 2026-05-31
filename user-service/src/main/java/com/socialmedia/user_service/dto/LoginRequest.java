package com.socialmedia.user_service.dto;

import com.socialmedia.user_service.entity.Provider;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull Provider provider,
        String username,
        String password,
        String idToken
) {}
