package com.socialmedia.notification_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("notifications")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id
    private UUID id;

    @Column("recipient_id")
    private String recipientId;

    private String type;

    @Column("actor_id")
    private String actorId;

    @Column("post_id")
    private String postId;

    @Column("is_read")
    private boolean read;

    @Column("created_at")
    private Instant createdAt;

    @Column("event_id")
    private String eventId;
}
