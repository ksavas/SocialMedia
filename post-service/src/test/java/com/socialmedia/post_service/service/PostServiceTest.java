package com.socialmedia.post_service.service;

import com.socialmedia.post_service.document.Comment;
import com.socialmedia.post_service.document.Like;
import com.socialmedia.post_service.document.Post;
import com.socialmedia.post_service.dto.CommentResponse;
import com.socialmedia.post_service.dto.CreateCommentRequest;
import com.socialmedia.post_service.dto.CreatePostRequest;
import com.socialmedia.post_service.dto.PostResponse;
import com.socialmedia.post_service.repository.CommentRepository;
import com.socialmedia.post_service.repository.LikeRepository;
import com.socialmedia.post_service.repository.OutboxEventRepository;
import com.socialmedia.post_service.repository.PostRepository;
import com.socialmedia.post_service.transaction.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock PostRepository postRepository;
    @Mock LikeRepository likeRepository;
    @Mock CommentRepository commentRepository;
    @Mock OutboxEventRepository outboxEventRepository;

    PostService postService;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, likeRepository, commentRepository, outboxEventRepository, directRunner());
    }

    private TransactionRunner directRunner() {
        return new TransactionRunner() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return work.get();
            }
        };
    }

    @Test
    void createPost_shouldSavePostAndOutboxEvent() {
        Post saved = Post.builder().id("p1").authorId("u1").authorUsername("alice")
                .content("Hello").createdAt(Instant.now()).build();
        when(postRepository.save(any())).thenReturn(saved);

        PostResponse response = postService.createPost("u1", "alice", new CreatePostRequest("Hello"));

        assertThat(response.id()).isEqualTo("p1");
        assertThat(response.content()).isEqualTo("Hello");
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("post.created")));
    }

    @Test
    void createPost_shouldIncludeCreatedAtInOutboxPayload() {
        Instant createdAt = Instant.parse("2026-06-01T10:15:30Z");
        Post saved = Post.builder().id("p1").authorId("u1").authorUsername("alice")
                .content("Hello").createdAt(createdAt).build();
        when(postRepository.save(any())).thenReturn(saved);

        postService.createPost("u1", "alice", new CreatePostRequest("Hello"));

        verify(outboxEventRepository).save(argThat(e ->
                e.getPayload().contains("\"createdAt\":\"2026-06-01T10:15:30Z\"")));
    }

    @Test
    void deletePost_shouldDeletePostAndOutboxEvent_whenAuthor() {
        Post post = Post.builder().id("p1").authorId("u1").content("Hello").createdAt(Instant.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));

        postService.deletePost("u1", "p1");

        verify(postRepository).delete(post);
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("post.deleted")));
    }

    @Test
    void deletePost_shouldThrow_whenNotAuthor() {
        Post post = Post.builder().id("p1").authorId("u1").content("Hello").createdAt(Instant.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost("u2", "p1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not authorized");
    }

    @Test
    void likePost_shouldSaveLikeAndOutboxEventWithoutTouchingPostDocument() {
        Post post = Post.builder().id("p1").authorId("u2").likeCount(0).createdAt(Instant.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.existsByPostIdAndUserId("p1", "u1")).thenReturn(false);

        postService.likePost("u1", "p1");

        verify(likeRepository).save(any(Like.class));
        verify(postRepository, never()).incrementLikeCount(anyString());
        verify(postRepository, never()).save(any());
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("post.liked")));
    }

    @Test
    void likePost_shouldThrow_whenAlreadyLiked() {
        Post post = Post.builder().id("p1").authorId("u2").createdAt(Instant.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.existsByPostIdAndUserId("p1", "u1")).thenReturn(true);

        assertThatThrownBy(() -> postService.likePost("u1", "p1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already liked");
    }

    @Test
    void likePost_shouldThrowAlreadyLiked_whenUniqueIndexRejectsConcurrentDuplicate() {
        Post post = Post.builder().id("p1").authorId("u2").createdAt(Instant.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.existsByPostIdAndUserId("p1", "u1")).thenReturn(false);
        when(likeRepository.save(any(Like.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> postService.likePost("u1", "p1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already liked");
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void unlikePost_shouldDeleteLikeAndSaveUnlikedOutboxEvent() {
        Post post = Post.builder().id("p1").authorId("u2").likeCount(1).createdAt(Instant.now()).build();
        Like like = Like.builder().id("l1").postId("p1").userId("u1").build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.findByPostIdAndUserId("p1", "u1")).thenReturn(Optional.of(like));

        postService.unlikePost("u1", "p1");

        verify(likeRepository).delete(like);
        verify(postRepository, never()).decrementLikeCount(anyString());
        verify(outboxEventRepository).save(argThat(e ->
                e.getEventType().equals("post.unliked") && e.getAggregateId().equals("p1")));
    }

    @Test
    void addComment_shouldSaveCommentAndOutboxEventWithoutTouchingPostDocument() {
        Post post = Post.builder().id("p1").authorId("u2").commentCount(0).createdAt(Instant.now()).build();
        Comment saved = Comment.builder().id("c1").postId("p1").authorId("u1")
                .authorUsername("alice").content("Nice!").createdAt(Instant.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(commentRepository.save(any())).thenReturn(saved);

        CommentResponse response = postService.addComment("u1", "alice", "p1", new CreateCommentRequest("Nice!"));

        assertThat(response.content()).isEqualTo("Nice!");
        verify(postRepository, never()).incrementCommentCount(anyString());
        verify(postRepository, never()).save(any());
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("comment.created")));
    }

    @Test
    void getComments_shouldReturnCommentsForPost() {
        Comment c = Comment.builder().id("c1").postId("p1").authorId("u1")
                .content("Hey").createdAt(Instant.now()).build();
        when(commentRepository.findByPostIdOrderByCreatedAtAsc("p1")).thenReturn(List.of(c));

        List<CommentResponse> result = postService.getComments("p1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().content()).isEqualTo("Hey");
    }

    @Test
    void getPostsByIds_shouldPreserveRequestedOrder() {
        Post p1 = Post.builder().id("p1").authorId("a").content("first").createdAt(Instant.now()).build();
        Post p2 = Post.builder().id("p2").authorId("a").content("second").createdAt(Instant.now()).build();
        Post p3 = Post.builder().id("p3").authorId("a").content("third").createdAt(Instant.now()).build();
        when(postRepository.findByIdIn(List.of("p3", "p1", "p2"))).thenReturn(List.of(p1, p2, p3));

        List<PostResponse> result = postService.getPostsByIds(List.of("p3", "p1", "p2"), "u1");

        assertThat(result).extracting(PostResponse::id).containsExactly("p3", "p1", "p2");
    }

    @Test
    void getPostsByIds_shouldSkipIdsOfDeletedPosts() {
        Post p1 = Post.builder().id("p1").authorId("a").content("first").createdAt(Instant.now()).build();
        when(postRepository.findByIdIn(List.of("p1", "gone"))).thenReturn(List.of(p1));

        List<PostResponse> result = postService.getPostsByIds(List.of("p1", "gone"), "u1");

        assertThat(result).extracting(PostResponse::id).containsExactly("p1");
    }

    @Test
    void getPostsByIds_shouldResolveLikedByMeWithSingleQuery() {
        Post p1 = Post.builder().id("p1").authorId("a").content("first").createdAt(Instant.now()).build();
        Post p2 = Post.builder().id("p2").authorId("a").content("second").createdAt(Instant.now()).build();
        when(postRepository.findByIdIn(List.of("p1", "p2"))).thenReturn(List.of(p1, p2));
        when(likeRepository.findByUserIdAndPostIdIn("u1", List.of("p1", "p2")))
                .thenReturn(List.of(Like.builder().postId("p2").userId("u1").build()));

        List<PostResponse> result = postService.getPostsByIds(List.of("p1", "p2"), "u1");

        assertThat(result).extracting(PostResponse::likedByMe).containsExactly(false, true);
        verify(likeRepository, never()).existsByPostIdAndUserId(anyString(), anyString());
    }

    @Test
    void getAllPosts_shouldResolveLikedByMeWithSingleQuery() {
        Post p1 = Post.builder().id("p1").authorId("a").content("first").createdAt(Instant.now()).build();
        Post p2 = Post.builder().id("p2").authorId("a").content("second").createdAt(Instant.now()).build();
        when(postRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(p1, p2));
        when(likeRepository.findByUserIdAndPostIdIn("u1", List.of("p1", "p2")))
                .thenReturn(List.of(Like.builder().postId("p1").userId("u1").build()));

        List<PostResponse> result = postService.getAllPosts("u1");

        assertThat(result).extracting(PostResponse::likedByMe).containsExactly(true, false);
        verify(likeRepository, never()).existsByPostIdAndUserId(anyString(), anyString());
    }
}
