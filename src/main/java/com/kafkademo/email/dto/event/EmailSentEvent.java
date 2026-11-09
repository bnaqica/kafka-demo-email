package com.kafkademo.email.dto.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to {@code email-events}. See docs/kafka-events.md for the contract.
 */
public record EmailSentEvent(
        UUID eventId, String eventType, Instant occurredAt, UUID emailId, UUID customerId, String to, String subject) {

    public static final String TYPE = "EMAIL_SENT";
}
