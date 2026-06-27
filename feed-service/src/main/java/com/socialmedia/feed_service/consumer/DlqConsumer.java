package com.socialmedia.feed_service.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqConsumer {

    @KafkaListener(topics = {"post.created.DLQ", "post.deleted.DLQ", "user.followed.DLQ", "user.unfollowed.DLQ"}, groupId = "feed-service-dlq", containerFactory = "dlqKafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, String> record) {
        log.error("DLQ event received — topic: {}, key: {}, payload: {}", record.topic(), record.key(), record.value());
    }
}
