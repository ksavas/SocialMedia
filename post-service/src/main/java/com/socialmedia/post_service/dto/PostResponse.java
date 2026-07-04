package com.socialmedia.post_service.dto;

import java.time.Instant;

public record PostResponse(
        String id,
        String authorId,
        String authorUsername,
        String content,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        Instant createdAt
) {}
