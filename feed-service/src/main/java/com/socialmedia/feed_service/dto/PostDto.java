package com.socialmedia.feed_service.dto;

import java.time.Instant;

public record PostDto(
        String id,
        String authorId,
        String authorUsername,
        String content,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        Instant createdAt
) {}
