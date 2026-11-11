package com.kafkademo.email.dto.event;

import java.time.Instant;
import java.util.UUID;

/** Payload consumed from the {@code customer-change-event} topic. See docs/kafka-events.md for the contract. */
public record CustomerChangeEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        UUID customerId,
        String firstName,
        String lastName,
        String email,
        String state,
        CustomerChanges changes) {}
