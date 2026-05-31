package com.socialmedia.user_service.service;

import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private LoginStrategy localStrategy;
    @Mock private LoginStrategy oauthStrategy;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(List.of(localStrategy, oauthStrategy), jwtService);
    }

    @Test
    void login_shouldReturnAuthResponse_whenLocalStrategySucceeds() {
        User user = User.builder().id(UUID.randomUUID()).username("kaan").build();
        LoginRequest request = new LoginRequest(Provider.LOCAL, "kaan", "secret", null);

        when(localStrategy.supports(Provider.LOCAL)).thenReturn(true);
        when(localStrategy.resolveUser(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt.token");
        assertThat(response.username()).isEqualTo("kaan");
    }

    @Test
    void login_shouldReturnAuthResponse_whenOAuthStrategySucceeds() {
        User user = User.builder().id(UUID.randomUUID()).username("kaan").build();
        LoginRequest request = new LoginRequest(Provider.GOOGLE, null, null, "id.token");

        when(localStrategy.supports(Provider.GOOGLE)).thenReturn(false);
        when(oauthStrategy.supports(Provider.GOOGLE)).thenReturn(true);
        when(oauthStrategy.resolveUser(request)).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt.token");
    }

    @Test
    void login_shouldThrow_whenNoStrategySupportsProvider() {
        LoginRequest request = new LoginRequest(Provider.MICROSOFT, null, null, "id.token");

        when(localStrategy.supports(Provider.MICROSOFT)).thenReturn(false);
        when(oauthStrategy.supports(Provider.MICROSOFT)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
