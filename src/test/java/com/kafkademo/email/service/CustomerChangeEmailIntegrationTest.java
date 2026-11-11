package com.kafkademo.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kafkademo.email.EventFixtures;
import com.kafkademo.email.TestcontainersConfiguration;
import com.kafkademo.email.entity.IncomingEventStatus;
import com.kafkademo.email.repository.EmailMessageRepository;
import com.kafkademo.email.repository.IncomingEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CustomerChangeEmailIntegrationTest {

    private static final String EVENT = EventFixtures.json("customer-addition", "{}");

    @Autowired
    private CustomerChangeEmailService service;

    @Autowired
    private EmailMessageRepository emails;

    @Autowired
    private IncomingEventRepository incomingEvents;

    @MockitoSpyBean
    private EmailSender sender;

    @BeforeEach
    void clean() {
        emails.deleteAll();
        incomingEvents.deleteAll();
    }

    @Test
    void redeliveredEventSendsOneEmail() {
        service.handle(EVENT);
        service.handle(EVENT);

        verify(sender, times(1)).send(ArgumentMatchers.any());
        assertThat(emails.count()).isEqualTo(1);
        assertThat(incomingEvents.findById(EventFixtures.EVENT_ID).orElseThrow().status())
                .isEqualTo(IncomingEventStatus.PROCESSED);
    }

    @Test
    void savesThePayloadExactlyAsReceived() {
        service.handle(EVENT);

        assertThat(incomingEvents.findById(EventFixtures.EVENT_ID).orElseThrow().payload())
                .isEqualTo(EVENT);
    }
}
