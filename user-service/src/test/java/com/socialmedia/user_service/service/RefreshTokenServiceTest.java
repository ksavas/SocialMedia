package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.RotatedRefreshToken;
import com.socialmedia.user_service.entity.RefreshToken;
import com.socialmedia.user_service.exception.InvalidRefreshTokenException;
import com.socialmedia.user_service.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration TTL = Duration.ofDays(7);

    @Mock private RefreshTokenRepository repository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(repository, TTL);
    }

    @Test
    void issue_shouldStoreOnlyTheHashAndReturnTheRawToken() {
        UUID userId = UUID.randomUUID();

        String rawToken = refreshTokenService.issue(userId);

        RefreshToken saved = captureSaved();
        assertThat(rawToken).hasSizeGreaterThanOrEqualTo(43);
        assertThat(saved.getTokenHash()).isEqualTo(sha256(rawToken)).isNotEqualTo(rawToken);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getFamilyId()).isNotNull();
        assertThat(saved.getExpiresAt()).isCloseTo(Instant.now().plus(TTL), within(5, ChronoUnit.SECONDS));
    }

    @Test
    void issue_shouldReturnDifferentTokensEachTime() {
        UUID userId = UUID.randomUUID();

        assertThat(refreshTokenService.issue(userId)).isNotEqualTo(refreshTokenService.issue(userId));
    }

    @Test
    void rotate_shouldMarkCurrentTokenUsedAndIssueNewTokenInSameFamily() {
        RefreshToken current = activeToken("current-token");
        when(repository.findByTokenHash(sha256("current-token"))).thenReturn(Optional.of(current));
        when(repository.markUsed(eq(current.getId()), any(Instant.class))).thenReturn(1);

        RotatedRefreshToken rotated = refreshTokenService.rotate("current-token");

        RefreshToken saved = captureSaved();
        assertThat(rotated.userId()).isEqualTo(current.getUserId());
        assertThat(rotated.token()).isNotEqualTo("current-token");
        assertThat(saved.getTokenHash()).isEqualTo(sha256(rotated.token()));
        assertThat(saved.getFamilyId()).isEqualTo(current.getFamilyId());
        assertThat(saved.getUserId()).isEqualTo(current.getUserId());
    }

    @Test
    void rotate_shouldThrow_whenTokenIsUnknown() {
        when(repository.findByTokenHash(sha256("unknown"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_shouldThrowWithoutIssuing_whenTokenIsExpired() {
        RefreshToken expired = activeToken("expired-token");
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(repository.findByTokenHash(sha256("expired-token"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).markUsed(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_shouldThrowWithoutIssuing_whenTokenIsRevoked() {
        RefreshToken revoked = activeToken("revoked-token");
        revoked.setRevokedAt(Instant.now().minusSeconds(60));
        when(repository.findByTokenHash(sha256("revoked-token"))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rotate_shouldRevokeWholeFamily_whenTokenWasAlreadyUsed() {
        RefreshToken reused = activeToken("reused-token");
        when(repository.findByTokenHash(sha256("reused-token"))).thenReturn(Optional.of(reused));
        when(repository.markUsed(eq(reused.getId()), any(Instant.class))).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.rotate("reused-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
        verify(repository).revokeFamily(eq(reused.getFamilyId()), any(Instant.class));
        verify(repository, never()).save(any());
    }

    @Test
    void revoke_shouldRevokeWholeFamily() {
        RefreshToken token = activeToken("logout-token");
        when(repository.findByTokenHash(sha256("logout-token"))).thenReturn(Optional.of(token));

        refreshTokenService.revoke("logout-token");

        verify(repository).revokeFamily(eq(token.getFamilyId()), any(Instant.class));
    }

    @Test
    void revoke_shouldDoNothing_whenTokenIsUnknown() {
        when(repository.findByTokenHash(sha256("unknown"))).thenReturn(Optional.empty());

        refreshTokenService.revoke("unknown");

        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void purgeExpired_shouldDeleteTokensExpiredBeforeNow() {
        refreshTokenService.purgeExpired();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).deleteByExpiresAtBefore(cutoff.capture());
        assertThat(cutoff.getValue()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
    }

    private RefreshToken activeToken(String rawToken) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .familyId(UUID.randomUUID())
                .tokenHash(sha256(rawToken))
                .expiresAt(Instant.now().plus(TTL))
                .createdAt(Instant.now())
                .build();
    }

    private RefreshToken captureSaved() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
