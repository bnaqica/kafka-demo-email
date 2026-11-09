package com.kafkademo.email.publisher;

import com.kafkademo.email.config.EmailProperties;
import com.kafkademo.email.dto.event.EmailSentEvent;
import com.kafkademo.email.dto.response.EmailResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final EmailProperties properties;
    private final Clock clock;

    /**
     * Publishes synchronously so a failure surfaces in the listener and the record is retried.
     */
    public void publishEmailSent(EmailResponse email) {
        EmailSentEvent event = new EmailSentEvent(
                UUID.randomUUID(),
                EmailSentEvent.TYPE,
                Instant.now(clock),
                email.id(),
                email.customerId(),
                email.to(),
                email.subject());
        String topic = properties.kafka().topics().emailEvents();
        kafkaTemplate
                .send(topic, email.customerId().toString(), jsonMapper.writeValueAsString(event))
                .join();
        log.info("Published {} for email {} to {}", event.eventType(), email.id(), topic);
    }
}
