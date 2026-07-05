package com.socialmedia.feed_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventStoreTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private ProcessedEventStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void isProcessed_returnsTrue_whenMarkerExists() {
        when(redisTemplate.hasKey("processed_event:feed-service:e1")).thenReturn(true);

        assertThat(store.isProcessed("e1")).isTrue();
    }

    @Test
    void isProcessed_returnsFalse_whenMarkerMissing() {
        when(redisTemplate.hasKey("processed_event:feed-service:e1")).thenReturn(false);

        assertThat(store.isProcessed("e1")).isFalse();
    }

    @Test
    void markProcessed_storesMarkerWithRetention() {
        store.markProcessed("e1");

        verify(valueOps).set("processed_event:feed-service:e1", "1", Duration.ofDays(7));
    }
}
