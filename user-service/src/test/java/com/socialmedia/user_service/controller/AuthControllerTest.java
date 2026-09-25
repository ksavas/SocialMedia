package com.socialmedia.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.AuthTokens;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.OAuthInitResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.exception.InvalidCredentialsException;
import com.socialmedia.user_service.exception.InvalidRefreshTokenException;
import com.socialmedia.user_service.exception.UserAlreadyExistsException;
import com.socialmedia.user_service.security.RefreshTokenCookies;
import com.socialmedia.user_service.security.SecurityConfig;
import com.socialmedia.user_service.service.AuthService;
import com.socialmedia.user_service.service.JwtService;
import com.socialmedia.user_service.service.OAuthStateService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, RefreshTokenCookies.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String REFRESH_COOKIE = "refresh_token";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OAuthStateService oauthStateService;

    @Test
    void register_shouldReturn201_whenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123", "Test User");
        when(authService.register(any())).thenReturn(tokens("jwt-token", "testuser", "LOCAL", "refresh-token"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(cookie().value(REFRESH_COOKIE, "refresh-token"));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123", "Test User");
        when(authService.register(any())).thenThrow(new UserAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void register_shouldReturn400_whenInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("", "", "", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn200_whenValidLocalCredentials() throws Exception {
        LoginRequest request = new LoginRequest(Provider.LOCAL, "testuser", "password123", null, null);
        when(authService.login(any())).thenReturn(tokens("jwt-token", "testuser", "LOCAL", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value(REFRESH_COOKIE, "refresh-token"))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andExpect(cookie().secure(REFRESH_COOKIE, true))
                .andExpect(cookie().sameSite(REFRESH_COOKIE, "Strict"))
                .andExpect(cookie().path(REFRESH_COOKIE, "/api/auth"))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 604800));
    }

    @Test
    void login_shouldReturn200_whenValidGoogleToken() throws Exception {
        LoginRequest request = new LoginRequest(Provider.GOOGLE, null, null, "valid.google.token", "valid-state");
        when(authService.login(any())).thenReturn(tokens("jwt-token", "testuser", "GOOGLE", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401_whenInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest(Provider.LOCAL, "testuser", "wrong", null, null);
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oauthInit_shouldReturn200_withStateAndNonce() throws Exception {
        when(oauthStateService.initiate()).thenReturn(new OAuthInitResponse("state-abc", "nonce-xyz"));

        mockMvc.perform(get("/api/auth/oauth/init"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("state-abc"))
                .andExpect(jsonPath("$.nonce").value("nonce-xyz"));
    }

    @Test
    void refresh_shouldReturnNewAccessTokenAndRotateCookie() throws Exception {
        when(authService.refresh("old-refresh")).thenReturn(tokens("new-jwt", "testuser", "LOCAL", "new-refresh"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(cookie().value(REFRESH_COOKIE, "new-refresh"))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true));
    }

    @Test
    void refresh_shouldReturn401_whenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
        verify(authService, never()).refresh(any());
    }

    @Test
    void refresh_shouldReturn401AndClearCookie_whenRefreshTokenIsInvalid() throws Exception {
        when(authService.refresh("stale-refresh")).thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie(REFRESH_COOKIE, "stale-refresh")))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().value(REFRESH_COOKIE, ""))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0))
                .andExpect(cookie().path(REFRESH_COOKIE, "/api/auth"));
    }

    @Test
    void logout_shouldRevokeRefreshTokenAndClearCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie(REFRESH_COOKIE, "refresh-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
        verify(authService).logout("refresh-token");
    }

    @Test
    void logout_shouldClearCookie_whenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
        verify(authService, never()).logout(any());
    }

    private static AuthTokens tokens(String accessToken, String username, String provider, String refreshToken) {
        return new AuthTokens(new AuthResponse(accessToken, username, provider), refreshToken);
    }
}
