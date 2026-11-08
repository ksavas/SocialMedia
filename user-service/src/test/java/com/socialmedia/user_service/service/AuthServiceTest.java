package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthTokens;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.RotatedRefreshToken;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import com.socialmedia.user_service.exception.InvalidRefreshTokenException;
import com.socialmedia.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private LoginStrategy localStrategy;
    @Mock private LoginStrategy oauthStrategy;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                List.of(localStrategy, oauthStrategy), jwtService, refreshTokenService, userService, userRepository);
    }

    @Test
    void login_shouldReturnAccessAndRefreshTokens_whenLocalStrategySucceeds() {
        User user = User.builder().id(UUID.randomUUID()).username("kaan").build();
        LoginRequest request = new LoginRequest(Provider.LOCAL, "kaan", "secret", null, null);

        when(localStrategy.supports(Provider.LOCAL)).thenReturn(true);
        when(localStrategy.resolveUser(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt.token");
        when(refreshTokenService.issue(user.getId())).thenReturn("refresh-token");

        AuthTokens tokens = authService.login(request);

        assertThat(tokens.response().token()).isEqualTo("jwt.token");
        assertThat(tokens.response().username()).isEqualTo("kaan");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void login_shouldThrow_whenNoStrategySupportsProvider() {
        LoginRequest request = new LoginRequest(Provider.MICROSOFT, null, null, "id.token", "some-state");

        when(localStrategy.supports(Provider.MICROSOFT)).thenReturn(false);
        when(oauthStrategy.supports(Provider.MICROSOFT)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refresh_shouldReturnNewAccessTokenAndRotatedRefreshToken() {
        User user = User.builder().id(UUID.randomUUID()).username("kaan").build();

        when(refreshTokenService.rotate("old-refresh")).thenReturn(new RotatedRefreshToken(user.getId(), "new-refresh"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new.jwt");

        AuthTokens tokens = authService.refresh("old-refresh");

        assertThat(tokens.response().token()).isEqualTo("new.jwt");
        assertThat(tokens.response().username()).isEqualTo("kaan");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_shouldThrow_whenUserNoLongerExists() {
        UUID userId = UUID.randomUUID();

        when(refreshTokenService.rotate("old-refresh")).thenReturn(new RotatedRefreshToken(userId, "new-refresh"));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-refresh"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
