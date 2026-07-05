package com.socialmedia.feed_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProcessedEventStore {

    private static final String KEY_PREFIX = "processed_event:feed-service:";
    private static final Duration RETENTION = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isProcessed(String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + eventId));
    }

    public void markProcessed(String eventId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + eventId, "1", RETENTION);
    }
}
