package com.socialmedia.user_service.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String displayName,
        long followerCount,
        long followingCount,
        boolean isFollowing
) {}
