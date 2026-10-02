package com.socialmedia.feed_service.consumer;

import com.socialmedia.feed_service.dto.UserFollowedEvent;
import com.socialmedia.feed_service.dto.UserUnfollowedEvent;
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
public class UserEventConsumer {

    private final FeedService feedService;
    private final ProcessedEventStore processedEventStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = {"user.followed", "user.unfollowed"}, groupId = "feed-service")
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

        if ("user.followed".equals(topic)) {
            UserFollowedEvent event = objectMapper.readValue(payload, UserFollowedEvent.class);
            feedService.handleUserFollowed(event.followerId(), event.followingId());
        } else if ("user.unfollowed".equals(topic)) {
            UserUnfollowedEvent event = objectMapper.readValue(payload, UserUnfollowedEvent.class);
            feedService.handleUserUnfollowed(event.followerId(), event.followingId());
        }
    }
}
