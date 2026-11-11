package com.kafkademo.email.service;

import com.kafkademo.email.config.EmailProperties;
import com.kafkademo.email.dto.EmailContent;
import com.kafkademo.email.dto.event.CustomerChangeEvent;
import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.request.IncomingEventRequest;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.entity.IncomingEventStatus;
import com.kafkademo.email.exception.MalformedEventException;
import com.kafkademo.email.processors.CustomerEmailComposer;
import com.kafkademo.email.processors.CustomerEventValidator;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Action service: reacts to customer change events, changing data only through the CRUD interfaces. The event is saved
 * before sending, so a failed send is retried, and only an event already {@code PROCESSED} is skipped.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CustomerChangeEmailService {

    private final JsonMapper jsonMapper;
    private final IncomingEventService incomingEvents;
    private final EmailService emails;
    private final EmailSender emailSender;
    private final CustomerEventValidator validator;
    private final CustomerEmailComposer composer;
    private final EmailProperties properties;

    /**
     * Malformed JSON and missing fields throw exceptions that aren't retried, so the error handler dead-letters them.
     */
    public void handle(String payload) {
        CustomerChangeEvent event = jsonMapper.readValue(payload, CustomerChangeEvent.class);
        if (event == null || event.eventType() == null) {
            throw new MalformedEventException("Event has no eventType");
        }
        if (!validator.handles(event.eventType())) {
            log.debug("Ignoring {} event", event.eventType());
            return;
        }
        validator.validate(event);
        EmailContent content = composer.compose(event).orElseThrow();

        Optional<IncomingEventStatus> status =
                incomingEvents.find(event.eventId()).map(existing -> existing.status());
        if (status.filter(IncomingEventStatus.PROCESSED::equals).isPresent()) {
            log.info("Event {} already processed, skipping", event.eventId());
            return;
        }
        if (status.isEmpty()) {
            incomingEvents.create(incomingEvent(event, payload, IncomingEventStatus.RECEIVED));
        }
        if (!emails.existsBySourceEventId(event.eventId())) {
            EmailRequest email = toEmail(event, content);
            emailSender.send(email);
            emails.create(email);
        }
        incomingEvents.update(event.eventId(), incomingEvent(event, payload, IncomingEventStatus.PROCESSED));
    }

    private static IncomingEventRequest incomingEvent(
            CustomerChangeEvent event, String payload, IncomingEventStatus status) {
        return new IncomingEventRequest(event.eventId(), event.eventType(), event.customerId(), payload, status);
    }

    private EmailRequest toEmail(CustomerChangeEvent event, EmailContent content) {
        return EmailRequest.builder()
                .sourceEventId(event.eventId())
                .customerId(event.customerId())
                .from(properties.email().from())
                .to(event.email())
                .subject(content.subject())
                .body(content.body())
                .status(EmailStatus.SENT)
                .lastName(event.lastName())
                .state(event.state())
                .eventType(event.eventType())
                .build();
    }
}
