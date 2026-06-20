package com.socialmedia.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.LoginRequest;
import com.socialmedia.user_service.dto.OAuthInitResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.exception.InvalidCredentialsException;
import com.socialmedia.user_service.exception.UserAlreadyExistsException;
import com.socialmedia.user_service.service.AuthService;
import com.socialmedia.user_service.service.OAuthStateService;
import com.socialmedia.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OAuthStateService oauthStateService;

    @Test
    void register_shouldReturn201_whenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123", "Test User");
        when(userService.register(any())).thenReturn(new AuthResponse("jwt-token", "testuser", "LOCAL"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password123", "Test User");
        when(userService.register(any())).thenThrow(new UserAlreadyExistsException("Email already exists"));

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
        when(authService.login(any())).thenReturn(new AuthResponse("jwt-token", "testuser", "LOCAL"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void login_shouldReturn200_whenValidGoogleToken() throws Exception {
        LoginRequest request = new LoginRequest(Provider.GOOGLE, null, null, "valid.google.token", "valid-state");
        when(authService.login(any())).thenReturn(new AuthResponse("jwt-token", "testuser", "GOOGLE"));

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
}
