package com.socialmedia.post_service.consumer;

import com.socialmedia.post_service.document.ProcessedEvent;
import com.socialmedia.post_service.dto.CounterEvent;
import com.socialmedia.post_service.repository.PostRepository;
import com.socialmedia.post_service.repository.ProcessedEventRepository;
import com.socialmedia.post_service.transaction.TransactionRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCounterConsumer {

    private static final String EVENT_ID_HEADER = "eventId";

    private final PostRepository postRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransactionRunner transactionRunner;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = {"post.liked", "post.unliked", "comment.created"}, groupId = "post-service-counters")
    public void listen(ConsumerRecord<String, String> record) {
        CounterEvent event = objectMapper.readValue(record.value(), CounterEvent.class);
        String eventId = eventId(record);
        try {
            transactionRunner.runInTransaction(() -> {
                if (eventId != null) {
                    processedEventRepository.insert(new ProcessedEvent(eventId, Instant.now()));
                }
                applyCounterChange(record.topic(), event.postId());
            });
        } catch (DuplicateKeyException alreadyApplied) {
            log.debug("Event {} already applied to counters, skipping", eventId);
        }
    }

    private void applyCounterChange(String topic, String postId) {
        switch (topic) {
            case "post.liked" -> postRepository.incrementLikeCount(postId);
            case "post.unliked" -> postRepository.decrementLikeCount(postId);
            case "comment.created" -> postRepository.incrementCommentCount(postId);
            default -> throw new IllegalArgumentException("Unexpected topic: " + topic);
        }
    }

    private String eventId(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_ID_HEADER);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
