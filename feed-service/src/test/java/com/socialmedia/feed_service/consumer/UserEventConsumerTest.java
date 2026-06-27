package com.socialmedia.feed_service.consumer;

import com.socialmedia.feed_service.service.FeedService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventConsumerTest {

    @Mock
    private FeedService feedService;

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
}
