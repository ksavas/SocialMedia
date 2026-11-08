package com.socialmedia.post_service.consumer;

import com.socialmedia.post_service.document.ProcessedEvent;
import com.socialmedia.post_service.repository.PostRepository;
import com.socialmedia.post_service.repository.ProcessedEventRepository;
import com.socialmedia.post_service.transaction.TransactionRunner;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostCounterConsumerTest {

    @Mock PostRepository postRepository;
    @Mock ProcessedEventRepository processedEventRepository;

    PostCounterConsumer consumer;

    @BeforeEach
    void setUp() {
        TransactionRunner directRunner = new TransactionRunner() {
            @Override
            public <T> T inTransaction(Supplier<T> work) {
                return work.get();
            }
        };
        consumer = new PostCounterConsumer(postRepository, processedEventRepository, directRunner);
    }

    @Test
    void postLiked_incrementsLikeCount() {
        consumer.listen(record("post.liked", "{\"postId\":\"p1\",\"userId\":\"u1\",\"authorId\":\"a1\"}", "e1"));

        verify(postRepository).incrementLikeCount("p1");
        verify(processedEventRepository).insert(argThat((ProcessedEvent e) -> e.getId().equals("e1")));
    }

    @Test
    void commentCreated_incrementsCommentCount() {
        consumer.listen(record("comment.created",
                "{\"commentId\":\"c1\",\"postId\":\"p1\",\"authorId\":\"u1\",\"postAuthorId\":\"a1\"}", "e3"));

        verify(postRepository).incrementCommentCount("p1");
    }

    @Test
    void duplicateEvent_isNotCountedTwice() {
        when(processedEventRepository.insert(any(ProcessedEvent.class))).thenThrow(new DuplicateKeyException("duplicate"));

        consumer.listen(record("post.liked", "{\"postId\":\"p1\",\"userId\":\"u1\",\"authorId\":\"a1\"}", "e1"));

        verify(postRepository, never()).incrementLikeCount(anyString());
    }

    @Test
    void malformedPayload_isRethrownForTheErrorHandler() {
        assertThatThrownBy(() -> consumer.listen(record("post.liked", "not json", "e9")))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(postRepository);
    }

    private ConsumerRecord<String, String> record(String topic, String payload, String eventId) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(topic, 0, 0L, "p1", payload);
        record.headers().add("eventId", eventId.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
