package com.socialmedia.notification_service.consumer;

import com.socialmedia.notification_service.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void listen_userFollowed_passesEventIdAndWaitsForSave() {
        when(notificationService.processUserFollowed("f1", "f2", "e1")).thenReturn(Mono.empty());

        consumer.listen(record("user.followed", "{\"followerId\":\"f1\",\"followingId\":\"f2\"}", "e1"));

        verify(notificationService).processUserFollowed("f1", "f2", "e1");
    }

    @Test
    void listen_postLiked_createsNotificationForAuthor() {
        when(notificationService.processPostLiked("u1", "a1", "p1", "e2")).thenReturn(Mono.empty());

        consumer.listen(record("post.liked", "{\"postId\":\"p1\",\"userId\":\"u1\",\"authorId\":\"a1\"}", "e2"));

        verify(notificationService).processPostLiked("u1", "a1", "p1", "e2");
    }

    @Test
    void listen_commentCreated_createsNotificationForPostAuthor() {
        when(notificationService.processCommentCreated("u1", "a1", "p1", "e3")).thenReturn(Mono.empty());

        consumer.listen(record("comment.created",
                "{\"commentId\":\"c1\",\"postId\":\"p1\",\"authorId\":\"u1\",\"postAuthorId\":\"a1\"}", "e3"));

        verify(notificationService).processCommentCreated("u1", "a1", "p1", "e3");
    }

    @Test
    void listen_feedUpdated_pushesNewPost() {
        consumer.listen(new ConsumerRecord<>("feed.updated", 0, 0L, "u1", "{\"userId\":\"u1\",\"postId\":\"p1\"}"));

        verify(notificationService).processFeedUpdated("u1", "p1");
    }

    @Test
    void listen_invalidJson_throwsSoErrorHandlerCanRouteToDlq() {
        assertThatThrownBy(() -> consumer.listen(record("user.followed", "not json", "e4")))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(notificationService);
    }

    @Test
    void listen_saveFailure_isPropagatedForRetry() {
        when(notificationService.processUserFollowed("f1", "f2", "e5"))
                .thenReturn(Mono.error(new IllegalStateException("database down")));

        assertThatThrownBy(() -> consumer.listen(record("user.followed", "{\"followerId\":\"f1\",\"followingId\":\"f2\"}", "e5")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database down");
    }

    private ConsumerRecord<String, String> record(String topic, String payload, String eventId) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0L, "key", payload);
        record.headers().add("eventId", eventId.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
