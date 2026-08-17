package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.RotatedRefreshToken;
import com.socialmedia.user_service.entity.RefreshToken;
import com.socialmedia.user_service.exception.InvalidRefreshTokenException;
import com.socialmedia.user_service.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository repository;
    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${jwt.refresh-expiration}") Duration ttl) {
        this.repository = repository;
        this.ttl = ttl;
    }

    @Transactional
    public String issue(UUID userId) {
        return store(userId, UUID.randomUUID(), Instant.now());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(now)) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked");
        }
        if (repository.markUsed(current.getId(), now) == 0) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw new InvalidRefreshTokenException("Refresh token reuse detected");
        }

        String next = store(current.getUserId(), current.getFamilyId(), now);
        return new RotatedRefreshToken(current.getUserId(), next);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), Instant.now()));
    }

    @Scheduled(cron = "${jwt.refresh-purge-cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
    }

    private String store(UUID userId, UUID familyId, Instant now) {
        String rawToken = generateRawToken();
        repository.save(RefreshToken.builder()
                .userId(userId)
                .familyId(familyId)
                .tokenHash(hash(rawToken))
                .createdAt(now)
                .expiresAt(now.plus(ttl))
                .build());
        return rawToken;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
