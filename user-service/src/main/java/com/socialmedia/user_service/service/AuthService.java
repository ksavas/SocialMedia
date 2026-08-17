package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.AuthTokens;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.dto.RotatedRefreshToken;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.InvalidRefreshTokenException;
import com.socialmedia.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final List<LoginStrategy> strategies;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthTokens login(LoginRequest request) {
        LoginStrategy strategy = strategies.stream()
                .filter(s -> s.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + request.provider()));

        return issueTokens(strategy.resolveUser(request));
    }

    public AuthTokens register(RegisterRequest request) {
        return issueTokens(userService.register(request));
    }

    public AuthTokens refresh(String refreshToken) {
        RotatedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(rotated.userId())
                .orElseThrow(() -> new InvalidRefreshTokenException("User no longer exists"));
        return new AuthTokens(toResponse(user), rotated.token());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthTokens issueTokens(User user) {
        return new AuthTokens(toResponse(user), refreshTokenService.issue(user.getId()));
    }

    private AuthResponse toResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getUsername(), user.getProvider().name());
    }
}
