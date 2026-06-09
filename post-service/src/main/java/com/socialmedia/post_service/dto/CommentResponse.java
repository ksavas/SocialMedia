package com.socialmedia.post_service.dto;

import java.time.LocalDateTime;

public record CommentResponse(
        String id,
        String postId,
        String authorId,
        String authorUsername,
        String content,
        LocalDateTime createdAt
) {}
