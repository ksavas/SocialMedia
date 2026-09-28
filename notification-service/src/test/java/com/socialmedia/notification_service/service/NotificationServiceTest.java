package com.socialmedia.notification_service.service;

import com.socialmedia.notification_service.dto.NotificationResponse;
import com.socialmedia.notification_service.entity.Notification;
import com.socialmedia.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.http.codec.ServerSentEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
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

        StepVerifier.create(service.processUserFollowed("user1", "user2", "e1"))
                .verifyComplete();

        verify(repository).save(argThat(n ->
                "user2".equals(n.getRecipientId()) &&
                "FOLLOWED".equals(n.getType()) &&
                "user1".equals(n.getActorId()) &&
                n.getPostId() == null &&
                "e1".equals(n.getEventId())
        ));
    }

    @Test
    void processPostLiked_savesNotificationForPostAuthor() {
        Notification saved = notification("author1", "POST_LIKED", "liker1", "post1");
        when(repository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.processPostLiked("liker1", "author1", "post1", "e2"))
                .verifyComplete();

        verify(repository).save(argThat(n ->
                "author1".equals(n.getRecipientId()) &&
                "POST_LIKED".equals(n.getType()) &&
                "liker1".equals(n.getActorId()) &&
                "post1".equals(n.getPostId()) &&
                "e2".equals(n.getEventId())
        ));
    }

    @Test
    void processCommentCreated_savesNotificationForPostAuthor() {
        Notification saved = notification("postAuthor", "COMMENT_CREATED", "commenter", "post1");
        when(repository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(service.processCommentCreated("commenter", "postAuthor", "post1", "e3"))
                .verifyComplete();

        verify(repository).save(argThat(n ->
                "postAuthor".equals(n.getRecipientId()) &&
                "COMMENT_CREATED".equals(n.getType()) &&
                "commenter".equals(n.getActorId()) &&
                "post1".equals(n.getPostId()) &&
                "e3".equals(n.getEventId())
        ));
    }

    @Test
    void processUserFollowed_ignoresDuplicateEventAndDoesNotPush() {
        when(repository.save(any())).thenReturn(Mono.error(new DuplicateKeyException("duplicate event_id")));
        List<NotificationResponse> received = new CopyOnWriteArrayList<>();
        Disposable subscription = service.streamNotifications("user2").subscribe(sse -> received.add(sse.data()));

        StepVerifier.create(service.processUserFollowed("user1", "user2", "e1"))
                .verifyComplete();

        assertThat(received).isEmpty();
        subscription.dispose();
    }

    @Test
    void processUserFollowed_propagatesNonDuplicateFailures() {
        when(repository.save(any())).thenReturn(Mono.error(new IllegalStateException("database down")));

        StepVerifier.create(service.processUserFollowed("user1", "user2", "e1"))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void getNotifications_returnsMappedResponses() {
        Notification n = notification("user1", "FOLLOWED", "user2", null);
        when(repository.findByRecipientIdOrderByCreatedAtDesc("user1", PageRequest.of(2, 10))).thenReturn(Flux.just(n));

        StepVerifier.create(service.getNotifications("user1", 2, 10))
                .expectNextMatches(r ->
                        "FOLLOWED".equals(r.type()) &&
                        "user2".equals(r.actorId()) &&
                        "user1".equals(r.recipientId())
                )
                .verifyComplete();
    }

    @Test
    void getNotifications_rejectsInvalidPagination() {
        StepVerifier.create(service.getNotifications("user1", 0, 101))
                .expectError(ResponseStatusException.class)
                .verify();
        verify(repository, never()).findByRecipientIdOrderByCreatedAtDesc(anyString(), any());
    }

    @Test
    void countUnread_delegatesToRepository() {
        when(repository.countByRecipientIdAndReadFalse("user1")).thenReturn(Mono.just(7L));

        StepVerifier.create(service.countUnread("user1"))
                .expectNext(7L)
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
    void processFeedUpdated_pushesEphemeralSseWithoutDbWrite() {
        Flux<ServerSentEvent<NotificationResponse>> stream = service.streamNotifications("user1");

        StepVerifier.create(stream)
                .then(() -> service.processFeedUpdated("user1", "post-abc"))
                .expectNextMatches(sse ->
                        "NEW_POST".equals(sse.data().type()) &&
                        "post-abc".equals(sse.data().postId()) &&
                        sse.data().id() == null
                )
                .thenCancel()
                .verify();

        verify(repository, never()).save(any());
    }

    @Test
    void streamNotifications_emitsWhenNotificationPushed() {
        NotificationResponse notification = response("user1");

        Flux<ServerSentEvent<NotificationResponse>> stream = service.streamNotifications("user1");

        StepVerifier.create(stream)
                .then(() -> service.pushNotification("user1", notification))
                .expectNextMatches(sse -> notification.equals(sse.data()))
                .thenCancel()
                .verify();
    }

    @Test
    void streamNotifications_keepsDeliveringToOtherTabs_whenOneTabDisconnects() {
        NotificationResponse notification = response("user1");
        List<NotificationResponse> secondTab = new CopyOnWriteArrayList<>();
        Disposable firstTabSubscription = service.streamNotifications("user1").subscribe();
        Disposable secondTabSubscription = service.streamNotifications("user1").subscribe(sse -> secondTab.add(sse.data()));

        firstTabSubscription.dispose();
        service.pushNotification("user1", notification);

        assertThat(secondTab).containsExactly(notification);
        secondTabSubscription.dispose();
    }

    @Test
    void streamNotifications_deliversToNewConnection_afterAllTabsDisconnected() {
        NotificationResponse notification = response("user1");
        service.streamNotifications("user1").subscribe().dispose();

        StepVerifier.create(service.streamNotifications("user1"))
                .then(() -> service.pushNotification("user1", notification))
                .expectNextMatches(sse -> notification.equals(sse.data()))
                .thenCancel()
                .verify(Duration.ofSeconds(5));
    }

    private NotificationResponse response(String recipientId) {
        return new NotificationResponse(
                UUID.randomUUID(), recipientId, "FOLLOWED", "user2", null, false, Instant.now()
        );
    }

    private Notification notification(String recipientId, String type, String actorId, String postId) {
        return Notification.builder()
                .id(UUID.randomUUID())
                .recipientId(recipientId)
                .type(type)
                .actorId(actorId)
                .postId(postId)
                .read(false)
                .createdAt(Instant.now())
                .build();
    }
}
