package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.EmailAlreadyRegisteredException;
import com.socialmedia.user_service.exception.InvalidTokenException;
import com.socialmedia.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthResponse googleAuth(String idToken) {
        OAuthUserInfo userInfo = googleTokenVerifier.verify(idToken);

        if (!userInfo.emailVerified()) {
            throw new InvalidTokenException("Email not verified by Google");
        }

        Optional<User> existingGoogleUser = userRepository
                .findByProviderAndProviderId(Provider.GOOGLE, userInfo.providerId());

        if (existingGoogleUser.isPresent()) {
            User user = existingGoogleUser.get();
            String token = jwtService.generateToken(user);
            return new AuthResponse(token, user.getUsername());
        }

        Optional<User> emailUser = userRepository.findByEmail(userInfo.email());
        if (emailUser.isPresent()) {
            throw new EmailAlreadyRegisteredException(
                    "Email already registered with a different method"
            );
        }

        String username = generateUniqueUsername(userInfo.email());
        User newUser = User.builder()
                .username(username)
                .email(userInfo.email())
                .displayName(userInfo.name())
                .password(null)
                .provider(Provider.GOOGLE)
                .providerId(userInfo.providerId())
                .build();

        User savedUser = userRepository.save(newUser);
        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token, savedUser.getUsername());
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf("@"))
                .replaceAll("[^a-zA-Z0-9]", "_");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }
}