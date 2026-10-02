package com.socialmedia.feed_service.consumer;

import com.socialmedia.feed_service.service.FeedService;
import com.socialmedia.feed_service.service.ProcessedEventStore;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostEventConsumerTest {

    @Mock
    private FeedService feedService;

    @Mock
    private ProcessedEventStore processedEventStore;

    @InjectMocks
    private PostEventConsumer consumer;

    @Test
    void listen_postCreated_callsHandlePostCreated() throws Exception {
        String payload = """
                {"postId":"p1","authorId":"a1","createdAt":"2026-06-01T10:15:30Z"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post.created", 0, 0L, "key", payload);

        consumer.listen(record);

        verify(feedService).handlePostCreated(eq("p1"), eq("a1"), anyLong());
    }

    @Test
    void listen_postCreated_scoresByPostCreationTimeNotRecordTimestamp() throws Exception {
        String payload = """
                {"postId":"p1","authorId":"a1","createdAt":"2026-06-01T10:15:30Z"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post.created", 0, 0L, "key", payload);

        consumer.listen(record);

        verify(feedService).handlePostCreated("p1", "a1", Instant.parse("2026-06-01T10:15:30Z").toEpochMilli());
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

    @Test
    void listen_skipsEventThatWasAlreadyProcessed() throws Exception {
        ConsumerRecord<String, String> record = recordWithEventId("post.created",
                "{\"postId\":\"p1\",\"authorId\":\"a1\",\"createdAt\":\"2026-06-01T10:15:30Z\"}", "e1");
        when(processedEventStore.tryStart("e1")).thenReturn(false);

        consumer.listen(record);

        verifyNoInteractions(feedService);
        verify(processedEventStore, never()).markProcessed(anyString());
    }

    @Test
    void listen_marksEventProcessedAfterSuccessfulHandling() throws Exception {
        ConsumerRecord<String, String> record = recordWithEventId("post.deleted",
                "{\"postId\":\"p1\",\"authorId\":\"a1\"}", "e2");
        when(processedEventStore.tryStart("e2")).thenReturn(true);

        consumer.listen(record);

        verify(feedService).handlePostDeleted("p1", "a1");
        verify(processedEventStore).markProcessed("e2");
    }

    @Test
    void listen_doesNotMarkEventProcessed_whenHandlingFails() {
        ConsumerRecord<String, String> record = recordWithEventId("post.deleted",
                "{\"postId\":\"p1\",\"authorId\":\"a1\"}", "e3");
        when(processedEventStore.tryStart("e3")).thenReturn(true);
        doThrow(new IllegalStateException("redis down")).when(feedService).handlePostDeleted("p1", "a1");

        assertThrows(IllegalStateException.class, () -> consumer.listen(record));

        verify(processedEventStore, never()).markProcessed(anyString());
        verify(processedEventStore).release("e3");
    }

    private ConsumerRecord<String, String> recordWithEventId(String topic, String payload, String eventId) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0L, "key", payload);
        record.headers().add(EventHeaders.EVENT_ID, eventId.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
