package com.socialmedia.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.RegisterRequest;
import com.socialmedia.user_service.exception.UserAlreadyExistsException;
import com.socialmedia.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;

import com.socialmedia.user_service.dto.OAuthRequest;
import com.socialmedia.user_service.entity.Provider;
import com.socialmedia.user_service.service.OAuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private OAuthService oAuthService;

    @Test
    void register_shouldReturn201_whenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testuser", "test@example.com", "password123", "Test User"
        );
        AuthResponse response = new AuthResponse("jwt-token", "testuser");

        when(userService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testuser", "test@example.com", "password123", "Test User"
        );

        when(userService.register(any())).thenThrow(new UserAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void register_shouldReturn400_whenInvalidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", "", "", ""
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleAuth_shouldReturn200_whenValidIdToken() throws Exception {
        OAuthRequest request = new OAuthRequest("valid.google.token");
        AuthResponse authResponse = new AuthResponse("jwt.token.here", "testUser");

        when(oAuthService.authenticate(Provider.GOOGLE, "valid.google.token"))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/oauth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.username").value("testUser"));
    }

    @Test
    void microsoftAuth_shouldReturn200_whenValidIdToken() throws Exception {
        OAuthRequest request = new OAuthRequest("valid.microsoft.token");
        AuthResponse authResponse = new AuthResponse("jwt.token.here", "testUser");

        when(oAuthService.authenticate(Provider.MICROSOFT, "valid.microsoft.token"))
                .thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/oauth/microsoft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.username").value("testUser"));
    }
}