package com.kafkademo.email.service;

import com.kafkademo.email.config.EmailProperties;
import com.kafkademo.email.dto.event.CustomerCreatedEvent;
import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.response.EmailResponse;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.publisher.EmailEventPublisher;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Action service: reacts to customer events, and changes data only through {@link EmailService}. */
@Slf4j
@RequiredArgsConstructor
@Service
public class WelcomeEmailService {

    private final JsonMapper jsonMapper;
    private final EmailService emailService;
    private final EmailSender emailSender;
    private final EmailEventPublisher eventPublisher;
    private final EmailProperties properties;

    /**
     * Sends the welcome email for a new customer. Kafka delivers at-least-once, so an event that was
     * already handled is skipped. Malformed JSON throws, so the error handler can dead-letter it.
     *
     * @return the email sent, or empty if the event was ignored or already processed
     */
    public Optional<EmailResponse> handleCustomerEvent(String payload) {
        JsonNode node = jsonMapper.readTree(payload);
        String eventType = node.path("eventType").asString();
        if (!CustomerCreatedEvent.TYPE.equals(eventType)) {
            log.debug("Ignoring {} event", eventType);
            return Optional.empty();
        }
        CustomerCreatedEvent event = jsonMapper.treeToValue(node, CustomerCreatedEvent.class);
        if (emailService.existsBySourceEventId(event.eventId())) {
            log.info("Event {} already processed, skipping", event.eventId());
            return Optional.empty();
        }
        EmailRequest request = EmailRequest.builder()
                .sourceEventId(event.eventId())
                .customerId(event.customerId())
                .from(properties.email().from())
                .to(event.email())
                .subject("Welcome to Kafka Demo, " + event.firstName() + "!")
                .body("Hi " + event.firstName() + " " + event.lastName() + ",\n\nThanks for signing up.")
                .status(EmailStatus.SENT)
                .build();
        emailSender.send(request);
        EmailResponse saved = emailService.create(request);
        eventPublisher.publishEmailSent(saved);
        return Optional.of(saved);
    }
}
