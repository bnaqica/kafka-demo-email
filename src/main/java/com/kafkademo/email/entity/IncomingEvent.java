package com.kafkademo.email.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A customer change event as it arrived. The {@code eventId} is the document id, so one event has one record.
 *
 * @param payload the event exactly as received
 */
@Builder
@Document("incoming_event")
public record IncomingEvent(
        @Id UUID eventId,
        String eventType,
        UUID customerId,
        String payload,
        IncomingEventStatus status,
        Instant receivedAt,
        Instant processedAt) {}
