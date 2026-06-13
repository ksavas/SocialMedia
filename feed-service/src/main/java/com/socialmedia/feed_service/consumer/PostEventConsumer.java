package com.socialmedia.feed_service.consumer;

import com.socialmedia.feed_service.dto.PostCreatedEvent;
import com.socialmedia.feed_service.dto.PostDeletedEvent;
import com.socialmedia.feed_service.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PostEventConsumer {

    private final FeedService feedService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = {"post.created", "post.deleted"}, groupId = "feed-service")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String topic = record.topic();
            String payload = record.value();
            if ("post.created".equals(topic)) {
                PostCreatedEvent event = objectMapper.readValue(payload, PostCreatedEvent.class);
                feedService.handlePostCreated(event.postId(), event.authorId(), record.timestamp());
            } else if ("post.deleted".equals(topic)) {
                PostDeletedEvent event = objectMapper.readValue(payload, PostDeletedEvent.class);
                feedService.handlePostDeleted(event.postId(), event.authorId());
            }
        } catch (Exception ignored) {}
    }
}
