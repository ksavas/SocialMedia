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
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void accountBelowConfiguredThresholdStillPushesPosts() {
        ReflectionTestUtils.setField(feedService, "celebrityFollowerThreshold", 3L);
        when(userServiceClient.getFollowerCount("author1")).thenReturn(2L);
        when(userServiceClient.getFollowerIds("author1")).thenReturn(List.of("f1", "f2"));

        feedService.handlePostCreated("normal", "author1", 1000L);

        verify(zSetOps).add("feed:f1", "normal", 1000.0);
        verify(zSetOps).add("feed:f2", "normal", 1000.0);
        verify(postServiceClient, never()).markForPull(anyString());
    }

    @Test
    void loweringFollowerCountDoesNotRemoveEarlierPullPostsFromReadPath() {
        ReflectionTestUtils.setField(feedService, "celebrityFollowerThreshold", 3L);
        when(userServiceClient.getFollowerCount("author")).thenReturn(3L, 2L);
        when(userServiceClient.getFollowerIds("author")).thenReturn(List.of("reader"));
        feedService.handlePostCreated("old-pull", "author", 1000L);
        feedService.handlePostCreated("new-push", "author", 2000L);
        when(userServiceClient.getFollowingIds("reader")).thenReturn(List.of("author"));
        when(zSetOps.reverseRangeWithScores("feed:reader", 0, 999)).thenReturn(Set.of(
                new DefaultTypedTuple<>("new-push", 2000.0)));
        when(postServiceClient.getPullPosts(List.of("author", "reader"), 1000)).thenReturn(List.of(
                new PostSummary("old-pull", "author", Instant.ofEpochMilli(1000))));

        assertThat(feedService.getFeedPostIds("reader", 0, 20)).containsExactly("new-push", "old-pull");
        verify(postServiceClient).markForPull("old-pull");
        verify(postServiceClient, never()).markForPull("new-push");
    }

    @Test
    void pageOutsideFeedWindowReturnsEmptyWithoutCallingDownstreamServices() {
        assertThat(feedService.getFeedPostIds("reader", 50, 20)).isEmpty();
        verifyNoInteractions(userServiceClient, postServiceClient);
    }

    @Test
    void invalidPaginationIsRejected() {
        assertThatThrownBy(() -> feedService.getFeedPostIds("reader", -1, 20))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> feedService.getFeedPostIds("reader", 0, 0))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void hybridFeedMergesBeforePaginationAndDeduplicatesPushAndPull() {
        when(userServiceClient.getFollowingIds("reader")).thenReturn(List.of("celebrity"));
        when(zSetOps.reverseRangeWithScores("feed:reader", 0, 999)).thenReturn(new LinkedHashSet<>(List.of(
                new DefaultTypedTuple<>("p1", 1000.0),
                new DefaultTypedTuple<>("p3", 3000.0))));
        when(postServiceClient.getPullPosts(List.of("celebrity", "reader"), 1000)).thenReturn(List.of(
                new PostSummary("p4", "celebrity", Instant.ofEpochMilli(4000)),
                new PostSummary("p3", "celebrity", Instant.ofEpochMilli(3000)),
                new PostSummary("p2", "celebrity", Instant.ofEpochMilli(2000))));

        assertThat(feedService.getFeedPostIds("reader", 0, 2)).containsExactly("p4", "p3");
        assertThat(feedService.getFeedPostIds("reader", 1, 2)).containsExactly("p2", "p1");
    }

    @Test
    void hybridFeedIncludesOwnPullPostsWithoutFollowingAnyone() {
        when(userServiceClient.getFollowingIds("reader")).thenReturn(List.of());
        when(postServiceClient.getPullPosts(List.of("reader"), 1000)).thenReturn(List.of(
                new PostSummary("mine", "reader", Instant.ofEpochMilli(4000))));

        assertThat(feedService.getFeedPostIds("reader", 0, 20)).containsExactly("mine");
    }

    @Test
    void hybridFeedOrdersEqualScoresByDescendingPostId() {
        when(userServiceClient.getFollowingIds("reader")).thenReturn(List.of("author"));
        when(zSetOps.reverseRangeWithScores("feed:reader", 0, 999)).thenReturn(Set.of(
                new DefaultTypedTuple<>("a", 1000.0)));
        when(postServiceClient.getPullPosts(List.of("author", "reader"), 1000)).thenReturn(List.of(
                new PostSummary("b", "author", Instant.ofEpochMilli(1000))));

        assertThat(feedService.getFeedPostIds("reader", 0, 20)).containsExactly("b", "a");
    }

    @Test
    void failedPullRegistrationIsRetriedInsteadOfFallingBackToHugeFanout() {
        when(userServiceClient.getFollowerCount("author1")).thenReturn(10_000L);
        doThrow(new IllegalStateException("post-service unavailable")).when(postServiceClient).markForPull("p1");

        assertThatThrownBy(() -> feedService.handlePostCreated("p1", "author1", 1000))
                .isInstanceOf(IllegalStateException.class);
        verify(userServiceClient, never()).getFollowerIds(anyString());
    }

    @Test
    void celebrityPostUsesPullWithoutLoadingFollowersOrSendingPerFollowerEvents() {
        when(userServiceClient.getFollowerCount("author1")).thenReturn(10_000L);

        feedService.handlePostCreated("p1", "author1", 1000L);

        verify(postServiceClient).markForPull("p1");
        verify(userServiceClient, never()).getFollowerIds(anyString());
        verifyNoInteractions(zSetOps, setOps, kafkaTemplate);
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
    void handlePostCreated_addsToAuthorFeedEvenWhenNoFollowers() {
        when(userServiceClient.getFollowerIds("author1")).thenReturn(List.of());

        feedService.handlePostCreated("p1", "author1", 1000L);

        verify(zSetOps).add(eq("feed:author1"), eq("p1"), anyDouble());
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
    void handleUserFollowed_scoresBackfilledPostsByEpochCreationTime() {
        Instant createdAt = Instant.parse("2026-06-01T10:15:30Z");
        when(postServiceClient.getPostsByAuthor("author2", 50))
                .thenReturn(List.of(new PostSummary("p1", "author2", createdAt)));

        feedService.handleUserFollowed("follower1", "author2");

        verify(zSetOps).add("feed:follower1", "p1", (double) createdAt.toEpochMilli());
    }

    @Test
    void handleUserFollowed_backfillsAuthorPostsIntoFollowerFeed() {
        PostSummary post1 = new PostSummary("p1", "author2", Instant.parse("2025-01-01T12:00:00Z"));
        PostSummary post2 = new PostSummary("p2", "author2", Instant.parse("2025-01-02T12:00:00Z"));
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
        when(zSetOps.reverseRangeWithScores("feed:user1", 0L, 999L)).thenReturn(new LinkedHashSet<>(List.of(
                new DefaultTypedTuple<>("p3", 3000.0),
                new DefaultTypedTuple<>("p2", 2000.0),
                new DefaultTypedTuple<>("p1", 1000.0))));

        List<String> result = feedService.getFeedPostIds("user1", 0, 20);

        assertThat(result).containsExactly("p3", "p2", "p1");
    }

    @Test
    void getFeedPostIds_returnsEmptyListWhenFeedIsEmpty() {
        when(zSetOps.reverseRangeWithScores("feed:user1", 0L, 999L)).thenReturn(null);

        List<String> result = feedService.getFeedPostIds("user1", 0, 20);

        assertThat(result).isEmpty();
    }
}
