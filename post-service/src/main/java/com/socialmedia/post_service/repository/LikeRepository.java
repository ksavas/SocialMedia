package com.socialmedia.post_service.repository;

import com.socialmedia.post_service.document.Like;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LikeRepository extends MongoRepository<Like, String> {
    boolean existsByPostIdAndUserId(String postId, String userId);
    Optional<Like> findByPostIdAndUserId(String postId, String userId);
    List<Like> findByUserIdAndPostIdIn(String userId, List<String> postIds);
}
