package com.socialmedia.user_service.dto;

import java.util.UUID;

public record RotatedRefreshToken(
        UUID userId,
        String token
) {}
