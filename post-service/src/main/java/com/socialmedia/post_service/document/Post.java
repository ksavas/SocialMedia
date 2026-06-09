package com.socialmedia.post_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "posts")
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
    private LocalDateTime createdAt;
}
