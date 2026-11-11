package com.kafkademo.email.dto.request;

import com.kafkademo.email.entity.IncomingEventStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record IncomingEventRequest(
        @NotNull UUID eventId,
        @NotBlank String eventType,
        @NotNull UUID customerId,
        @NotBlank String payload,
        @NotNull IncomingEventStatus status) {}
