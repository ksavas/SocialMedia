package com.socialmedia.feed_service.service;

import com.socialmedia.feed_service.dto.PostSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int FEED_MAX_SIZE = 1000;

    private final RedisTemplate<String, String> redisTemplate;
    private final UserServiceClient userServiceClient;
    private final PostServiceClient postServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisScript<Long> addPostToFeedScript;

    @Value("${feed.celebrity-follower-threshold:10000}")
    private long celebrityFollowerThreshold = 10_000;

    public void handlePostCreated(String postId, String authorId, long epochMillis) {
        if (celebrityFollowerThreshold < 1) {
            throw new IllegalStateException("Celebrity follower threshold must be positive");
        }
        if (userServiceClient.getFollowerCount(authorId) >= celebrityFollowerThreshold) {
            postServiceClient.markForPull(postId);
            return;
        }
        addPostToFeed(authorId, authorId, postId, epochMillis);
        List<String> followerIds = userServiceClient.getFollowerIds(authorId);
        for (String followerId : followerIds) {
            addPostToFeed(followerId, authorId, postId, epochMillis);
            kafkaTemplate.send("feed.updated", followerId,
                    "{\"userId\":\"" + followerId + "\",\"postId\":\"" + postId + "\"}");
        }
    }

    public void handlePostDeleted(String postId, String authorId) {
        Set<String> userIds = redisTemplate.opsForSet().members("post_feeds:" + postId);
        if (userIds == null) return;
        for (String userId : userIds) {
            redisTemplate.opsForZSet().remove("feed:" + userId, postId);
            redisTemplate.opsForSet().remove("author_posts_in_feed:" + userId + ":" + authorId, postId);
        }
        redisTemplate.delete("post_feeds:" + postId);
    }

    public void handleUserFollowed(String followerId, String followingId) {
        List<PostSummary> posts = postServiceClient.getPostsByAuthor(followingId, 50);
        for (PostSummary post : posts) {
            long score = post.createdAt().toEpochMilli();
            addPostToFeed(followerId, followingId, post.id(), score);
        }
    }

    public void handleUserUnfollowed(String followerId, String followingId) {
        String key = "author_posts_in_feed:" + followerId + ":" + followingId;
        Set<String> postIds = redisTemplate.opsForSet().members(key);
        if (postIds != null) {
            for (String postId : postIds) {
                redisTemplate.opsForZSet().remove("feed:" + followerId, postId);
                redisTemplate.opsForSet().remove("post_feeds:" + postId, followerId);
            }
        }
        redisTemplate.delete(key);
    }

    public List<String> getFeedPostIds(String userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feed page or size");
        }
        long start = (long) page * size;
        if (start >= FEED_MAX_SIZE) {
            return List.of();
        }
        Map<String, Long> candidates = new HashMap<>();
        var pushed = redisTemplate.opsForZSet().reverseRangeWithScores("feed:" + userId, 0, FEED_MAX_SIZE - 1);
        if (pushed != null) {
            for (var post : pushed) {
                if (post.getValue() != null && post.getScore() != null) {
                    candidates.put(post.getValue(), post.getScore().longValue());
                }
            }
        }
        Set<String> authorIds = new LinkedHashSet<>(userServiceClient.getFollowingIds(userId));
        authorIds.add(userId);
        for (PostSummary post : postServiceClient.getPullPosts(new ArrayList<>(authorIds), FEED_MAX_SIZE)) {
            candidates.merge(post.id(), post.createdAt().toEpochMilli(), Math::max);
        }
        return candidates.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .limit(FEED_MAX_SIZE)
                .skip(start)
                .limit(size)
                .map(Map.Entry::getKey)
                .toList();
    }

    private void addPostToFeed(String userId, String authorId, String postId, long epochMillis) {
        redisTemplate.execute(addPostToFeedScript,
                List.of("feed:" + userId, "author_posts_in_feed:" + userId + ":" + authorId, "post_feeds:" + postId),
                postId, String.valueOf(epochMillis), userId, String.valueOf(FEED_MAX_SIZE));
    }
}
