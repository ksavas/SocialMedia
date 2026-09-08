package com.socialmedia.post_service.repository;

import com.socialmedia.post_service.document.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);
    List<Post> findAllByOrderByCreatedAtDesc();
    List<Post> findByIdIn(List<String> ids);
    List<Post> findByFanoutOnReadTrueAndAuthorIdInOrderByCreatedAtDescIdDesc(List<String> authorIds, Pageable pageable);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'fanoutOnRead': true } }")
    void markForPull(String postId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'likeCount': 1 } }")
    void incrementLikeCount(String postId);

    @Query("{ '_id': ?0, 'likeCount': { '$gt': 0 } }")
    @Update("{ '$inc': { 'likeCount': -1 } }")
    void decrementLikeCount(String postId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'commentCount': 1 } }")
    void incrementCommentCount(String postId);
}
