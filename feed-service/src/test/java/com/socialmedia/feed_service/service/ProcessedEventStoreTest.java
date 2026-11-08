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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessedEventStoreTest {

    private static final String KEY = "processed_event:feed-service:e1";

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
    void tryStart_claimsEventWithShortLease_whenNotSeenBefore() {
        when(valueOps.setIfAbsent(KEY, "processing", Duration.ofMinutes(2))).thenReturn(true);

        assertThat(store.tryStart("e1")).isTrue();
    }

    @Test
    void tryStart_returnsFalse_whenEventWasAlreadyProcessed() {
        when(valueOps.setIfAbsent(KEY, "processing", Duration.ofMinutes(2))).thenReturn(false);
        when(valueOps.get(KEY)).thenReturn("done");

        assertThat(store.tryStart("e1")).isFalse();
    }

    @Test
    void tryStart_throws_whenAnotherConsumerIsProcessingTheEvent() {
        when(valueOps.setIfAbsent(KEY, "processing", Duration.ofMinutes(2))).thenReturn(false);
        when(valueOps.get(KEY)).thenReturn("processing");

        assertThatThrownBy(() -> store.tryStart("e1")).isInstanceOf(EventInProgressException.class);
    }

    @Test
    void markProcessed_replacesLeaseWithRetainedMarker() {
        store.markProcessed("e1");

        verify(valueOps).set(KEY, "done", Duration.ofDays(7));
    }

    @Test
    void release_removesLeaseSoEventCanBeRetried() {
        store.release("e1");

        verify(redisTemplate).delete(KEY);
    }
}
