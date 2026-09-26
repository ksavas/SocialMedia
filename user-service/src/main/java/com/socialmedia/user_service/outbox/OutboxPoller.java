package com.socialmedia.user_service.outbox;

import com.socialmedia.user_service.entity.OutboxEvent;
import com.socialmedia.user_service.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class OutboxPoller {

    public static final String EVENT_ID_HEADER = "eventId";
    private static final long SEND_TIMEOUT_SECONDS = 10;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_BACKOFF = Duration.ofSeconds(30);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    private Duration backoff = Duration.ZERO;
    private Instant nextAttemptAt = Instant.MIN;

    @Autowired
    public OutboxPoller(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this(outboxEventRepository, kafkaTemplate, Clock.systemUTC());
    }

    OutboxPoller(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate, Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        if (clock.instant().isBefore(nextAttemptAt)) {
            return;
        }
        for (OutboxEvent event : outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")) {
            if (!publish(event)) {
                backOff();
                return;
            }
            backoff = Duration.ZERO;
            event.setStatus("PROCESSED");
            event.setProcessedAt(LocalDateTime.now(clock));
            outboxEventRepository.save(event);
        }
    }

    private void backOff() {
        backoff = nextBackoff();
        nextAttemptAt = clock.instant().plus(backoff);
    }

    private static Duration min(Duration a, Duration b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private boolean publish(OutboxEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(event.getEventType(), event.getAggregateId(), event.getPayload());
        record.headers().add(EVENT_ID_HEADER, event.getId().toString().getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            log.warn("Outbox event {} could not be published, retrying in {}: {}",
                    event.getId(), nextBackoff(), e.getMessage());
            return false;
        }
    }

    private Duration nextBackoff() {
        return backoff.isZero() ? INITIAL_BACKOFF : min(backoff.multipliedBy(2), MAX_BACKOFF);
    }
}
