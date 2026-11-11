package com.kafkademo.email.service;

import com.kafkademo.email.dto.request.EmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(EmailRequest email) {
        log.info("Sending {} email for event {}", email.eventType(), email.sourceEventId());
    }
}
