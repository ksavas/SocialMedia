package com.socialmedia.feed_service.config;

import com.socialmedia.feed_service.service.EventInProgressException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaConfig {

    private static final long RETRY_WITHOUT_LIMIT_INTERVAL_MS = 5_000L;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.consumer.concurrency:3}") int concurrency) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLQ", -1)
        );

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setCommonErrorHandler(consumerErrorHandler(recoverer));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler());
        return factory;
    }

    static DefaultErrorHandler consumerErrorHandler(ConsumerRecordRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxElapsedTime(10_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(JacksonException.class);
        errorHandler.setBackOffFunction((record, failure) -> retryWithoutLimitBackOff(failure));
        return errorHandler;
    }

    static BackOff retryWithoutLimitBackOff(Exception failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ResourceAccessException
                    || cause instanceof CallNotPermittedException
                    || cause instanceof EventInProgressException
                    || cause instanceof DataAccessResourceFailureException
                    || cause instanceof QueryTimeoutException) {
                return new FixedBackOff(RETRY_WITHOUT_LIMIT_INTERVAL_MS, FixedBackOff.UNLIMITED_ATTEMPTS);
            }
        }
        return null;
    }
}
