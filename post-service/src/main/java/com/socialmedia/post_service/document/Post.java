package com.socialmedia.post_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "posts")
@CompoundIndex(name = "author_created_idx", def = "{'authorId': 1, 'createdAt': -1}")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    @Id
    private String id;
    private String authorId;
    private String authorUsername;
    private String content;
    @Builder.Default
    private long likeCount = 0;
    @Builder.Default
    private long commentCount = 0;
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant createdAt;
}
