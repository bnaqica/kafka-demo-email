package com.kafkademo.email.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.kafkademo.email.TestcontainersConfiguration;
import com.kafkademo.email.repository.EmailMessageRepository;
import com.kafkademo.email.service.CustomerChangeEmailService;
import com.kafkademo.email.service.EmailSender;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(
        properties = {
            "spring.kafka.listener.auto-startup=true",
            "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
        })
@EmbeddedKafka(
        partitions = 1,
        topics = {DeadLetterIntegrationTest.TOPIC, DeadLetterIntegrationTest.DLQ})
@Import(TestcontainersConfiguration.class)
class DeadLetterIntegrationTest {

    static final String TOPIC = "customer-change-event";
    static final String DLQ = "customer-change-event-dlq";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EmailMessageRepository emails;

    @MockitoSpyBean
    private CustomerChangeEmailService service;

    @MockitoSpyBean
    private EmailSender sender;

    @Value("${spring.embedded.kafka.brokers}")
    private String brokers;

    @Test
    void unusableEventsGoStraightToTheDeadLetterTopic() throws Exception {
        String malformed = "not json " + UUID.randomUUID();
        String missingChanges = event("customer-phone-add", "ok-" + UUID.randomUUID() + "@example.com")
                .replace(",\"changes\":{}", "");

        kafkaTemplate.send(TOPIC, "a", malformed).get();
        kafkaTemplate.send(TOPIC, "a", missingChanges).get();

        assertThat(deadLetters(List.of(malformed, missingChanges), 3000)).contains(malformed, missingChanges);
        verify(service, times(1)).handle(malformed);
        verify(service, times(1)).handle(missingChanges);
    }

    @Test
    void aFailedSendIsRetriedThreeTimesThenDeadLettered() throws Exception {
        String address = "fail-" + UUID.randomUUID() + "@example.com";
        String event = event("customer-addition", address);
        doThrow(new IllegalStateException("mail server down"))
                .when(sender)
                .send(argThat(email -> email != null && address.equals(email.to())));

        kafkaTemplate.send(TOPIC, "b", event).get();

        verify(service, timeout(10000).times(4)).handle(event);
        assertThat(deadLetters(List.of(event), 5000)).contains(event);
        assertThat(emails.findAll()).noneMatch(email -> address.equals(email.to()));
    }

    private static String event(String type, String email) {
        return """
                {"eventId":"%s","eventType":"%s","occurredAt":"2026-11-09T18:04:18Z","customerId":"%s",\
                "firstName":"Ada","lastName":"Lovelace","email":"%s","state":"NY","changes":{}}""".formatted(UUID.randomUUID(), type, UUID.randomUUID(), email);
    }

    private List<String> deadLetters(List<String> wanted, long waitMillis) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                brokers,
                ConsumerConfig.GROUP_ID_CONFIG,
                UUID.randomUUID().toString(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class);
        List<String> values = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(DLQ));
            long deadline = System.currentTimeMillis() + waitMillis;
            while (!values.containsAll(wanted) && System.currentTimeMillis() < deadline) {
                consumer.poll(Duration.ofMillis(200)).forEach(record -> values.add(record.value()));
            }
        }
        return values;
    }
}
