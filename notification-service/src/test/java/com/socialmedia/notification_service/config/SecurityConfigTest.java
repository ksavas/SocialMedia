package com.socialmedia.notification_service.config;

import com.socialmedia.notification_service.controller.NotificationController;
import com.socialmedia.notification_service.security.JwtService;
import com.socialmedia.notification_service.security.StreamTicketService;
import com.socialmedia.notification_service.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(NotificationController.class)
@Import({SecurityConfig.class, JwtService.class})
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private StreamTicketService streamTicketService;

    @Test
    void healthEndpoint_isNotBehindAuthentication() {
        webTestClient.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn401_whenTokenIsMissing() {
        webTestClient.get().uri("/api/notifications")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().doesNotExist("WWW-Authenticate");
    }

    @Test
    void shouldReturn401_whenTokenIsInvalid() {
        webTestClient.get().uri("/api/notifications")
                .header("Authorization", "Bearer not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
