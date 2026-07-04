package com.socialmedia.post_service.outbox;

import com.socialmedia.post_service.document.OutboxEvent;
import com.socialmedia.post_service.repository.OutboxEventRepository;
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
import java.time.Instant;
import java.util.List;
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
        OutboxEvent event = pendingEvent("e1", "post.created", "p1");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledged());

        poller.poll();

        assertThat(event.getStatus()).isEqualTo("PROCESSED");
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void poll_shouldSendEventIdAsHeaderKeyedByAggregateId() {
        OutboxEvent event = pendingEvent("e1", "post.created", "p1");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledged());

        poller.poll();

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, String> record = captor.getValue();
        Header header = record.headers().lastHeader(OutboxPoller.EVENT_ID_HEADER);
        assertThat(record.topic()).isEqualTo("post.created");
        assertThat(record.key()).isEqualTo("p1");
        assertThat(new String(header.value(), StandardCharsets.UTF_8)).isEqualTo("e1");
    }

    @Test
    void poll_shouldKeepEventPendingAndStop_whenSendFails() {
        OutboxEvent first = pendingEvent("e1", "post.created", "p1");
        OutboxEvent second = pendingEvent("e2", "post.created", "p2");
        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        poller.poll();

        assertThat(first.getStatus()).isEqualTo("PENDING");
        assertThat(second.getStatus()).isEqualTo("PENDING");
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        verify(outboxEventRepository, never()).save(any());
    }

    private OutboxEvent pendingEvent(String id, String eventType, String aggregateId) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateType("Post")
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload("{}")
                .createdAt(Instant.now())
                .build();
    }

    private CompletableFuture<SendResult<String, String>> acknowledged() {
        return CompletableFuture.completedFuture(null);
    }
}
