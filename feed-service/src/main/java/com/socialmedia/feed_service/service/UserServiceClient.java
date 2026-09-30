package com.socialmedia.feed_service.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class UserServiceClient {

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public UserServiceClient(@Value("${services.user-service.url}") String baseUrl,
                             @Qualifier("userServiceCircuitBreaker") CircuitBreaker circuitBreaker) {
        this.restClient = RestClientFactory.create(baseUrl);
        this.circuitBreaker = circuitBreaker;
    }

    public List<String> getFollowingIds(String userId) {
        List<String> ids = circuitBreaker.executeSupplier(() -> restClient.get()
                .uri("/api/internal/users/{userId}/following-ids", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {}));
        if (ids == null) {
            throw new IllegalStateException("Missing following IDs");
        }
        return ids;
    }

    public long getFollowerCount(String userId) {
        Long count = circuitBreaker.executeSupplier(() -> restClient.get()
                .uri("/api/internal/users/{userId}/follower-count", userId)
                .retrieve()
                .body(Long.class));
        if (count == null) {
            throw new IllegalStateException("Missing follower count");
        }
        return count;
    }

    public List<String> getFollowerIds(String userId) {
        return circuitBreaker.executeSupplier(() -> restClient.get()
                .uri("/api/internal/users/{userId}/follower-ids", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {}));
    }
}
