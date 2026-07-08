package com.socialmedia.notification_service.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String recipientId,
        String type,
        String actorId,
        String postId,
        boolean read,
        Instant createdAt
) {}
