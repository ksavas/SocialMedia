package com.socialmedia.feed_service.consumer;

import com.socialmedia.feed_service.dto.PostCreatedEvent;
import com.socialmedia.feed_service.dto.PostDeletedEvent;
import com.socialmedia.feed_service.service.FeedService;
import com.socialmedia.feed_service.service.ProcessedEventStore;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostEventConsumer {

    private final FeedService feedService;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = {"post.created", "post.deleted"}, groupId = "feed-service")
    public void listen(ConsumerRecord<String, String> record) throws Exception {
        Optional<String> eventId = EventHeaders.eventId(record);
        if (eventId.isPresent() && !processedEventStore.tryStart(eventId.get())) {
            return;
        }
        try {
            handle(record);
        } catch (Exception e) {
            eventId.ifPresent(processedEventStore::release);
            throw e;
        }
        eventId.ifPresent(processedEventStore::markProcessed);
    }

    private void handle(ConsumerRecord<String, String> record) throws Exception {
        String topic = record.topic();
        String payload = record.value();

        if ("post.created".equals(topic)) {
            PostCreatedEvent event = objectMapper.readValue(payload, PostCreatedEvent.class);
            long score = event.createdAt() != null ? event.createdAt().toEpochMilli() : record.timestamp();
            feedService.handlePostCreated(event.postId(), event.authorId(), score);
        } else if ("post.deleted".equals(topic)) {
            PostDeletedEvent event = objectMapper.readValue(payload, PostDeletedEvent.class);
            feedService.handlePostDeleted(event.postId(), event.authorId());
        }
    }
}
