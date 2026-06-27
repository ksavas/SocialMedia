package com.socialmedia.feed_service.consumer;

import com.socialmedia.feed_service.service.FeedService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostEventConsumerTest {

    @Mock
    private FeedService feedService;

    @InjectMocks
    private PostEventConsumer consumer;

    @Test
    void listen_postCreated_callsHandlePostCreated() throws Exception {
        String payload = """
                {"postId":"p1","authorId":"a1"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post.created", 0, 0L, "key", payload);

        consumer.listen(record);

        verify(feedService).handlePostCreated(eq("p1"), eq("a1"), anyLong());
    }

    @Test
    void listen_postDeleted_callsHandlePostDeleted() throws Exception {
        String payload = """
                {"postId":"p1","authorId":"a1"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post.deleted", 0, 0L, "key", payload);

        consumer.listen(record);

        verify(feedService).handlePostDeleted("p1", "a1");
    }

    @Test
    void listen_invalidJson_throwsException() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post.created", 0, 0L, "key", "invalid");

        assertThrows(Exception.class, () -> consumer.listen(record));
    }
}
