package com.socialmedia.feed_service.service;

import com.socialmedia.feed_service.dto.BatchPostRequest;
import com.socialmedia.feed_service.dto.PostDto;
import com.socialmedia.feed_service.dto.PostSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class PostServiceClient {

    private final RestClient restClient;

    public PostServiceClient(@Value("${services.post-service.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<PostSummary> getPostsByAuthor(String authorId, int limit) {
        return restClient.get()
                .uri("/api/internal/posts/by-author/{authorId}?limit={limit}", authorId, limit)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<PostDto> getPostsByIds(List<String> postIds, String requesterId) {
        return restClient.post()
                .uri("/api/internal/posts/batch")
                .body(new BatchPostRequest(postIds, requesterId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
