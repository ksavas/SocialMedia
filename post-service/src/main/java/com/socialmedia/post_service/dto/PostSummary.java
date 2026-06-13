package com.socialmedia.post_service.dto;

import java.time.LocalDateTime;

public record PostSummary(String id, String authorId, LocalDateTime createdAt) {}
