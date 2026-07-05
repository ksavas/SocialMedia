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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private FeedService feedService;

    @Mock
    private ProcessedEventStore processedEventStore;

    @InjectMocks
    private UserEventConsumer consumer;

    @Test
    void listen_userFollowed_callsHandleUserFollowed() throws Exception {
        String payload = """
                {"followerId":"f1","followingId":"f2"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("user.followed", 0, 0L, "key", payload);

        consumer.listen(record);

        verify(feedService).handleUserFollowed("f1", "f2");
    }

    @Test
    void listen_userUnfollowed_callsHandleUserUnfollowed() throws Exception {
        String payload = """
                {"followerId":"f1","followingId":"f2"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("user.unfollowed", 0, 0L, "key", payload);

        consumer.listen(record);

        verify(feedService).handleUserUnfollowed("f1", "f2");
    }

    @Test
    void listen_invalidJson_throwsException() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("user.followed", 0, 0L, "key", "bad");

        assertThrows(Exception.class, () -> consumer.listen(record));
    }

    @Test
    void listen_skipsEventThatWasAlreadyProcessed() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("user.followed", 0, 0L, "key",
                "{\"followerId\":\"f1\",\"followingId\":\"f2\"}");
        record.headers().add(EventHeaders.EVENT_ID, "e1".getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.isProcessed("e1")).thenReturn(true);

        consumer.listen(record);

        verifyNoInteractions(feedService);
    }

    @Test
    void listen_marksEventProcessedAfterSuccessfulHandling() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("user.unfollowed", 0, 0L, "key",
                "{\"followerId\":\"f1\",\"followingId\":\"f2\"}");
        record.headers().add(EventHeaders.EVENT_ID, "e2".getBytes(StandardCharsets.UTF_8));
        when(processedEventStore.isProcessed("e2")).thenReturn(false);

        consumer.listen(record);

        verify(feedService).handleUserUnfollowed("f1", "f2");
        verify(processedEventStore).markProcessed("e2");
    }
}
