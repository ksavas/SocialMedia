package com.socialmedia.user_service.controller;

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
    void getUsers_shouldPassPageAndSize() throws Exception {
        when(followService.getUsers("testuser", 2, 5)).thenReturn(List.of());

        mockMvc.perform(get("/api/users").param("page", "2").param("size", "5").with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(followService).getUsers("testuser", 2, 5);
    }
}
