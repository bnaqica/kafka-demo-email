package com.kafkademo.email.exception;

/** An event that can never be processed, so it goes to the dead-letter topic without retries. */
public class MalformedEventException extends RuntimeException {

    public MalformedEventException(String message) {
        super(message);
    }
}
