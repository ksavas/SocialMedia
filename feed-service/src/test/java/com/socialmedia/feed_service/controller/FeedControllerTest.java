package com.socialmedia.feed_service.controller;

import com.socialmedia.feed_service.security.UserPrincipal;
import com.socialmedia.feed_service.service.FeedService;
import com.socialmedia.feed_service.service.PostServiceClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedControllerTest {

    private final FeedService feedService = mock(FeedService.class);
    private final PostServiceClient postServiceClient = mock(PostServiceClient.class);
    private final FeedController controller = new FeedController(feedService, postServiceClient);
    private final UserPrincipal reader = new UserPrincipal("reader", "reader");

    @Test
    void returns503_whenPostServiceIsUnreachable() {
        when(feedService.getFeedPostIds("reader", 0, 20)).thenReturn(List.of("p1"));
        when(postServiceClient.getPostsByIds(anyList(), anyString())).thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> controller.getFeed(reader, 0, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void returns503_whenPostServiceCircuitIsOpen() {
        when(feedService.getFeedPostIds("reader", 0, 20)).thenReturn(List.of("p1"));
        when(postServiceClient.getPostsByIds(anyList(), anyString())).thenThrow(
                CallNotPermittedException.createCallNotPermittedException(CircuitBreaker.ofDefaults("post-service")));

        assertThatThrownBy(() -> controller.getFeed(reader, 0, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
