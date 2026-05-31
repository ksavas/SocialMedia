package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final List<LoginStrategy> strategies;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        LoginStrategy strategy = strategies.stream()
                .filter(s -> s.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + request.provider()));

        User user = strategy.resolveUser(request);
        return new AuthResponse(jwtService.generateToken(user), user.getUsername());
    }
}
