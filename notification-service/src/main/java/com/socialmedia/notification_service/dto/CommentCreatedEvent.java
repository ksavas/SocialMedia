package com.socialmedia.notification_service.dto;

public record CommentCreatedEvent(String commentId, String postId, String authorId, String postAuthorId) {}
