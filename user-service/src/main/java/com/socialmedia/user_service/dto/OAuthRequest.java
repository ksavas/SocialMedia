package com.socialmedia.user_service.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthRequest(
        @NotBlank String idToken
) {}