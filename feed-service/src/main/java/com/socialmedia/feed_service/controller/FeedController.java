package com.socialmedia.feed_service.controller;

import com.socialmedia.feed_service.dto.PostDto;
import com.socialmedia.feed_service.security.UserPrincipal;
import com.socialmedia.feed_service.service.FeedService;
import com.socialmedia.feed_service.service.PostServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final PostServiceClient postServiceClient;

    @GetMapping
    public List<PostDto> getFeed(@AuthenticationPrincipal UserPrincipal principal,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        List<String> postIds = feedService.getFeedPostIds(principal.userId(), page, size);
        if (postIds.isEmpty()) {
            return List.of();
        }
        return postServiceClient.getPostsByIds(postIds, principal.userId());
    }
}
