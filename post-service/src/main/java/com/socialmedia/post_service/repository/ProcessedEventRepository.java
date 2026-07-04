package com.socialmedia.post_service.repository;

import com.socialmedia.post_service.document.ProcessedEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedEventRepository extends MongoRepository<ProcessedEvent, String> {
}
