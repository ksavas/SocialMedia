package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.OAuthInitResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.service.AuthService;
import com.socialmedia.user_service.service.OAuthStateService;
import com.socialmedia.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final OAuthStateService oauthStateService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @GetMapping("/oauth/init")
    public OAuthInitResponse oauthInit() {
        return oauthStateService.initiate();
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
