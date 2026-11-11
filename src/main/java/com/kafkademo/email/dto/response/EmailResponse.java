package com.kafkademo.email.dto.response;

import com.kafkademo.email.entity.EmailStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record EmailResponse(
        UUID id,
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
