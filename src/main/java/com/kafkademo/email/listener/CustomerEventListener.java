package com.kafkademo.email.listener;

import com.kafkademo.email.service.CustomerChangeEmailService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomerEventListener {

    private final CustomerChangeEmailService emailService;

    @KafkaListener(topics = "${app.kafka.topics.customer-change-event}")
    public void onCustomerChange(ConsumerRecord<String, String> record) {
        emailService.handle(record.value());
    }
}
