package com.socialmedia.user_service.outbox;

import com.socialmedia.user_service.entity.OutboxEvent;
import com.socialmedia.user_service.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    OutboxPoller poller;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(outboxEventRepository, kafkaTemplate);
    }

    @Test
    void poll_shouldMarkEventProcessedAfterBrokerAcknowledges() {
        OutboxEvent event = pendingEvent("user.followed", "follower-1");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledged());

        poller.poll();

        assertThat(event.getStatus()).isEqualTo("PROCESSED");
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void poll_shouldSendEventIdAsHeaderKeyedByAggregateId() {
        OutboxEvent event = pendingEvent("user.followed", "follower-1");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledged());

        poller.poll();

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();
        Header header = record.headers().lastHeader(OutboxPoller.EVENT_ID_HEADER);
        assertThat(record.topic()).isEqualTo("user.followed");
        assertThat(record.key()).isEqualTo("follower-1");
        assertThat(new String(header.value(), StandardCharsets.UTF_8)).isEqualTo(event.getId().toString());
    }

    @Test
    void poll_shouldKeepEventPendingAndStop_whenSendFails() {
        OutboxEvent first = pendingEvent("user.followed", "follower-1");
        OutboxEvent second = pendingEvent("user.unfollowed", "follower-1");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        poller.poll();

        assertThat(first.getStatus()).isEqualTo("PENDING");
        assertThat(second.getStatus()).isEqualTo("PENDING");
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        verify(outboxEventRepository, never()).save(any());
    }

    private OutboxEvent pendingEvent(String eventType, String aggregateId) {
        return OutboxEvent.builder()
                .id(UUID.randomUUID())
                .aggregateType("User")
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload("{}")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CompletableFuture<SendResult<String, String>> acknowledged() {
        return CompletableFuture.completedFuture(null);
    }
}
