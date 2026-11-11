package com.kafkademo.email.exception;

import java.util.UUID;

public class IncomingEventNotFoundException extends RuntimeException {

    public IncomingEventNotFoundException(UUID eventId) {
        super("Incoming event " + eventId + " not found");
    }
}
