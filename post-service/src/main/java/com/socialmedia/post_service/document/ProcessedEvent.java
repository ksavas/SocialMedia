package com.socialmedia.post_service.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "processed_events")
@Getter @NoArgsConstructor @AllArgsConstructor
public class ProcessedEvent {
    @Id
    private String id;

    @Indexed(expireAfter = "7d")
    private Instant processedAt;
}
