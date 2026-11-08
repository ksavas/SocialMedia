package com.socialmedia.feed_service.controller;

import com.socialmedia.feed_service.dto.PostDto;
import com.socialmedia.feed_service.security.UserPrincipal;
import com.socialmedia.feed_service.service.FeedService;
import com.socialmedia.feed_service.service.PostServiceClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final PostServiceClient postServiceClient;

    @GetMapping
    public List<PostDto> getFeed(@AuthenticationPrincipal UserPrincipal principal,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        List<String> postIds = loadFeedPostIds(principal.userId(), page, size);
        if (postIds.isEmpty()) {
            return List.of();
        }
        try {
            return postServiceClient.getPostsByIds(postIds, principal.userId());
        } catch (ResourceAccessException | HttpServerErrorException | CallNotPermittedException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Post service unavailable", e);
        }
    }

    private List<String> loadFeedPostIds(String userId, int page, int size) {
        try {
            return feedService.getFeedPostIds(userId, page, size);
        } catch (DataAccessResourceFailureException | QueryTimeoutException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Feed store unavailable", e);
        }
    }
}
