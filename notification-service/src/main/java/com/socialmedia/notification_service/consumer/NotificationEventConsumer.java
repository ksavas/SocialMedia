package com.socialmedia.notification_service.consumer;

import com.socialmedia.notification_service.dto.CommentCreatedEvent;
import com.socialmedia.notification_service.dto.FeedUpdatedEvent;
import com.socialmedia.notification_service.dto.PostLikedEvent;
import com.socialmedia.notification_service.dto.UserFollowedEvent;
import com.socialmedia.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private static final String EVENT_ID_HEADER = "eventId";
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(10);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = {"user.followed", "post.liked", "comment.created", "feed.updated"}, groupId = "notification-service")
    public void listen(ConsumerRecord<String, String> record) {
        String payload = record.value();
        String eventId = eventId(record);

        switch (record.topic()) {
            case "user.followed" -> {
                UserFollowedEvent event = objectMapper.readValue(payload, UserFollowedEvent.class);
                notificationService.processUserFollowed(event.followerId(), event.followingId(), eventId)
                        .block(PROCESSING_TIMEOUT);
            }
            case "post.liked" -> {
                PostLikedEvent event = objectMapper.readValue(payload, PostLikedEvent.class);
                notificationService.processPostLiked(event.userId(), event.authorId(), event.postId(), eventId)
                        .block(PROCESSING_TIMEOUT);
            }
            case "comment.created" -> {
                CommentCreatedEvent event = objectMapper.readValue(payload, CommentCreatedEvent.class);
                notificationService.processCommentCreated(event.authorId(), event.postAuthorId(), event.postId(), eventId)
                        .block(PROCESSING_TIMEOUT);
            }
            case "feed.updated" -> {
                FeedUpdatedEvent event = objectMapper.readValue(payload, FeedUpdatedEvent.class);
                notificationService.processFeedUpdated(event.userId(), event.postId());
            }
            default -> {
            }
        }
    }

    private String eventId(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EVENT_ID_HEADER);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
