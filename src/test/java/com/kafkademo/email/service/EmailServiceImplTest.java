package com.kafkademo.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kafkademo.email.dto.request.EmailRequest;
import com.kafkademo.email.dto.response.EmailResponse;
import com.kafkademo.email.entity.EmailMessage;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.exception.EmailNotFoundException;
import com.kafkademo.email.repository.EmailMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant SENT = Instant.parse("2025-06-01T10:00:00Z");
    private static final UUID ID = UUID.randomUUID();
    private static final UUID SOURCE_EVENT_ID = UUID.randomUUID();

    @Mock
    private EmailMessageRepository repository;

    private EmailService service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createKeepsSuppliedSourceEventId() {
        when(repository.save(any(EmailMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailResponse created = service.create(request(SOURCE_EVENT_ID));

        assertThat(created.id()).isNotNull();
        assertThat(created.sourceEventId()).isEqualTo(SOURCE_EVENT_ID);
        assertThat(created.sentAt()).isEqualTo(NOW);
    }

    @Test
    void createGeneratesSourceEventIdWhenAbsent() {
        when(repository.save(any(EmailMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(request(null)).sourceEventId()).isNotNull();
    }

    @Test
    void updateKeepsSourceEventIdAndSentAt() {
        when(repository.findById(ID)).thenReturn(Optional.of(existing()));
        when(repository.save(any(EmailMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        EmailResponse updated = service.update(ID, request(UUID.randomUUID()));

        assertThat(updated.id()).isEqualTo(ID);
        assertThat(updated.sourceEventId()).isEqualTo(SOURCE_EVENT_ID);
        assertThat(updated.sentAt()).isEqualTo(SENT);
        assertThat(updated.subject()).isEqualTo("New subject");
    }

    @Test
    void deleteRemovesExistingEmail() {
        EmailMessage email = existing();
        when(repository.findById(ID)).thenReturn(Optional.of(email));

        service.delete(ID);

        verify(repository).delete(email);
    }

    @Test
    void getUnknownEmailThrowsNotFound() {
        when(repository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(ID)).isInstanceOf(EmailNotFoundException.class);
    }

    private static EmailRequest request(UUID sourceEventId) {
        return new EmailRequest(
                sourceEventId, UUID.randomUUID(), "a@test.com", "b@test.com", "New subject", "Body", EmailStatus.SENT);
    }

    private static EmailMessage existing() {
        return new EmailMessage(
                ID,
                SOURCE_EVENT_ID,
                UUID.randomUUID(),
                "a@test.com",
                "b@test.com",
                "Old",
                "Body",
                EmailStatus.SENT,
                SENT);
    }
}
