package com.socialmedia.feed_service.dto;

import java.time.Instant;

public record PostCreatedEvent(String postId, String authorId, Instant createdAt) {}
