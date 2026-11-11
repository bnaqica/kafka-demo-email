package com.kafkademo.email.dto.response;

import com.kafkademo.email.entity.IncomingEventStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record IncomingEventResponse(
        UUID eventId,
        String eventType,
        UUID customerId,
        String payload,
        IncomingEventStatus status,
        Instant receivedAt,
        Instant processedAt) {}
