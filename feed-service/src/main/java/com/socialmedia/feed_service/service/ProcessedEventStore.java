package com.socialmedia.feed_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProcessedEventStore {

    private static final String KEY_PREFIX = "processed_event:feed-service:";
    private static final String PROCESSING = "processing";
    private static final String DONE = "done";
    private static final Duration LEASE = Duration.ofMinutes(2);
    private static final Duration RETENTION = Duration.ofDays(7);

    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryStart(String eventId) {
        String key = KEY_PREFIX + eventId;
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, PROCESSING, LEASE))) {
            return true;
        }
        String marker = redisTemplate.opsForValue().get(key);
        if (marker == null || PROCESSING.equals(marker)) {
            throw new EventInProgressException(eventId);
        }
        return false;
    }

    public void markProcessed(String eventId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + eventId, DONE, RETENTION);
    }

    public void release(String eventId) {
        redisTemplate.delete(KEY_PREFIX + eventId);
    }
}
