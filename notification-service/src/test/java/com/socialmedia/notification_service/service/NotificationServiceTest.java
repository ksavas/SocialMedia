package com.socialmedia.notification_service.service;

import com.socialmedia.notification_service.dto.NotificationResponse;
import com.socialmedia.notification_service.entity.Notification;
import com.socialmedia.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.http.codec.ServerSentEvent;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @InjectMocks
    private NotificationService service;

    @Test
    void processUserFollowed_savesNotificationForFollowingUser() {
        Notification saved = notification("user2", "FOLLOWED", "user1", null);
        when(repository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.processUserFollowed("user1", "user2"))
                .verifyComplete();

        verify(repository).save(argThat(n ->
                "user2".equals(n.getRecipientId()) &&
                "FOLLOWED".equals(n.getType()) &&
                "user1".equals(n.getActorId()) &&
                n.getPostId() == null
        ));
    }

    @Test
    void processPostLiked_savesNotificationForPostAuthor() {
        Notification saved = notification("author1", "POST_LIKED", "liker1", "post1");
        when(repository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.processPostLiked("liker1", "author1", "post1"))
                .verifyComplete();

        verify(repository).save(argThat(n ->
                "author1".equals(n.getRecipientId()) &&
                "POST_LIKED".equals(n.getType()) &&
                "liker1".equals(n.getActorId()) &&
                "post1".equals(n.getPostId())
        ));
    }

    @Test
    void processCommentCreated_savesNotificationForPostAuthor() {
        Notification saved = notification("postAuthor", "COMMENT_CREATED", "commenter", "post1");
        when(repository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.processCommentCreated("commenter", "postAuthor", "post1"))
                .verifyComplete();

        verify(repository).save(argThat(n ->
                "postAuthor".equals(n.getRecipientId()) &&
                "COMMENT_CREATED".equals(n.getType()) &&
                "commenter".equals(n.getActorId()) &&
                "post1".equals(n.getPostId())
        ));
    }

    @Test
    void getNotifications_returnsMappedResponses() {
        Notification n = notification("user1", "FOLLOWED", "user2", null);
        when(repository.findByRecipientIdOrderByCreatedAtDesc("user1")).thenReturn(Flux.just(n));

        StepVerifier.create(service.getNotifications("user1"))
                .expectNextMatches(r ->
                        "FOLLOWED".equals(r.type()) &&
                        "user2".equals(r.actorId()) &&
                        "user1".equals(r.recipientId())
                )
                .verifyComplete();
    }

    @Test
    void markAsRead_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        when(repository.markAsRead(id, "user1")).thenReturn(Mono.just(1));

        StepVerifier.create(service.markAsRead(id, "user1"))
                .verifyComplete();

        verify(repository).markAsRead(id, "user1");
    }

    @Test
    void streamNotifications_emitsWhenNotificationPushed() {
        NotificationResponse notification = new NotificationResponse(
                UUID.randomUUID(), "user1", "FOLLOWED", "user2", null, false, LocalDateTime.now()
        );

        Flux<ServerSentEvent<NotificationResponse>> stream = service.streamNotifications("user1");

        StepVerifier.create(stream)
                .then(() -> service.pushNotification("user1", notification))
                .expectNextMatches(sse -> notification.equals(sse.data()))
                .thenCancel()
                .verify();
    }

    private Notification notification(String recipientId, String type, String actorId, String postId) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(recipientId)
                .type(type)
                .actorId(actorId)
                .postId(postId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
