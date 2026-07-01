package com.socialmedia.post_service.transaction;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T inTransaction(Supplier<T> work);

    default void runInTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
