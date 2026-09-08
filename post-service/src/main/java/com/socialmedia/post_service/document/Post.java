package com.socialmedia.post_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "posts")
@CompoundIndexes({
        @CompoundIndex(name = "author_created_idx", def = "{'authorId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "pull_feed_idx", def = "{'fanoutOnRead': 1, 'authorId': 1, 'createdAt': -1, '_id': -1}")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    @Id
    private String id;
    private String authorId;
    private String authorUsername;
    private String content;
    private boolean fanoutOnRead;
    @Builder.Default
    private long likeCount = 0;
    @Builder.Default
    private long commentCount = 0;
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant createdAt;
}
