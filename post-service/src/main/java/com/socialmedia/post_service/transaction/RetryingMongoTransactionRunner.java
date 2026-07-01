package com.socialmedia.post_service.transaction;

import com.mongodb.MongoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
public class RetryingMongoTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;
    private final int maxAttempts;
    private final Duration baseBackoff;

    public RetryingMongoTransactionRunner(PlatformTransactionManager transactionManager, int maxAttempts, Duration baseBackoff) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maxAttempts = maxAttempts;
        this.baseBackoff = baseBackoff;
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transactionTemplate.execute(status -> work.get());
            } catch (RuntimeException e) {
                if (attempt >= maxAttempts || !isTransient(e)) {
                    throw e;
                }
                log.debug("Transient transaction error on attempt {}, retrying: {}", attempt, e.getMessage());
                backOff(attempt);
            }
        }
    }

    private boolean isTransient(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof MongoException mongoException
                    && mongoException.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                return true;
            }
        }
        return false;
    }

    private void backOff(int attempt) {
        long maxDelayMillis = baseBackoff.toMillis() << (attempt - 1);
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(maxDelayMillis + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying transaction", e);
        }
    }
}
