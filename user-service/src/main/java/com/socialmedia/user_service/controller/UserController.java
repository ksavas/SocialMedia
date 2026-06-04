package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.UpdateProfileRequest;
import com.socialmedia.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public AuthResponse updateProfile(Principal principal, @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getName(), request);
    }
}