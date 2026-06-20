package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.OAuthUserInfo;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.EmailAlreadyRegisteredException;
import com.socialmedia.user_service.exception.InvalidTokenException;
import com.socialmedia.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OAuthLoginStrategy implements LoginStrategy {

    private final List<OAuthTokenVerifier> verifiers;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuthStateService oauthStateService;

    private Map<Provider, OAuthTokenVerifier> verifierMap() {
        return verifiers.stream()
                .collect(Collectors.toMap(OAuthTokenVerifier::getProvider, v -> v));
    }

    @Override
    public boolean supports(Provider provider) {
        return provider == Provider.GOOGLE || provider == Provider.MICROSOFT;
    }

    @Override
    public User resolveUser(LoginRequest request) {
        OAuthTokenVerifier verifier = verifierMap().get(request.provider());
        if (verifier == null) {
            throw new InvalidTokenException("Unsupported provider: " + request.provider());
        }

        String expectedNonce = oauthStateService.validateAndConsume(request.state());
        OAuthUserInfo userInfo = verifier.verify(request.idToken(), expectedNonce);

        if (!userInfo.emailVerified()) {
            throw new InvalidTokenException("Email not verified by " + request.provider());
        }

        return userRepository
                .findByProviderAndProviderId(request.provider(), userInfo.providerId())
                .orElseGet(() -> createOAuthUser(request.provider(), userInfo));
    }

    private User createOAuthUser(Provider provider, OAuthUserInfo userInfo) {
        if (userRepository.findByEmail(userInfo.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException("Email already registered with a different method");
        }

        User user = User.builder()
                .username(generateUniqueUsername(userInfo.email()))
                .email(userInfo.email())
                .displayName(userInfo.name())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .provider(provider)
                .providerId(userInfo.providerId())
                .build();

        return userRepository.save(user);
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf("@")).replaceAll("[^a-zA-Z0-9]", "_");
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }
}
