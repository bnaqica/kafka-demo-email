package com.kafkademo.email.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kafkademo.email.TestcontainersConfiguration;
import com.kafkademo.email.entity.EmailMessage;
import com.kafkademo.email.entity.EmailStatus;
import com.kafkademo.email.entity.IncomingEvent;
import com.kafkademo.email.entity.IncomingEventStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/** Against real MongoDB with the Flyway migrations applied, so the unique indexes are the real ones. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EmailPersistenceTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private EmailMessageRepository emails;

    @Autowired
    private IncomingEventRepository incomingEvents;

    @BeforeEach
    void clean() {
        emails.deleteAll();
        incomingEvents.deleteAll();
    }

    @Test
    void newestEmailsComeFirst() {
        emails.save(email("Old", "NY", 1));
        emails.save(email("New", "NY", 2));

        assertThat(search(null, null, 0, 20).getContent())
                .extracting(EmailMessage::lastName)
                .containsExactly("New", "Old");
    }

    @Test
    void pagesThroughResultsWithTheTotal() {
        for (int i = 0; i < 25; i++) {
            emails.save(email("Name" + i, "NY", i));
        }

        Page<EmailMessage> first = search(null, null, 0, 20);
        Page<EmailMessage> second = search(null, null, 1, 20);

        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getTotalElements()).isEqualTo(25);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getContent()).hasSize(5);
        assertThat(first.getContent().getFirst().lastName()).isEqualTo("Name24");
    }

    @Test
    void lastNameMatchesTheStartIgnoringCase() {
        emails.save(email("Lovelace", "NY", 1));
        emails.save(email("Glover", "NY", 2));

        assertThat(search("lov", null, 0, 20).getContent())
                .extracting(EmailMessage::lastName)
                .containsExactly("Lovelace");
    }

    @Test
    void stateMatchesExactlyIgnoringCase() {
        emails.save(email("Lovelace", "NY", 1));
        emails.save(email("Turing", "NV", 2));

        assertThat(search(null, "ny", 0, 20).getContent())
                .extracting(EmailMessage::state)
                .containsExactly("NY");
    }

    @Test
    void bothFiltersCombineNewestFirst() {
        emails.save(email("Lovelace", "NY", 1));
        emails.save(email("Lovell", "NY", 3));
        emails.save(email("Lovelace", "NV", 2));
        emails.save(email("Turing", "NY", 4));

        assertThat(search("lov", "NY", 0, 20).getContent())
                .extracting(EmailMessage::lastName)
                .containsExactly("Lovell", "Lovelace");
    }

    @Test
    void lastNameIsTreatedAsPlainText() {
        emails.save(email("Lovelace", "NY", 1));
        emails.save(email(".*Odd", "NY", 2));

        assertThat(search(".*", null, 0, 20).getContent())
                .extracting(EmailMessage::lastName)
                .containsExactly(".*Odd");
    }

    @Test
    void blankFiltersAreIgnored() {
        emails.save(email("Lovelace", "NY", 1));

        assertThat(search(" ", "", 0, 20).getContent()).hasSize(1);
    }

    @Test
    void oneEmailPerSourceEvent() {
        UUID source = UUID.randomUUID();
        emails.save(email("Lovelace", "NY", 1, source));

        assertThatThrownBy(() -> emails.save(email("Lovelace", "NY", 2, source)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void incomingEventIsKeyedByEventId() {
        UUID eventId = UUID.randomUUID();
        incomingEvents.insert(incoming(eventId));

        assertThatThrownBy(() -> incomingEvents.insert(incoming(eventId))).isInstanceOf(DuplicateKeyException.class);
    }

    private Page<EmailMessage> search(String lastName, String state, int page, int size) {
        return emails.search(lastName, state, PageRequest.of(page, size));
    }

    private static EmailMessage email(String lastName, String state, int minutes) {
        return email(lastName, state, minutes, UUID.randomUUID());
    }

    private static EmailMessage email(String lastName, String state, int minutes, UUID source) {
        return EmailMessage.builder()
                .id(UUID.randomUUID())
                .sourceEventId(source)
                .customerId(UUID.randomUUID())
                .from("a@test.com")
                .to("b@test.com")
                .subject("s")
                .body("b")
                .status(EmailStatus.SENT)
                .lastName(lastName)
                .state(state)
                .eventType("customer-addition")
                .sentAt(BASE.plusSeconds(minutes * 60L))
                .build();
    }

    private static IncomingEvent incoming(UUID eventId) {
        return IncomingEvent.builder()
                .eventId(eventId)
                .eventType("customer-addition")
                .payload("{}")
                .status(IncomingEventStatus.RECEIVED)
                .receivedAt(BASE)
                .build();
    }
}
