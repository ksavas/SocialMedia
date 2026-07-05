package com.socialmedia.feed_service.dto;

import java.time.Instant;

public record PostSummary(String id, String authorId, Instant createdAt) {}
