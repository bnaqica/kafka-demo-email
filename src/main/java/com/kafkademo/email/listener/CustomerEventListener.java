package com.kafkademo.email.listener;

import com.kafkademo.email.service.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomerEventListener {

    private final WelcomeEmailService welcomeEmailService;

    @KafkaListener(topics = "${app.kafka.topics.customer-events}")
    public void onCustomerEvent(ConsumerRecord<String, String> record) {
        welcomeEmailService.handleCustomerEvent(record.value());
    }
}
