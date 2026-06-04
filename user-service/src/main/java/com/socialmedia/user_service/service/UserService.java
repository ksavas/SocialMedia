package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.dto.UpdateProfileRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.EmailAlreadyRegisteredException;
import com.socialmedia.user_service.exception.UserAlreadyExistsException;
import com.socialmedia.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();

        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), user.getUsername(), user.getProvider().name());
    }

    public AuthResponse updateProfile(String currentUsername, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndIdNot(request.username(), user.getId())) {
                throw new UserAlreadyExistsException("This username is already taken");
            }
            user.setUsername(request.username());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (user.getProvider() == Provider.LOCAL) {
            if (request.email() != null && !request.email().equals(user.getEmail())) {
                if (userRepository.existsByEmailAndIdNot(request.email(), user.getId())) {
                    throw new EmailAlreadyRegisteredException("This email is already registered");
                }
                user.setEmail(request.email());
            }
        }

        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), user.getUsername(), user.getProvider().name());
    }
}
