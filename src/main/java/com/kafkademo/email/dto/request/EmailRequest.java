package com.kafkademo.email.dto.request;

import com.kafkademo.email.entity.EmailStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

/**
 * Body of the create and update email APIs.
 *
 * @param sourceEventId the event that triggered the email; generated on create if absent, and ignored on update
 */
@Builder
public record EmailRequest(
        UUID sourceEventId,
        @NotNull UUID customerId,
        @NotBlank @Email String from,
        @NotBlank @Email String to,
        @NotBlank String subject,
        @NotBlank String body,
        @NotNull EmailStatus status) {}
