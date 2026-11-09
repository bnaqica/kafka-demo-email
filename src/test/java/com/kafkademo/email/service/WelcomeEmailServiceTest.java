package com.kafkademo.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kafkademo.email.config.EmailProperties;
import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.response.EmailResponse;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.publisher.EmailEventPublisher;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class WelcomeEmailServiceTest {

    private static final UUID EVENT_ID = UUID.fromString("0e15a972-ae98-429a-a35f-0b61251f0481");
    private static final String CUSTOMER_CREATED = """
            {"eventId":"0e15a972-ae98-429a-a35f-0b61251f0481","eventType":"CUSTOMER_CREATED",
             "occurredAt":"2026-11-07T18:04:18.720578Z","customerId":"1c02b30d-233c-44ea-a4bf-aa491c81dee9",
             "firstName":"Ada","lastName":"Lovelace","email":"ada@example.com"}
            """;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailSender emailSender;

    @Mock
    private EmailEventPublisher eventPublisher;

    private WelcomeEmailService service;

    @BeforeEach
    void setUp() {
        EmailProperties properties = new EmailProperties(
                new EmailProperties.Kafka(new EmailProperties.Topics("customer-events", "email-events")),
                new EmailProperties.Email("no-reply@test"));
        service = new WelcomeEmailService(
                JsonMapper.builder().build(), emailService, emailSender, eventPublisher, properties);
    }

    @Test
    void sendsSavesAndPublishesWelcomeEmail() {
        EmailResponse saved = EmailResponse.builder()
                .id(UUID.randomUUID())
                .sentAt(Instant.now())
                .build();
        when(emailService.existsBySourceEventId(EVENT_ID)).thenReturn(false);
        when(emailService.create(any())).thenReturn(saved);

        Optional<EmailResponse> result = service.handleCustomerEvent(CUSTOMER_CREATED);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailSender).send(captor.capture());
        EmailRequest request = captor.getValue();
        assertThat(request.sourceEventId()).isEqualTo(EVENT_ID);
        assertThat(request.to()).isEqualTo("ada@example.com");
        assertThat(request.from()).isEqualTo("no-reply@test");
        assertThat(request.subject()).contains("Ada");
        assertThat(request.status()).isEqualTo(EmailStatus.SENT);
        verify(emailService).create(request);
        verify(eventPublisher).publishEmailSent(saved);
        assertThat(result).contains(saved);
    }

    @Test
    void skipsEventThatWasAlreadyProcessed() {
        when(emailService.existsBySourceEventId(EVENT_ID)).thenReturn(true);

        assertThat(service.handleCustomerEvent(CUSTOMER_CREATED)).isEmpty();
        verifyNoInteractions(emailSender, eventPublisher);
        verify(emailService, never()).create(any());
    }

    @Test
    void otherEventTypesAreIgnored() {
        assertThat(service.handleCustomerEvent("{\"eventType\":\"CUSTOMER_DELETED\"}"))
                .isEmpty();
        verifyNoInteractions(emailService, emailSender, eventPublisher);
    }

    @Test
    void malformedJsonThrowsSoTheErrorHandlerCanDeadLetterIt() {
        assertThatThrownBy(() -> service.handleCustomerEvent("not json")).isInstanceOf(JacksonException.class);
    }
}
