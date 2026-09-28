package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.dto.UserProfileResponse;
import com.socialmedia.user_service.dto.UserSummary;
import com.socialmedia.user_service.security.RefreshTokenCookies;
import com.socialmedia.user_service.security.SecurityConfig;
import com.socialmedia.user_service.service.FollowService;
import com.socialmedia.user_service.service.JwtService;
import com.socialmedia.user_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtService.class, RefreshTokenCookies.class})
class UserControllerFollowTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean UserService userService;
    @MockitoBean FollowService followService;

    @Test
    void getUsers_shouldReturn200WithList_whenAuthenticated() throws Exception {
        UUID id = UUID.randomUUID();
        when(followService.getUsers("testuser", 0, 20))
                .thenReturn(List.of(new UserSummary(id, "alice", "Alice", false)));

        mockMvc.perform(get("/api/users").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].isFollowing").value(false));
    }

    @Test
    void getUsers_shouldPassPageAndSize() throws Exception {
        when(followService.getUsers("testuser", 2, 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/users").param("page", "2").param("size", "5").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(followService).getUsers("testuser", 2, 5);
    }

    @Test
    void getUsers_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_shouldReturn200_whenAuthenticated() throws Exception {
        UUID id = UUID.randomUUID();
        when(followService.getProfile(eq("testuser"), eq(id)))
                .thenReturn(new UserProfileResponse(id, "alice", "Alice", 10L, 5L, true));

        mockMvc.perform(get("/api/users/{id}", id).with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.followerCount").value(10))
                .andExpect(jsonPath("$.isFollowing").value(true));
    }

    @Test
    void follow_shouldReturn204_whenAuthenticated() throws Exception {
        UUID targetId = UUID.randomUUID();
        doNothing().when(followService).follow(eq("testuser"), eq(targetId));

        mockMvc.perform(post("/api/users/{id}/follow", targetId).with(user("testuser")))
                .andExpect(status().isNoContent());
    }

    @Test
    void follow_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/users/{id}/follow", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unfollow_shouldReturn204_whenAuthenticated() throws Exception {
        UUID targetId = UUID.randomUUID();
        doNothing().when(followService).unfollow(eq("testuser"), eq(targetId));

        mockMvc.perform(delete("/api/users/{id}/follow", targetId).with(user("testuser")))
                .andExpect(status().isNoContent());
    }
}
