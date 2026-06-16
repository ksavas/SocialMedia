package com.socialmedia.notification_service.dto;

public record PostLikedEvent(String postId, String userId, String authorId) {}
