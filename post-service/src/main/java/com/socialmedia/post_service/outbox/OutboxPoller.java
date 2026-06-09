package com.socialmedia.post_service.outbox;

import com.socialmedia.post_service.document.OutboxEvent;
import com.socialmedia.post_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : pending) {
            kafkaTemplate.send(event.getEventType(), event.getAggregateId(), event.getPayload());
            event.setStatus("PROCESSED");
            event.setProcessedAt(LocalDateTime.now());
            outboxEventRepository.save(event);
        }
    }
}
