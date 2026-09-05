package com.socialmedia.user_service.controller;

import com.socialmedia.user_service.service.FollowService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HybridFeedControllerTest {
    @Test
    void exposesFollowerCountAndFollowingIdsAsJson() throws Exception {
        FollowService service = mock(FollowService.class);
        UUID id = UUID.randomUUID();
        when(service.getFollowerCount(id)).thenReturn(10000L);
        when(service.getFollowingIds(id)).thenReturn(List.of("author"));
        var mvc = MockMvcBuilders.standaloneSetup(new InternalUserController(service)).build();

        mvc.perform(get("/api/internal/users/{id}/follower-count", id))
                .andExpect(status().isOk()).andExpect(content().string("10000"));
        mvc.perform(get("/api/internal/users/{id}/following-ids", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("author"));
    }
}
