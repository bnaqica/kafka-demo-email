package com.kafkademo.email.entity;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An email this service has sent. Collection and indexes are created by Flyway (db/migration).
 *
 * @param sourceEventId the Kafka event that triggered the email; unique, so redelivered events are ignored
 * @param lastName copied from the event, for filtering
 * @param state copied from the event, for filtering
 */
@Builder
@Document("email")
public record EmailMessage(
        @Id UUID id,
        UUID sourceEventId,
        UUID customerId,
        String from,
        String to,
        String subject,
        String body,
        EmailStatus status,
        Instant sentAt,
        String lastName,
        String state,
        String eventType) {}
