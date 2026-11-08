package com.socialmedia.post_service.controller;

import com.socialmedia.post_service.dto.PostSummary;
import com.socialmedia.post_service.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HybridFeedControllerTest {
    private final PostService service = mock(PostService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new InternalPostController(service)).build();
    }

    @Test
    void pullEndpointBindsAudienceAndSerializesPostSummaries() throws Exception {
        when(service.getPullFeedPosts(List.of("author"), 1000)).thenReturn(List.of(
                new PostSummary("p1", "author", Instant.parse("2026-10-17T10:00:00Z"))));

        mvc.perform(post("/api/internal/posts/pull-feed").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorIds\":[\"author\"],\"limit\":1000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("p1"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-17T10:00:00Z"));
    }
}
