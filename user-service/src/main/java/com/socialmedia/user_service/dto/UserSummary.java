package com.socialmedia.user_service.dto;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String username,
        String displayName,
        boolean isFollowing
) {}
