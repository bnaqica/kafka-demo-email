package com.kafkademo.email.exception;

import java.util.UUID;

public class DuplicateIncomingEventException extends RuntimeException {

    public DuplicateIncomingEventException(UUID eventId) {
        super("Incoming event " + eventId + " already exists");
    }
}
