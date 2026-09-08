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
import com.socialmedia.post_service.dto.PostSummary;
import com.socialmedia.post_service.repository.CommentRepository;
import com.socialmedia.post_service.repository.LikeRepository;
import com.socialmedia.post_service.repository.OutboxEventRepository;
import com.socialmedia.post_service.repository.PostRepository;
import com.socialmedia.post_service.transaction.TransactionRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionRunner transactionRunner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PostResponse createPost(String authorId, String authorUsername, CreatePostRequest request) {
        Post post = transactionRunner.inTransaction(() -> {
            Post saved = postRepository.save(Post.builder()
                    .authorId(authorId)
                    .authorUsername(authorUsername)
                    .content(request.content())
                    .createdAt(Instant.now())
                    .build());
            saveOutboxEvent("post.created", saved.getId(), Map.of(
                    "postId", saved.getId(),
                    "authorId", authorId,
                    "createdAt", saved.getCreatedAt().toString()
            ));
            return saved;
        });
        return toResponse(post, false);
    }

    public PostResponse getPost(String requesterId, String postId) {
        Post post = findPost(postId);
        return toResponse(post, likeRepository.existsByPostIdAndUserId(postId, requesterId));
    }

    public void deletePost(String requesterId, String postId) {
        transactionRunner.runInTransaction(() -> {
            Post post = findPost(postId);
            if (!post.getAuthorId().equals(requesterId)) {
                throw new IllegalStateException("Not authorized");
            }
            postRepository.delete(post);
            saveOutboxEvent("post.deleted", postId, Map.of("postId", postId, "authorId", post.getAuthorId()));
        });
    }

    public void likePost(String userId, String postId) {
        transactionRunner.runInTransaction(() -> {
            Post post = findPost(postId);
            if (likeRepository.existsByPostIdAndUserId(postId, userId)) {
                throw new IllegalStateException("Already liked");
            }
            try {
                likeRepository.save(Like.builder().postId(postId).userId(userId).createdAt(Instant.now()).build());
            } catch (DuplicateKeyException e) {
                throw new IllegalStateException("Already liked");
            }
            saveOutboxEvent("post.liked", postId, Map.of(
                    "postId", postId,
                    "userId", userId,
                    "authorId", post.getAuthorId()
            ));
        });
    }

    public void unlikePost(String userId, String postId) {
        transactionRunner.runInTransaction(() -> {
            Post post = findPost(postId);
            Like like = likeRepository.findByPostIdAndUserId(postId, userId)
                    .orElseThrow(() -> new IllegalStateException("Not liked"));
            likeRepository.delete(like);
            saveOutboxEvent("post.unliked", postId, Map.of(
                    "postId", postId,
                    "userId", userId,
                    "authorId", post.getAuthorId()
            ));
        });
    }

    public CommentResponse addComment(String authorId, String authorUsername, String postId, CreateCommentRequest request) {
        Comment comment = transactionRunner.inTransaction(() -> {
            Post post = findPost(postId);
            Comment saved = commentRepository.save(Comment.builder()
                    .postId(postId)
                    .authorId(authorId)
                    .authorUsername(authorUsername)
                    .content(request.content())
                    .createdAt(Instant.now())
                    .build());
            saveOutboxEvent("comment.created", postId, Map.of(
                    "commentId", saved.getId(),
                    "postId", postId,
                    "authorId", authorId,
                    "postAuthorId", post.getAuthorId()
            ));
            return saved;
        });
        return toCommentResponse(comment);
    }

    public List<PostResponse> getAllPosts(String requesterId) {
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        Set<String> likedPostIds = findLikedPostIds(requesterId, posts.stream().map(Post::getId).toList());
        return posts.stream()
                .map(p -> toResponse(p, likedPostIds.contains(p.getId())))
                .toList();
    }

    public List<PostSummary> getPostsByAuthor(String authorId, int limit) {
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(0, limit))
                .stream()
                .map(p -> new PostSummary(p.getId(), p.getAuthorId(), p.getCreatedAt()))
                .toList();
    }

    public void markForPull(String postId) {
        postRepository.markForPull(postId);
    }

    public List<PostSummary> getPullFeedPosts(List<String> authorIds, int limit) {
        if (authorIds == null || limit < 1 || limit > 1000 || authorIds.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid pull feed request");
        }
        if (authorIds.isEmpty()) {
            return List.of();
        }
        return postRepository.findByFanoutOnReadTrueAndAuthorIdInOrderByCreatedAtDescIdDesc(
                        authorIds.stream().distinct().toList(), PageRequest.of(0, limit)).stream()
                .map(post -> new PostSummary(post.getId(), post.getAuthorId(), post.getCreatedAt()))
                .toList();
    }

    public List<PostResponse> getPostsByIds(List<String> postIds, String requesterId) {
        Map<String, Post> postsById = postRepository.findByIdIn(postIds).stream()
                .collect(Collectors.toMap(Post::getId, Function.identity()));
        Set<String> likedPostIds = findLikedPostIds(requesterId, postIds);
        return postIds.stream()
                .map(postsById::get)
                .filter(Objects::nonNull)
                .map(p -> toResponse(p, likedPostIds.contains(p.getId())))
                .toList();
    }

    public List<CommentResponse> getComments(String postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private Set<String> findLikedPostIds(String userId, List<String> postIds) {
        if (postIds.isEmpty()) {
            return Set.of();
        }
        return likeRepository.findByUserIdAndPostIdIn(userId, postIds).stream()
                .map(Like::getPostId)
                .collect(Collectors.toSet());
    }

    private Post findPost(String postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    private PostResponse toResponse(Post post, boolean likedByMe) {
        return new PostResponse(
                post.getId(),
                post.getAuthorId(),
                post.getAuthorUsername(),
                post.getContent(),
                post.getLikeCount(),
                post.getCommentCount(),
                likedByMe,
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
                    .createdAt(Instant.now())
                    .build());
        } catch (JacksonIOException e) {
            throw new RuntimeException(e);
        }
    }
}
