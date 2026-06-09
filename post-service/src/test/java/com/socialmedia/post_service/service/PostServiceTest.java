package com.socialmedia.post_service.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        postService = new PostService(postRepository, likeRepository, commentRepository, outboxEventRepository);
    }

    @Test
    void createPost_shouldSavePostAndOutboxEvent() {
        Post saved = Post.builder().id("p1").authorId("u1").authorUsername("alice")
                .content("Hello").createdAt(LocalDateTime.now()).build();
        when(postRepository.save(any())).thenReturn(saved);

        PostResponse response = postService.createPost("u1", "alice", new CreatePostRequest("Hello"));

        assertThat(response.id()).isEqualTo("p1");
        assertThat(response.content()).isEqualTo("Hello");
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("post.created")));
    }

    @Test
    void deletePost_shouldDeletePostAndOutboxEvent_whenAuthor() {
        Post post = Post.builder().id("p1").authorId("u1").content("Hello").createdAt(LocalDateTime.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));

        postService.deletePost("u1", "p1");

        verify(postRepository).delete(post);
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("post.deleted")));
    }

    @Test
    void deletePost_shouldThrow_whenNotAuthor() {
        Post post = Post.builder().id("p1").authorId("u1").content("Hello").createdAt(LocalDateTime.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost("u2", "p1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not authorized");
    }

    @Test
    void likePost_shouldIncrementLikeCountAndSaveOutboxEvent() {
        Post post = Post.builder().id("p1").authorId("u2").likeCount(0).createdAt(LocalDateTime.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.existsByPostIdAndUserId("p1", "u1")).thenReturn(false);
        when(postRepository.save(any())).thenReturn(post);

        postService.likePost("u1", "p1");

        assertThat(post.getLikeCount()).isEqualTo(1);
        verify(likeRepository).save(any(Like.class));
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("post.liked")));
    }

    @Test
    void likePost_shouldThrow_whenAlreadyLiked() {
        Post post = Post.builder().id("p1").authorId("u2").createdAt(LocalDateTime.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.existsByPostIdAndUserId("p1", "u1")).thenReturn(true);

        assertThatThrownBy(() -> postService.likePost("u1", "p1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Already liked");
    }

    @Test
    void unlikePost_shouldDecrementLikeCount() {
        Post post = Post.builder().id("p1").authorId("u2").likeCount(1).createdAt(LocalDateTime.now()).build();
        Like like = Like.builder().id("l1").postId("p1").userId("u1").build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(likeRepository.findByPostIdAndUserId("p1", "u1")).thenReturn(Optional.of(like));
        when(postRepository.save(any())).thenReturn(post);

        postService.unlikePost("u1", "p1");

        assertThat(post.getLikeCount()).isEqualTo(0);
        verify(likeRepository).delete(like);
    }

    @Test
    void addComment_shouldSaveCommentAndOutboxEvent() {
        Post post = Post.builder().id("p1").authorId("u2").commentCount(0).createdAt(LocalDateTime.now()).build();
        Comment saved = Comment.builder().id("c1").postId("p1").authorId("u1")
                .authorUsername("alice").content("Nice!").createdAt(LocalDateTime.now()).build();
        when(postRepository.findById("p1")).thenReturn(Optional.of(post));
        when(commentRepository.save(any())).thenReturn(saved);
        when(postRepository.save(any())).thenReturn(post);

        CommentResponse response = postService.addComment("u1", "alice", "p1", new CreateCommentRequest("Nice!"));

        assertThat(response.content()).isEqualTo("Nice!");
        assertThat(post.getCommentCount()).isEqualTo(1);
        verify(outboxEventRepository).save(argThat(e -> e.getEventType().equals("comment.created")));
    }

    @Test
    void getComments_shouldReturnCommentsForPost() {
        Comment c = Comment.builder().id("c1").postId("p1").authorId("u1")
                .content("Hey").createdAt(LocalDateTime.now()).build();
        when(commentRepository.findByPostIdOrderByCreatedAtAsc("p1")).thenReturn(List.of(c));

        List<CommentResponse> result = postService.getComments("p1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().content()).isEqualTo("Hey");
    }
}
