package com.socialmedia.notification_service.controller;

import com.socialmedia.notification_service.dto.NotificationResponse;
import com.socialmedia.notification_service.dto.StreamTicketResponse;
import com.socialmedia.notification_service.security.StreamTicketService;
import com.socialmedia.notification_service.security.UserPrincipal;
import com.socialmedia.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final StreamTicketService streamTicketService;

    @PostMapping("/stream-ticket")
    public StreamTicketResponse streamTicket(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new StreamTicketResponse(streamTicketService.issue(principal.userId()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationResponse>> stream(@RequestParam String ticket) {
        String userId = streamTicketService.redeem(ticket)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired stream ticket"));
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
