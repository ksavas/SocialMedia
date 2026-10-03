package com.socialmedia.feed_service.config;

import com.socialmedia.feed_service.controller.FeedController;
import com.socialmedia.feed_service.security.JwtService;
import com.socialmedia.feed_service.service.FeedService;
import com.socialmedia.feed_service.service.PostServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@Import({SecurityConfig.class, JwtService.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private PostServiceClient postServiceClient;

    @Test
    void healthEndpoint_isNotBehindAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void shouldReturn401_whenTokenIsInvalid() throws Exception {
        mockMvc.perform(get("/api/feed").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
