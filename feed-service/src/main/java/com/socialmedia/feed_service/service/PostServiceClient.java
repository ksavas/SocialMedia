package com.socialmedia.feed_service.service;

import com.socialmedia.feed_service.dto.BatchPostRequest;
import com.socialmedia.feed_service.dto.PostDto;
import com.socialmedia.feed_service.dto.PostSummary;
import com.socialmedia.feed_service.dto.PullFeedRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class PostServiceClient {

    private final RestClient restClient;

    public PostServiceClient(@Value("${services.post-service.url}") String baseUrl) {
        this.restClient = RestClientFactory.create(baseUrl);
    }

    public List<PostSummary> getPullPosts(List<String> authorIds, int limit) {
        List<PostSummary> posts = restClient.post()
                .uri("/api/internal/posts/pull-feed")
                .body(new PullFeedRequest(authorIds, limit))
                .retrieve()
                .body(new ParameterizedTypeReference<List<PostSummary>>() {});
        if (posts == null) {
            throw new IllegalStateException("Missing pull feed response");
        }
        return posts;
    }

    public void markForPull(String postId) {
        restClient.put()
                .uri("/api/internal/posts/{postId}/fanout-on-read", postId)
                .retrieve()
                .toBodilessEntity();
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
