package com.socialmedia.notification_service.service;

import com.socialmedia.notification_service.dto.NotificationResponse;
import com.socialmedia.notification_service.entity.Notification;
import com.socialmedia.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final Map<String, Sinks.Many<NotificationResponse>> userSinks = new ConcurrentHashMap<>();

    public Mono<Void> processUserFollowed(String followerId, String followingId, String eventId) {
        return create(followingId, "FOLLOWED", followerId, null, eventId);
    }

    public Mono<Void> processPostLiked(String actorId, String authorId, String postId, String eventId) {
        return create(authorId, "POST_LIKED", actorId, postId, eventId);
    }

    public Mono<Void> processCommentCreated(String actorId, String postAuthorId, String postId, String eventId) {
        return create(postAuthorId, "COMMENT_CREATED", actorId, postId, eventId);
    }

    public void processFeedUpdated(String userId, String postId) {
        NotificationResponse notification = new NotificationResponse(
                null, userId, "NEW_POST", null, postId, false, Instant.now()
        );
        pushNotification(userId, notification);
    }

    public Flux<NotificationResponse> getNotifications(String userId) {
        return repository.findByRecipientIdOrderByCreatedAtDesc(userId)
                .map(this::toResponse);
    }

    public Mono<Void> markAsRead(UUID id, String userId) {
        return repository.markAsRead(id, userId).then();
    }

    public Flux<ServerSentEvent<NotificationResponse>> streamNotifications(String userId) {
        Sinks.Many<NotificationResponse> sink = userSinks.computeIfAbsent(
                userId,
                id -> Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false)
        );
        return sink.asFlux()
                .map(n -> ServerSentEvent.builder(n).build())
                .doFinally(signal -> userSinks.computeIfPresent(
                        userId,
                        (id, current) -> current.currentSubscriberCount() == 0 ? null : current
                ));
    }

    public void pushNotification(String userId, NotificationResponse notification) {
        Sinks.Many<NotificationResponse> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(notification);
        }
    }

    private Mono<Void> create(String recipientId, String type, String actorId, String postId, String eventId) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .type(type)
                .actorId(actorId)
                .postId(postId)
                .read(false)
                .createdAt(Instant.now())
                .eventId(eventId)
                .build();

        return repository.save(notification)
                .doOnNext(saved -> pushNotification(recipientId, toResponse(saved)))
                .onErrorResume(DuplicateKeyException.class, duplicate -> Mono.empty())
                .then();
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getRecipientId(), n.getType(),
                n.getActorId(), n.getPostId(), n.isRead(), n.getCreatedAt()
        );
    }
}
