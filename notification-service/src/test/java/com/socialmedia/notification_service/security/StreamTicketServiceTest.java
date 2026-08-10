package com.socialmedia.notification_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class StreamTicketServiceTest {

    private static final Duration TTL = Duration.ofSeconds(30);

    private MutableClock clock;
    private StreamTicketService ticketService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-10T10:00:00Z"));
        ticketService = new StreamTicketService(clock, TTL);
    }

    @Test
    void redeem_shouldReturnUserId_forFreshTicket() {
        String ticket = ticketService.issue("user-1");

        assertThat(ticketService.redeem(ticket)).contains("user-1");
    }

    @Test
    void redeem_shouldWorkOnlyOnce() {
        String ticket = ticketService.issue("user-1");

        ticketService.redeem(ticket);

        assertThat(ticketService.redeem(ticket)).isEmpty();
    }

    @Test
    void redeem_shouldReturnEmpty_whenTicketIsExpired() {
        String ticket = ticketService.issue("user-1");

        clock.advance(TTL.plusSeconds(1));

        assertThat(ticketService.redeem(ticket)).isEmpty();
    }

    @Test
    void redeem_shouldReturnEmpty_whenTicketIsUnknown() {
        assertThat(ticketService.redeem("unknown")).isEmpty();
    }

    @Test
    void issue_shouldReturnUnguessableDistinctTickets() {
        String first = ticketService.issue("user-1");
        String second = ticketService.issue("user-1");

        assertThat(first).isNotEqualTo(second).hasSizeGreaterThanOrEqualTo(43);
    }

    @Test
    void issue_shouldDropExpiredTickets() {
        ticketService.issue("user-1");
        clock.advance(TTL.plusSeconds(1));

        ticketService.issue("user-2");

        assertThat(ticketService.activeTicketCount()).isEqualTo(1);
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
