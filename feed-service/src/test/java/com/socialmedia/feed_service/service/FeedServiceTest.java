package com.socialmedia.feed_service.service;

import com.socialmedia.feed_service.dto.PostSummary;
import com.socialmedia.feed_service.dto.PostDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOps;
    @Mock
    private SetOperations<String, String> setOps;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private PostServiceClient postServiceClient;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private FeedService feedService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
    }

    @Test
    void handlePostCreated_addsPostToEachFollowerFeed() {
        when(userServiceClient.getFollowerIds("author1")).thenReturn(List.of("f1", "f2"));

        feedService.handlePostCreated("p1", "author1", 1000L);

        verify(zSetOps).add("feed:f1", "p1", 1000.0);
        verify(zSetOps).add("feed:f2", "p1", 1000.0);
        verify(setOps).add("author_posts_in_feed:f1:author1", "p1");
        verify(setOps).add("author_posts_in_feed:f2:author1", "p1");
        verify(setOps).add("post_feeds:p1", "f1");
        verify(setOps).add("post_feeds:p1", "f2");
    }

    @Test
    void handlePostCreated_publishesFeedUpdatedForEachFollower() {
        when(userServiceClient.getFollowerIds("author1")).thenReturn(List.of("f1", "f2"));

        feedService.handlePostCreated("p1", "author1", 1000L);

        verify(kafkaTemplate).send(eq("feed.updated"), eq("f1"), contains("p1"));
        verify(kafkaTemplate).send(eq("feed.updated"), eq("f2"), contains("p1"));
    }

    @Test
    void handlePostCreated_doesNothingWhenNoFollowers() {
        when(userServiceClient.getFollowerIds("author1")).thenReturn(List.of());

        feedService.handlePostCreated("p1", "author1", 1000L);

        verify(zSetOps, never()).add(any(), any(), anyDouble());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void handlePostDeleted_removesPostFromAllAffectedFeeds() {
        when(setOps.members("post_feeds:p1")).thenReturn(Set.of("u1", "u2"));

        feedService.handlePostDeleted("p1", "author1");

        verify(zSetOps).remove("feed:u1", "p1");
        verify(zSetOps).remove("feed:u2", "p1");
        verify(setOps).remove("author_posts_in_feed:u1:author1", "p1");
        verify(setOps).remove("author_posts_in_feed:u2:author1", "p1");
        verify(redisTemplate).delete("post_feeds:p1");
    }

    @Test
    void handlePostDeleted_doesNothingWhenNoFeedsContainPost() {
        when(setOps.members("post_feeds:p1")).thenReturn(null);

        feedService.handlePostDeleted("p1", "author1");

        verify(zSetOps, never()).remove(any(), any());
    }

    @Test
    void handleUserFollowed_backfillsAuthorPostsIntoFollowerFeed() {
        PostSummary post1 = new PostSummary("p1", "author2", LocalDateTime.of(2025, 1, 1, 12, 0));
        PostSummary post2 = new PostSummary("p2", "author2", LocalDateTime.of(2025, 1, 2, 12, 0));
        when(postServiceClient.getPostsByAuthor("author2", 50)).thenReturn(List.of(post1, post2));

        feedService.handleUserFollowed("follower1", "author2");

        verify(zSetOps).add(eq("feed:follower1"), eq("p1"), anyDouble());
        verify(zSetOps).add(eq("feed:follower1"), eq("p2"), anyDouble());
        verify(setOps).add("author_posts_in_feed:follower1:author2", "p1");
        verify(setOps).add("author_posts_in_feed:follower1:author2", "p2");
        verify(setOps).add("post_feeds:p1", "follower1");
        verify(setOps).add("post_feeds:p2", "follower1");
    }

    @Test
    void handleUserUnfollowed_removesAuthorPostsFromFollowerFeed() {
        when(setOps.members("author_posts_in_feed:follower1:author2")).thenReturn(Set.of("p1", "p2"));

        feedService.handleUserUnfollowed("follower1", "author2");

        verify(zSetOps).remove("feed:follower1", "p1");
        verify(zSetOps).remove("feed:follower1", "p2");
        verify(setOps).remove("post_feeds:p1", "follower1");
        verify(setOps).remove("post_feeds:p2", "follower1");
        verify(redisTemplate).delete("author_posts_in_feed:follower1:author2");
    }

    @Test
    void getFeedPostIds_returnsPaginatedPostIdsFromRedis() {
        LinkedHashSet<String> stored = new LinkedHashSet<>(List.of("p3", "p2", "p1"));
        when(zSetOps.reverseRange("feed:user1", 0L, 19L)).thenReturn(stored);

        List<String> result = feedService.getFeedPostIds("user1", 0, 20);

        assertThat(result).containsExactly("p3", "p2", "p1");
    }

    @Test
    void getFeedPostIds_returnsEmptyListWhenFeedIsEmpty() {
        when(zSetOps.reverseRange("feed:user1", 0L, 19L)).thenReturn(null);

        List<String> result = feedService.getFeedPostIds("user1", 0, 20);

        assertThat(result).isEmpty();
    }
}
