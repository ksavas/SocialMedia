package com.socialmedia.feed_service.service;

import com.socialmedia.feed_service.dto.PostSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int FEED_MAX_SIZE = 1000;

    private final RedisTemplate<String, String> redisTemplate;
    private final UserServiceClient userServiceClient;
    private final PostServiceClient postServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void handlePostCreated(String postId, String authorId, long epochMillis) {
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
            long score = post.createdAt().toInstant(ZoneOffset.UTC).toEpochMilli();
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
        long start = (long) page * size;
        long end = start + size - 1;
        Set<String> postIds = redisTemplate.opsForZSet().reverseRange("feed:" + userId, start, end);
        return postIds == null ? List.of() : new ArrayList<>(postIds);
    }

    private void addPostToFeed(String userId, String authorId, String postId, long epochMillis) {
        redisTemplate.opsForZSet().add("feed:" + userId, postId, epochMillis);
        redisTemplate.opsForSet().add("author_posts_in_feed:" + userId + ":" + authorId, postId);
        redisTemplate.opsForSet().add("post_feeds:" + postId, userId);
        redisTemplate.opsForZSet().removeRange("feed:" + userId, 0, -(FEED_MAX_SIZE + 1));
    }
}
