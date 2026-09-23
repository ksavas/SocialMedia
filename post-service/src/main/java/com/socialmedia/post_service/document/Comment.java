package com.socialmedia.post_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "comments")
@CompoundIndex(name = "post_created_idx", def = "{'postId': 1, 'createdAt': 1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Comment {
    @Id
    private String id;
    private String postId;
    private String authorId;
    private String authorUsername;
    private String content;
    private Instant createdAt;
}
