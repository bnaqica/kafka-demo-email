package com.kafkademo.email.service;

import static com.kafkademo.email.EventFixtures.CUSTOMER_ID;
import static com.kafkademo.email.EventFixtures.EVENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kafkademo.email.EventFixtures;
import com.kafkademo.email.config.EmailProperties;
import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.request.IncomingEventRequest;
import com.kafkademo.email.dto.response.IncomingEventResponse;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.entity.IncomingEventStatus;
import com.kafkademo.email.exception.MalformedEventException;
import com.kafkademo.email.processors.CustomerEmailComposer;
import com.kafkademo.email.processors.CustomerEventValidator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomerChangeEmailServiceTest {

    private static final String ADDITION = EventFixtures.json("customer-addition", "{}");

    @Mock
    private IncomingEventService incomingEvents;

    @Mock
    private EmailService emails;

    @Mock
    private EmailSender sender;

    private CustomerChangeEmailService service;

    @BeforeEach
    void setUp() {
        service = new CustomerChangeEmailService(
                JsonMapper.builder().build(),
                incomingEvents,
                emails,
                sender,
                new CustomerEventValidator(),
                new CustomerEmailComposer(),
                new EmailProperties(new EmailProperties.Email("no-reply@test.com")));
    }

    @Test
    void newEventIsSavedThenSentThenMarkedProcessed() {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.empty());

        service.handle(ADDITION);

        InOrder order = inOrder(incomingEvents, sender, emails);
        ArgumentCaptor<IncomingEventRequest> saved = ArgumentCaptor.forClass(IncomingEventRequest.class);
        order.verify(incomingEvents).create(saved.capture());
        order.verify(sender).send(any());
        order.verify(emails).create(any());
        ArgumentCaptor<IncomingEventRequest> updated = ArgumentCaptor.forClass(IncomingEventRequest.class);
        order.verify(incomingEvents).update(eq(EVENT_ID), updated.capture());
        assertThat(saved.getValue().status()).isEqualTo(IncomingEventStatus.RECEIVED);
        assertThat(saved.getValue().payload()).isEqualTo(ADDITION);
        assertThat(saved.getValue().eventType()).isEqualTo("customer-addition");
        assertThat(updated.getValue().status()).isEqualTo(IncomingEventStatus.PROCESSED);
    }

    @Test
    void emailGoesToTheEventsAddressWithItsFilterFields() {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.empty());

        service.handle(ADDITION);

        ArgumentCaptor<EmailRequest> email = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emails).create(email.capture());
        assertThat(email.getValue().to()).isEqualTo("ada@example.com");
        assertThat(email.getValue().from()).isEqualTo("no-reply@test.com");
        assertThat(email.getValue().subject()).contains("Welcome");
        assertThat(email.getValue().sourceEventId()).isEqualTo(EVENT_ID);
        assertThat(email.getValue().customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(email.getValue().lastName()).isEqualTo("Lovelace");
        assertThat(email.getValue().state()).isEqualTo("NY");
        assertThat(email.getValue().eventType()).isEqualTo("customer-addition");
        assertThat(email.getValue().status()).isEqualTo(EmailStatus.SENT);
    }

    @Test
    void processedEventIsSkipped() {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.of(existing(IncomingEventStatus.PROCESSED)));

        service.handle(ADDITION);

        verifyNoInteractions(sender, emails);
        verify(incomingEvents, never()).create(any());
        verify(incomingEvents, never()).update(any(), any());
    }

    @Test
    void unfinishedEventWithNoEmailIsSentAndFinished() {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.of(existing(IncomingEventStatus.RECEIVED)));
        when(emails.existsBySourceEventId(EVENT_ID)).thenReturn(false);

        service.handle(ADDITION);

        verify(incomingEvents, never()).create(any());
        verify(sender).send(any());
        verify(incomingEvents).update(eq(EVENT_ID), any());
    }

    @Test
    void unfinishedEventWithAnEmailIsOnlyFinished() {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.of(existing(IncomingEventStatus.RECEIVED)));
        when(emails.existsBySourceEventId(EVENT_ID)).thenReturn(true);

        service.handle(ADDITION);

        verifyNoInteractions(sender);
        verify(emails, never()).create(any());
        verify(incomingEvents).update(eq(EVENT_ID), any());
    }

    @Test
    void failedSendLeavesTheEventUnfinishedAndPropagates() {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("smtp down")).when(sender).send(any());

        assertThatThrownBy(() -> service.handle(ADDITION)).isInstanceOf(IllegalStateException.class);

        verify(incomingEvents).create(any());
        verify(incomingEvents, never()).update(any(), any());
        verify(emails, never()).create(any());
    }

    @Test
    void unknownTypeIsIgnoredAndNotSaved() {
        service.handle(EventFixtures.json("something-else", "{}"));

        verifyNoInteractions(incomingEvents, emails, sender);
    }

    @Test
    void malformedJsonThrowsSoItIsDeadLettered() {
        assertThatThrownBy(() -> service.handle("not json")).isInstanceOf(JacksonException.class);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "customer-addition|{}",
                "customer-change|{\"fields\":[\"lastName\"]}",
                "customer-address-change|{\"address\":{\"street\":\"9 Elm\",\"apartment\":null,\"city\":\"Reno\",\"state\":\"NV\",\"zip\":\"89501\"}}",
                "customer-phone-add|{\"phone\":{\"type\":\"CELL\",\"number\":\"555-0100\"}}",
                "customer-phone-change|{\"previousPhone\":{\"type\":\"CELL\",\"number\":\"555-0100\"},\"phone\":{\"type\":\"CELL\",\"number\":\"555-0111\"}}",
                "customer-phone-delete|{\"phone\":{\"type\":\"CELL\",\"number\":\"555-0100\"}}",
                "customer-delete|{}"
            })
    void everyEventTypeFromTheContractSendsAnEmail(String type, String changes) {
        when(incomingEvents.find(EVENT_ID)).thenReturn(Optional.empty());

        service.handle(EventFixtures.json(type, changes));

        ArgumentCaptor<EmailRequest> email = ArgumentCaptor.forClass(EmailRequest.class);
        verify(sender).send(email.capture());
        assertThat(email.getValue().body()).doesNotContain("null");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "\"eventId\":\"0e15a972-ae98-429a-a35f-0b61251f0481\",",
                "\"occurredAt\":\"2026-11-09T18:04:18Z\",",
                "\"customerId\":\"1c02b30d-233c-44ea-a4bf-aa491c81dee9\",",
                "\"firstName\":\"Ada\",",
                "\"lastName\":\"Lovelace\",",
                "\"email\":\"ada@example.com\",",
                "\"state\":\"NY\","
            })
    void aMissingRequiredFieldIsRejected(String field) {
        assertRejected(ADDITION.replace(field, ""));
    }

    @Test
    void aBlankEmailIsRejected() {
        assertRejected(ADDITION.replace("ada@example.com", " "));
    }

    @Test
    void aNullEventIsRejected() {
        assertRejected("null");
    }

    @Test
    void aMissingEventTypeIsRejected() {
        assertRejected(ADDITION.replace("\"eventType\":\"customer-addition\",", ""));
    }

    @Test
    void missingChangesAreRejected() {
        assertRejected(ADDITION.replace(",\"changes\":{}", ""));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "customer-change|{}",
                "customer-change|{\"fields\":[]}",
                "customer-address-change|{}",
                "customer-address-change|{\"address\":{\"street\":\"9 Elm\"}}",
                "customer-phone-add|{}",
                "customer-phone-add|{\"phone\":{}}",
                "customer-phone-change|{\"phone\":{\"type\":\"CELL\",\"number\":\"1\"}}",
                "customer-phone-delete|{\"phone\":{\"type\":\"CELL\"}}"
            })
    void changesTheTemplateNeedsAreRequired(String type, String changes) {
        assertRejected(EventFixtures.json(type, changes));
    }

    private void assertRejected(String payload) {
        assertThatThrownBy(() -> service.handle(payload)).isInstanceOf(MalformedEventException.class);

        verifyNoInteractions(incomingEvents, emails, sender);
    }

    private static IncomingEventResponse existing(IncomingEventStatus status) {
        return IncomingEventResponse.builder().eventId(EVENT_ID).status(status).build();
    }
}
