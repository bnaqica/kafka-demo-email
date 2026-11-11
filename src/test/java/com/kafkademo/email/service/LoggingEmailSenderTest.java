package com.kafkademo.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kafkademo.email.dto.request.EmailRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class LoggingEmailSenderTest {

    @Test
    void logsIdsRatherThanPersonalData(CapturedOutput output) {
        UUID eventId = UUID.randomUUID();

        new LoggingEmailSender()
                .send(EmailRequest.builder()
                        .sourceEventId(eventId)
                        .to("ada@example.com")
                        .subject("Welcome to Kafka Demo, Ada!")
                        .eventType("customer-addition")
                        .build());

        assertThat(output)
                .contains(eventId.toString())
                .doesNotContain("ada@example.com")
                .doesNotContain("Ada!");
    }
}
