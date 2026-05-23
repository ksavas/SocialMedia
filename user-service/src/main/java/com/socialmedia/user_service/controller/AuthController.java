package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.OAuthRequest;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.service.OAuthService;
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
    private final OAuthService oAuthService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/oauth/google")
    public AuthResponse googleAuth(@Valid @RequestBody OAuthRequest request) {
        return oAuthService.authenticate(Provider.GOOGLE, request.idToken());
    }

    @PostMapping("/oauth/microsoft")
    public AuthResponse microsoftAuth(@Valid @RequestBody OAuthRequest request) {
        return oAuthService.authenticate(Provider.MICROSOFT, request.idToken());
    }
}