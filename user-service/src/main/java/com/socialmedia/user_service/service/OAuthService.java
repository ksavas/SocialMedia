package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.EmailAlreadyRegisteredException;
import com.socialmedia.user_service.exception.InvalidTokenException;
import com.socialmedia.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OAuthService {

    private final Map<Provider, OAuthTokenVerifier> verifiers;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public OAuthService(
            List<OAuthTokenVerifier> verifierList,
            UserRepository userRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.verifiers = verifierList.stream()
                .collect(Collectors.toMap(OAuthTokenVerifier::getProvider, v -> v));
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse authenticate(Provider provider, String idToken) {
        OAuthTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new InvalidTokenException("Unsupported provider: " + provider);
        }

        OAuthUserInfo userInfo = verifier.verify(idToken);

        if (!userInfo.emailVerified()) {
            throw new InvalidTokenException("Email not verified by " + provider);
        }

        Optional<User> existingUser = userRepository
                .findByProviderAndProviderId(provider, userInfo.providerId());

        if (existingUser.isPresent()) {
            User user = existingUser.get();
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
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .provider(provider)
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