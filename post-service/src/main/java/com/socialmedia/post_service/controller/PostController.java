package com.socialmedia.post_service.controller;

import com.socialmedia.post_service.dto.CommentResponse;
import com.socialmedia.post_service.dto.CreateCommentRequest;
import com.socialmedia.post_service.dto.CreatePostRequest;
import com.socialmedia.post_service.dto.PostResponse;
import com.socialmedia.post_service.security.UserPrincipal;
import com.socialmedia.post_service.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public List<PostResponse> getAllPosts(@AuthenticationPrincipal UserPrincipal principal) {
        return postService.getAllPosts(principal.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(principal.userId(), principal.username(), request);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@AuthenticationPrincipal UserPrincipal principal,
                                @PathVariable String id) {
        return postService.getPost(principal.userId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable String id) {
        postService.deletePost(principal.userId(), id);
    }

    @PostMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void likePost(@AuthenticationPrincipal UserPrincipal principal,
                         @PathVariable String id) {
        postService.likePost(principal.userId(), id);
    }

    @DeleteMapping("/{id}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlikePost(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable String id) {
        postService.unlikePost(principal.userId(), id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable String id,
                                      @Valid @RequestBody CreateCommentRequest request) {
        return postService.addComment(principal.userId(), principal.username(), id, request);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(@PathVariable String id) {
        return postService.getComments(id);
    }
}
