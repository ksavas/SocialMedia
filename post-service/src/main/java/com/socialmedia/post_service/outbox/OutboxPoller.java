package com.socialmedia.post_service.outbox;

import com.socialmedia.post_service.document.OutboxEvent;
import com.socialmedia.post_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPoller {

    public static final String EVENT_ID_HEADER = "eventId";
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        for (OutboxEvent event : outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")) {
            if (!publish(event)) {
                return;
            }
            event.setStatus("PROCESSED");
            event.setProcessedAt(Instant.now());
            outboxEventRepository.save(event);
        }
    }

    private boolean publish(OutboxEvent event) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(event.getEventType(), event.getAggregateId(), event.getPayload());
        record.headers().add(EVENT_ID_HEADER, event.getId().getBytes(StandardCharsets.UTF_8));
        try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException e) {
            log.warn("Outbox event {} could not be published, will retry: {}", event.getId(), e.getMessage());
            return false;
        }
    }
}
