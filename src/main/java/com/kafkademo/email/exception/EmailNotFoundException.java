package com.kafkademo.email.exception;

import java.util.UUID;

public class EmailNotFoundException extends RuntimeException {

    public EmailNotFoundException(UUID id) {
        super("Email " + id + " not found");
    }
}
