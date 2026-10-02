package com.socialmedia.feed_service.service;

public class EventInProgressException extends RuntimeException {

    public EventInProgressException(String eventId) {
        super("Event " + eventId + " is being processed by another consumer");
    }
}
