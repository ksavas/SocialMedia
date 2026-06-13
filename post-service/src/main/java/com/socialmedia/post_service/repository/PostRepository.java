package com.socialmedia.post_service.repository;

import com.socialmedia.post_service.document.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByAuthorIdOrderByCreatedAtDesc(String authorId, Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findByIdIn(List<String> ids);
}
