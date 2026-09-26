package com.socialmedia.post_service.outbox;

import com.socialmedia.post_service.document.OutboxEvent;
import com.socialmedia.post_service.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.KafkaException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPollerTest {

    @Mock OutboxEventRepository outboxEventRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    final MutableClock clock = new MutableClock(Instant.parse("2026-09-26T10:00:00Z"));

    OutboxPoller poller;

    @BeforeEach
    void setUp() {
        poller = new OutboxPoller(outboxEventRepository, kafkaTemplate, clock);
    }

    @Test
    void poll_shouldMarkEventProcessedAfterBrokerAcknowledges() {
        OutboxEvent event = pendingEvent("e1", "post.created", "p1");
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(acknowledged());

        poller.poll();

        assertThat(event.getStatus()).isEqualTo("PROCESSED");
        assertThat(event.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(event);
    }

    @Test
    void poll_shouldSendEventIdAsHeaderKeyedByAggregateId() {
        OutboxEvent event = pendingEvent("e1", "post.created", "p1");
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
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
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(first, second));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

        poller.poll();

        assertThat(first.getStatus()).isEqualTo("PENDING");
        assertThat(second.getStatus()).isEqualTo("PENDING");
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    void poll_shouldSkipPollingDuringBackoff_afterSendFails() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(pendingEvent("e1", "post.created", "p1")));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(brokerUnavailable());

        poller.poll();
        clock.advance(Duration.ofMillis(500));
        poller.poll();

        verify(outboxEventRepository, times(1)).findTop100ByStatusOrderByCreatedAtAsc("PENDING");
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
    }

    @Test
    void poll_shouldDoubleBackoffOnConsecutiveFailures() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(pendingEvent("e1", "post.created", "p1")));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(brokerUnavailable());

        poller.poll();
        clock.advance(Duration.ofSeconds(1));
        poller.poll();
        clock.advance(Duration.ofMillis(1500));
        poller.poll();
        verify(kafkaTemplate, times(2)).send(any(ProducerRecord.class));

        clock.advance(Duration.ofMillis(500));
        poller.poll();
        verify(kafkaTemplate, times(3)).send(any(ProducerRecord.class));
    }

    @Test
    void poll_shouldCapBackoffAtThirtySeconds() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(pendingEvent("e1", "post.created", "p1")));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenReturn(brokerUnavailable());

        for (int attempt = 0; attempt < 10; attempt++) {
            poller.poll();
            clock.advance(Duration.ofMinutes(5));
        }
        poller.poll();
        clock.advance(Duration.ofSeconds(30));
        poller.poll();

        verify(kafkaTemplate, times(12)).send(any(ProducerRecord.class));
    }

    @Test
    void poll_shouldResetBackoff_afterSuccessfulPublish() {
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(pendingEvent("e1", "post.created", "p1")));
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(brokerUnavailable(), brokerUnavailable(), acknowledged(), brokerUnavailable(), acknowledged());

        poller.poll();
        clock.advance(Duration.ofSeconds(1));
        poller.poll();
        clock.advance(Duration.ofSeconds(2));
        poller.poll();
        poller.poll();
        clock.advance(Duration.ofSeconds(1));
        poller.poll();

        verify(kafkaTemplate, times(5)).send(any(ProducerRecord.class));
    }

    @Test
    void poll_shouldKeepEventPending_whenSendThrowsSynchronously() {
        OutboxEvent event = pendingEvent("e1", "post.created", "p1");
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING")).thenReturn(List.of(event));
        when(kafkaTemplate.send(any(ProducerRecord.class))).thenThrow(new KafkaException("Send failed", new TimeoutException("Topic not present in metadata after 5000 ms.")));

        poller.poll();
        clock.advance(Duration.ofMillis(500));
        poller.poll();

        assertThat(event.getStatus()).isEqualTo("PENDING");
        verify(outboxEventRepository, never()).save(any());
        verify(kafkaTemplate, times(1)).send(any(ProducerRecord.class));
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

    private CompletableFuture<SendResult<String, String>> brokerUnavailable() {
        return CompletableFuture.failedFuture(new RuntimeException("broker unavailable"));
    }
}
