package com.socialmedia.post_service.transaction;

import com.mongodb.MongoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RetryingMongoTransactionRunnerTest {

    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private RetryingMongoTransactionRunner runner;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        runner = new RetryingMongoTransactionRunner(transactionManager, 3, Duration.ofMillis(1));
    }

    @Test
    void inTransaction_returnsResultOfWork() {
        String result = runner.inTransaction(() -> "done");

        assertThat(result).isEqualTo("done");
        verify(transactionManager).commit(any());
    }

    @Test
    void inTransaction_retriesTransientTransactionErrorsUntilSuccess() {
        AtomicInteger attempts = new AtomicInteger();

        String result = runner.inTransaction(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw transientWriteConflict();
            }
            return "done";
        });

        assertThat(result).isEqualTo("done");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void inTransaction_doesNotRetryNonTransientErrors() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> runner.inTransaction(() -> {
            attempts.incrementAndGet();
            throw new DuplicateKeyException("duplicate");
        })).isInstanceOf(DuplicateKeyException.class);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void inTransaction_givesUpAfterMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> runner.inTransaction(() -> {
            attempts.incrementAndGet();
            throw transientWriteConflict();
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(attempts).hasValue(3);
    }

    @Test
    void inTransaction_runsRunnableWork() {
        AtomicInteger calls = new AtomicInteger();

        runner.runInTransaction(calls::incrementAndGet);

        assertThat(calls).hasValue(1);
    }

    private DataIntegrityViolationException transientWriteConflict() {
        MongoException cause = new MongoException(112, "WriteConflict");
        cause.addLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL);
        return new DataIntegrityViolationException("write conflict", cause);
    }
}
