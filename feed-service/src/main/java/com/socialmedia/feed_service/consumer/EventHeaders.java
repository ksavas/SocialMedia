package com.socialmedia.feed_service.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class EventHeaders {

    public static final String EVENT_ID = "eventId";

    private EventHeaders() {
    }

    public static Optional<String> eventId(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(EVENT_ID);
        return Optional.ofNullable(header).map(h -> new String(h.value(), StandardCharsets.UTF_8));
    }
}
