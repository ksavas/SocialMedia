package com.socialmedia.notification_service.controller;

import com.socialmedia.notification_service.dto.NotificationResponse;
import com.socialmedia.notification_service.security.JwtService;
import com.socialmedia.notification_service.security.UserPrincipal;
import com.socialmedia.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationResponse>> stream(@RequestParam String token) {
        String userId = jwtService.extractUserId(token);
        return notificationService.streamNotifications(userId);
    }

    @GetMapping
    public Flux<NotificationResponse> getNotifications(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return notificationService.getNotifications(principal.userId());
    }

    @PutMapping("/{id}/read")
    public Mono<Void> markAsRead(Authentication authentication, @PathVariable UUID id) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return notificationService.markAsRead(id, principal.userId());
    }
}
