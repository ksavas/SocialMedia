package com.socialmedia.feed_service.dto;

public record UserUnfollowedEvent(String followerId, String followingId) {}
