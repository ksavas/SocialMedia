package com.socialmedia.user_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialmedia.user_service.dto.AuthResponse;
import com.socialmedia.user_service.dto.UpdateProfileRequest;
import com.socialmedia.user_service.exception.UserAlreadyExistsException;
import com.socialmedia.user_service.security.RefreshTokenCookies;
import com.socialmedia.user_service.security.SecurityConfig;
import com.socialmedia.user_service.service.FollowService;
import com.socialmedia.user_service.service.JwtService;
import com.socialmedia.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtService.class, RefreshTokenCookies.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FollowService followService;

    @Test
    void updateProfile_shouldReturn200AndNewToken_whenValidRequest() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("newuser", null, null);
        when(userService.updateProfile(eq("testuser"), any()))
                .thenReturn(new AuthResponse("new-jwt", "newuser", "LOCAL"));

        mockMvc.perform(patch("/api/users/me")
                        .with(user("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.provider").value("LOCAL"));
    }

    @Test
    void updateProfile_shouldReturn409_whenUsernameAlreadyTaken() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("takenuser", null, null);
        when(userService.updateProfile(eq("testuser"), any()))
                .thenThrow(new UserAlreadyExistsException("This username is already taken"));

        mockMvc.perform(patch("/api/users/me")
                        .with(user("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This username is already taken"));
    }
}
