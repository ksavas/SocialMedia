package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.OAuthInitResponse;
import com.socialmedia.user_service.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthStateServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private OAuthStateService oauthStateService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void initiate_shouldReturnNonNullStateAndNonce() {
        OAuthInitResponse response = oauthStateService.initiate();

        assertThat(response.state()).isNotNull().isNotEmpty();
        assertThat(response.nonce()).isNotNull().isNotEmpty();
    }

    @Test
    void initiate_shouldReturnUniqueValuesEachTime() {
        OAuthInitResponse first = oauthStateService.initiate();
        OAuthInitResponse second = oauthStateService.initiate();

        assertThat(first.state()).isNotEqualTo(second.state());
        assertThat(first.nonce()).isNotEqualTo(second.nonce());
    }

    @Test
    void initiate_shouldStoreNonceInRedisWithFiveMinuteTtl() {
        OAuthInitResponse response = oauthStateService.initiate();

        verify(valueOps).set(
                eq("oauth:state:" + response.state()),
                eq(response.nonce()),
                eq(Duration.ofMinutes(5))
        );
    }

    @Test
    void validateAndConsume_shouldReturnNonce_whenValidState() {
        when(valueOps.get("oauth:state:valid-state")).thenReturn("stored-nonce");

        String nonce = oauthStateService.validateAndConsume("valid-state");

        assertThat(nonce).isEqualTo("stored-nonce");
    }

    @Test
    void validateAndConsume_shouldDeleteKeyFromRedis_afterConsumption() {
        when(valueOps.get("oauth:state:valid-state")).thenReturn("stored-nonce");

        oauthStateService.validateAndConsume("valid-state");

        verify(redisTemplate).delete("oauth:state:valid-state");
    }

    @Test
    void validateAndConsume_shouldThrow_whenStateNotFound() {
        when(valueOps.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> oauthStateService.validateAndConsume("invalid-state"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid or expired OAuth state");
    }
}
