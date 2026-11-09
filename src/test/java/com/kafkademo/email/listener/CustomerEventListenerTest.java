package com.kafkademo.email.listener;

import static org.mockito.Mockito.verify;

import com.kafkademo.email.service.WelcomeEmailService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerEventListenerTest {

    @Mock
    private WelcomeEmailService welcomeEmailService;

    @InjectMocks
    private CustomerEventListener listener;

    @Test
    void passesPayloadToService() {
        listener.onCustomerEvent(new ConsumerRecord<>("customer-events", 0, 0L, "key", "{\"a\":1}"));

        verify(welcomeEmailService).handleCustomerEvent("{\"a\":1}");
    }
}
