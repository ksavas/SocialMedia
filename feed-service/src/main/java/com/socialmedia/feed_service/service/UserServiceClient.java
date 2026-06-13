package com.socialmedia.feed_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${services.user-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<String> getFollowerIds(String userId) {
        return restClient.get()
                .uri("/api/internal/users/{userId}/follower-ids", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
