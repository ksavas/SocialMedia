package com.socialmedia.post_service.service;

import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;
import com.socialmedia.post_service.document.Comment;
import com.socialmedia.post_service.document.Like;
import com.socialmedia.post_service.document.OutboxEvent;
import com.socialmedia.post_service.document.Post;
import com.socialmedia.post_service.dto.CommentResponse;
import com.socialmedia.post_service.dto.CreateCommentRequest;
import com.socialmedia.post_service.dto.CreatePostRequest;
import com.socialmedia.post_service.dto.PostResponse;
import com.socialmedia.post_service.repository.CommentRepository;
import com.socialmedia.post_service.repository.LikeRepository;
import com.socialmedia.post_service.repository.OutboxEventRepository;
import com.socialmedia.post_service.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostResponse createPost(String authorId, String authorUsername, CreatePostRequest request) {
        Post post = postRepository.save(Post.builder()
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .build());

        saveOutboxEvent("post.created", post.getId(), Map.of(
                "postId", post.getId(),
                "authorId", authorId
        ));

        return toResponse(post, authorId);
    }

    public PostResponse getPost(String requesterId, String postId) {
        Post post = findPost(postId);
        return toResponse(post, requesterId);
    }

    public void deletePost(String requesterId, String postId) {
        Post post = findPost(postId);
        if (!post.getAuthorId().equals(requesterId)) {
            throw new IllegalStateException("Not authorized");
        }
        postRepository.delete(post);
        saveOutboxEvent("post.deleted", postId, Map.of("postId", postId));
    }

    public void likePost(String userId, String postId) {
        Post post = findPost(postId);
        if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new IllegalStateException("Already liked");
        }
        likeRepository.save(Like.builder().postId(postId).userId(userId).createdAt(LocalDateTime.now()).build());
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        saveOutboxEvent("post.liked", postId, Map.of(
                "postId", postId,
                "userId", userId,
                "authorId", post.getAuthorId()
        ));
    }

    public void unlikePost(String userId, String postId) {
        Post post = findPost(postId);
        Like like = likeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new IllegalStateException("Not liked"));
        likeRepository.delete(like);
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postRepository.save(post);
    }

    public CommentResponse addComment(String authorId, String authorUsername, String postId, CreateCommentRequest request) {
        Post post = findPost(postId);
        Comment comment = commentRepository.save(Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .authorUsername(authorUsername)
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .build());

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);

        saveOutboxEvent("comment.created", comment.getId(), Map.of(
                "commentId", comment.getId(),
                "postId", postId,
                "authorId", authorId,
                "postAuthorId", post.getAuthorId()
        ));

        return toCommentResponse(comment);
    }

    public List<PostResponse> getAllPosts(String requesterId) {
        return postRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(p -> toResponse(p, requesterId))
                .toList();
    }

    public List<CommentResponse> getComments(String postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private Post findPost(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    private PostResponse toResponse(Post post, String requesterId) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getContent(),
                post.getLikeCount(),
                post.getCommentCount(),
                likeRepository.existsByPostIdAndUserId(post.getId(), requesterId),
                post.getCreatedAt()
        );
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getAuthorId(),
                comment.getAuthorUsername(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }

    private void saveOutboxEvent(String eventType, String aggregateId, Map<String, String> payload) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .aggregateType("Post")
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(objectMapper.writeValueAsString(payload))
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (JacksonIOException e) {
            throw new RuntimeException(e);
        }
    }
}
