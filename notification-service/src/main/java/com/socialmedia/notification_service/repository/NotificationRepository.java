package com.socialmedia.notification_service.repository;

import com.socialmedia.notification_service.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface NotificationRepository extends ReactiveCrudRepository<Notification, UUID> {

    Flux<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    Mono<Long> countByRecipientIdAndReadFalse(String recipientId);

    @Query("UPDATE notifications SET is_read = true WHERE id = :id AND recipient_id = :recipientId")
    Mono<Integer> markAsRead(@Param("id") UUID id, @Param("recipientId") String recipientId);

    @Query("UPDATE notifications SET is_read = true WHERE recipient_id = :recipientId AND is_read = false")
    Mono<Integer> markAllAsRead(@Param("recipientId") String recipientId);
}
