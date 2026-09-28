package com.socialmedia.notification_service.controller;

import com.socialmedia.notification_service.config.SecurityConfig;
import com.socialmedia.notification_service.security.JwtService;
import com.socialmedia.notification_service.security.StreamTicketService;
import com.socialmedia.notification_service.security.UserPrincipal;
import com.socialmedia.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(NotificationController.class)
@Import({SecurityConfig.class, JwtService.class})
class NotificationControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private StreamTicketService streamTicketService;

    @Test
    void streamTicket_shouldReturnTicketForAuthenticatedUser() {
        when(streamTicketService.issue("user-1")).thenReturn("ticket-abc");

        webTestClient.mutateWith(mockAuthentication(authenticated("user-1")))
                .post().uri("/api/notifications/stream-ticket")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.ticket").isEqualTo("ticket-abc");
    }

    @Test
    void getNotifications_shouldPassPageAndSize() {
        when(notificationService.getNotifications("user-1", 1, 5)).thenReturn(Flux.empty());

        webTestClient.mutateWith(mockAuthentication(authenticated("user-1")))
                .get().uri("/api/notifications?page=1&size=5")
                .exchange()
                .expectStatus().isOk();
        verify(notificationService).getNotifications("user-1", 1, 5);
    }

    @Test
    void unreadCount_shouldReturnCountForAuthenticatedUser() {
        when(notificationService.countUnread("user-1")).thenReturn(Mono.just(3L));

        webTestClient.mutateWith(mockAuthentication(authenticated("user-1")))
                .get().uri("/api/notifications/unread-count")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.count").isEqualTo(3);
    }

    @Test
    void streamTicket_shouldReturn401_whenNotAuthenticated() {
        webTestClient.post().uri("/api/notifications/stream-ticket")
                .exchange()
                .expectStatus().isUnauthorized();
        verify(streamTicketService, never()).issue(anyString());
    }

    @Test
    void stream_shouldOpenStreamForUserOfValidTicket() {
        when(streamTicketService.redeem("ticket-abc")).thenReturn(Optional.of("user-1"));
        when(notificationService.streamNotifications("user-1")).thenReturn(Flux.empty());

        webTestClient.get().uri("/api/notifications/stream?ticket=ticket-abc")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
        verify(notificationService).streamNotifications("user-1");
    }

    @Test
    void stream_shouldReturn401_whenTicketIsInvalid() {
        when(streamTicketService.redeem("stale")).thenReturn(Optional.empty());

        webTestClient.get().uri("/api/notifications/stream?ticket=stale")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isUnauthorized();
        verify(notificationService, never()).streamNotifications(anyString());
    }

    @Test
    void stream_shouldNotAcceptJwtInQueryString() {
        webTestClient.get().uri("/api/notifications/stream?token=some.jwt.value")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().is4xxClientError();
        verify(notificationService, never()).streamNotifications(anyString());
    }

    private static UsernamePasswordAuthenticationToken authenticated(String userId) {
        return new UsernamePasswordAuthenticationToken(new UserPrincipal(userId, "alice"), null, List.of());
    }
}
