package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.UpdateProfileRequest;
import com.socialmedia.user_service.dto.UserProfileResponse;
import com.socialmedia.user_service.dto.UserSummary;
import com.socialmedia.user_service.service.FollowService;
import com.socialmedia.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;

    @PatchMapping("/me")
    public AuthResponse updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getName(), request);
    }

    @GetMapping
    public List<UserSummary> getUsers(Principal principal) {
        return followService.getUsers(principal.getName());
    }

    @GetMapping("/{id}")
    public UserProfileResponse getProfile(Principal principal, @PathVariable UUID id) {
        return followService.getProfile(principal.getName(), id);
    }

    @PostMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(Principal principal, @PathVariable UUID id) {
        followService.follow(principal.getName(), id);
    }

    @DeleteMapping("/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(Principal principal, @PathVariable UUID id) {
        followService.unfollow(principal.getName(), id);
    }
}
