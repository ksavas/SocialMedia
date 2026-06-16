package com.socialmedia.notification_service.consumer;

import com.socialmedia.notification_service.dto.CommentCreatedEvent;
import com.socialmedia.notification_service.dto.PostLikedEvent;
import com.socialmedia.notification_service.dto.UserFollowedEvent;
import com.socialmedia.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = {"user.followed", "post.liked", "comment.created"}, groupId = "notification-service")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            String topic = record.topic();
            String payload = record.value();
            switch (topic) {
                case "user.followed" -> {
                    UserFollowedEvent event = objectMapper.readValue(payload, UserFollowedEvent.class);
                    notificationService.processUserFollowed(event.followerId(), event.followingId()).subscribe();
                }
                case "post.liked" -> {
                    PostLikedEvent event = objectMapper.readValue(payload, PostLikedEvent.class);
                    notificationService.processPostLiked(event.userId(), event.authorId(), event.postId()).subscribe();
                }
                case "comment.created" -> {
                    CommentCreatedEvent event = objectMapper.readValue(payload, CommentCreatedEvent.class);
                    notificationService.processCommentCreated(event.authorId(), event.postAuthorId(), event.postId()).subscribe();
                }
            }
        } catch (Exception ignored) {}
    }
}
