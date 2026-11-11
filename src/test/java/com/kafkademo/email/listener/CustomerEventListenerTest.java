package com.kafkademo.email.listener;

import static org.mockito.Mockito.verify;

import com.kafkademo.email.service.CustomerChangeEmailService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerEventListenerTest {

    @Mock
    private CustomerChangeEmailService emailService;

    @InjectMocks
    private CustomerEventListener listener;

    @Test
    void passesPayloadToService() {
        listener.onCustomerChange(new ConsumerRecord<>("customer-change-event", 0, 0L, "key", "{\"a\":1}"));

        verify(emailService).handle("{\"a\":1}");
    }
}
