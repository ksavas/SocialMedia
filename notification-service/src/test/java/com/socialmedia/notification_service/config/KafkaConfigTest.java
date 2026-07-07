package com.socialmedia.notification_service.config;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KafkaConfigTest {

    private final ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);
    private final DefaultErrorHandler errorHandler = KafkaConfig.consumerErrorHandler(recoverer);
    private final ConsumerRecord<String, String> record = new ConsumerRecord<>("post.liked", 0, 0L, "key", "{bad");

    @Test
    void malformedJson_isSentToRecovererWithoutRetry() {
        Exception failure = new ListenerExecutionFailedException("listener failed", malformedJsonException());

        errorHandler.handleRemaining(failure, List.of(record), mock(Consumer.class), mock(MessageListenerContainer.class));

        verify(recoverer).accept(eq(record), any());
    }

    @Test
    void transientFailure_isRetriedBeforeRecovery() {
        Exception failure = new ListenerExecutionFailedException("listener failed", new IllegalStateException("database down"));

        assertThatThrownBy(() -> errorHandler.handleRemaining(
                failure, List.of(record), mock(Consumer.class), mock(MessageListenerContainer.class)))
                .hasMessageContaining("Record in retry and not yet recovered");

        verify(recoverer, never()).accept(any(), any());
    }

    private JacksonException malformedJsonException() {
        try {
            new ObjectMapper().readTree("{bad");
        } catch (JacksonException e) {
            return e;
        }
        throw new IllegalStateException("expected malformed JSON to fail");
    }
}
