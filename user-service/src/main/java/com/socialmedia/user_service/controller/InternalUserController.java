package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final FollowService followService;

    @GetMapping("/{userId}/follower-ids")
    public List<String> getFollowerIds(@PathVariable UUID userId) {
        return followService.getFollowerIds(userId);
    }
}
