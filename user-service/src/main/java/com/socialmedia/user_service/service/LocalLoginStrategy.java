package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.InvalidCredentialsException;
import com.socialmedia.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocalLoginStrategy implements LoginStrategy {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.LOCAL;
    }

    @Override
    public User resolveUser(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return user;
    }
}
