package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthInitResponse;
import com.socialmedia.user_service.exception.InvalidTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "oauth:state:";

    private final StringRedisTemplate redisTemplate;

    public OAuthInitResponse initiate() {
        String state = UUID.randomUUID().toString();
        String nonce = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(KEY_PREFIX + state, nonce, TTL);
        return new OAuthInitResponse(state, nonce);
    }

    public String validateAndConsume(String state) {
        String key = KEY_PREFIX + state;
        String nonce = redisTemplate.opsForValue().get(key);
        if (nonce == null) {
            throw new InvalidTokenException("Invalid or expired OAuth state");
        }
        redisTemplate.delete(key);
        return nonce;
    }
}
