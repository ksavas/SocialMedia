package com.socialmedia.notification_service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StreamTicketService {

    private static final int TICKET_BYTES = 32;

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock;
    private final Duration ttl;

    public StreamTicketService(Clock clock, @Value("${notification.stream-ticket-ttl:30s}") Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    public String issue(String userId) {
        Instant now = clock.instant();
        tickets.values().removeIf(ticket -> ticket.isExpiredAt(now));
        String value = generate();
        tickets.put(value, new Ticket(userId, now.plus(ttl)));
        return value;
    }

    public Optional<String> redeem(String value) {
        Ticket ticket = tickets.remove(value);
        if (ticket == null || ticket.isExpiredAt(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(ticket.userId());
    }

    int activeTicketCount() {
        return tickets.size();
    }

    private String generate() {
        byte[] bytes = new byte[TICKET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Ticket(String userId, Instant expiresAt) {
        boolean isExpiredAt(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }
}
