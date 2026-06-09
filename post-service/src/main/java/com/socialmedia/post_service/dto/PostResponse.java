package com.socialmedia.post_service.dto;

import java.time.LocalDateTime;

public record PostResponse(
        String id,
        String authorId,
        String authorUsername,
        String content,
        long likeCount,
        long commentCount,
        boolean likedByMe,
        LocalDateTime createdAt
) {}
