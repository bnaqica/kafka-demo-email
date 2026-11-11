package com.kafkademo.email.service;

import static com.kafkademo.email.EventFixtures.CUSTOMER_ID;
import static com.kafkademo.email.EventFixtures.EVENT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kafkademo.email.dto.request.IncomingEventRequest;
import com.kafkademo.email.dto.response.IncomingEventResponse;
import com.kafkademo.email.entity.IncomingEvent;
import com.kafkademo.email.entity.IncomingEventStatus;
import com.kafkademo.email.exception.DuplicateIncomingEventException;
import com.kafkademo.email.exception.IncomingEventNotFoundException;
import com.kafkademo.email.repository.IncomingEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class IncomingEventServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant RECEIVED = Instant.parse("2025-06-01T10:00:00Z");

    @Mock
    private IncomingEventRepository repository;

    private IncomingEventService service;

    @BeforeEach
    void setUp() {
        service = new IncomingEventServiceImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createStoresThePayloadUnchanged() {
        when(repository.insert(any(IncomingEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        IncomingEventResponse created = service.create(request(IncomingEventStatus.RECEIVED));

        assertThat(created.eventId()).isEqualTo(EVENT_ID);
        assertThat(created.payload()).isEqualTo("{\"a\": 1}");
        assertThat(created.receivedAt()).isEqualTo(NOW);
        assertThat(created.processedAt()).isNull();
    }

    @Test
    void createRejectsAnExistingEvent() {
        when(repository.insert(any(IncomingEvent.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.create(request(IncomingEventStatus.RECEIVED)))
                .isInstanceOf(DuplicateIncomingEventException.class);
    }

    @Test
    void findIsEmptyForAnUnknownEvent() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThat(service.find(EVENT_ID)).isEmpty();
    }

    @Test
    void getUnknownEventThrows() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(EVENT_ID)).isInstanceOf(IncomingEventNotFoundException.class);
    }

    @Test
    void movingToProcessedSetsProcessedAtAndKeepsReceivedAt() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.of(existing(null)));
        when(repository.save(any(IncomingEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        IncomingEventResponse updated = service.update(EVENT_ID, request(IncomingEventStatus.PROCESSED));

        assertThat(updated.status()).isEqualTo(IncomingEventStatus.PROCESSED);
        assertThat(updated.processedAt()).isEqualTo(NOW);
        assertThat(updated.receivedAt()).isEqualTo(RECEIVED);
    }

    @Test
    void updatingAProcessedEventKeepsItsProcessedAt() {
        when(repository.findById(EVENT_ID)).thenReturn(Optional.of(existing(RECEIVED)));
        when(repository.save(any(IncomingEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.update(EVENT_ID, request(IncomingEventStatus.PROCESSED))
                        .processedAt())
                .isEqualTo(RECEIVED);
    }

    @Test
    void deleteRemovesTheRecord() {
        IncomingEvent event = existing(null);
        when(repository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        service.delete(EVENT_ID);

        verify(repository).delete(event);
    }

    private static IncomingEventRequest request(IncomingEventStatus status) {
        return new IncomingEventRequest(EVENT_ID, "customer-addition", CUSTOMER_ID, "{\"a\": 1}", status);
    }

    private static IncomingEvent existing(Instant processedAt) {
        return IncomingEvent.builder()
                .eventId(EVENT_ID)
                .eventType("customer-addition")
                .customerId(CUSTOMER_ID)
                .payload("{}")
                .status(IncomingEventStatus.RECEIVED)
                .receivedAt(RECEIVED)
                .processedAt(processedAt)
                .build();
    }
}
