package com.socialmedia.post_service.controller;

import com.socialmedia.post_service.dto.BatchPostRequest;
import com.socialmedia.post_service.dto.PostResponse;
import com.socialmedia.post_service.dto.PostSummary;
import com.socialmedia.post_service.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/posts")
@RequiredArgsConstructor
public class InternalPostController {

    private final PostService postService;

    @GetMapping("/by-author/{authorId}")
    public List<PostSummary> getPostsByAuthor(@PathVariable String authorId,
                                              @RequestParam(defaultValue = "50") int limit) {
        return postService.getPostsByAuthor(authorId, limit);
    }

    @PostMapping("/batch")
    public List<PostResponse> getPostsByIds(@RequestBody BatchPostRequest request) {
        return postService.getPostsByIds(request.postIds(), request.requesterId());
    }
}
