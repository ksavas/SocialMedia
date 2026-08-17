package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.AuthTokens;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.OAuthInitResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.exception.InvalidRefreshTokenException;
import com.socialmedia.user_service.security.RefreshTokenCookies;
import com.socialmedia.user_service.service.AuthService;
import com.socialmedia.user_service.service.OAuthStateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OAuthStateService oauthStateService;
    private final RefreshTokenCookies refreshTokenCookies;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return withRefreshCookie(HttpStatus.CREATED, authService.register(request));
    }

    @GetMapping("/oauth/init")
    public OAuthInitResponse oauthInit() {
        return oauthStateService.initiate();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }
        return withRefreshCookie(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear())
                .build();
    }

    private ResponseEntity<AuthResponse> withRefreshCookie(HttpStatus status, AuthTokens tokens) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.create(tokens.refreshToken()))
                .body(tokens.response());
    }
}
