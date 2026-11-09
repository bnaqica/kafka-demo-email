package com.kafkademo.email.dto.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Consumed from {@code customer-events}. Owned by kafka-demo-customer; see docs/kafka-events.md.
 */
public record CustomerCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID customerId,
        String firstName,
        String lastName,
        String email) {

    public static final String TYPE = "CUSTOMER_CREATED";
}
