package com.kafkademo.email;

import com.kafkademo.email.dto.event.CustomerChangeEvent;
import com.kafkademo.email.dto.event.CustomerChanges;
import java.time.Instant;
import java.util.UUID;

public final class EventFixtures {

    public static final UUID EVENT_ID = UUID.fromString("0e15a972-ae98-429a-a35f-0b61251f0481");
    public static final UUID CUSTOMER_ID = UUID.fromString("1c02b30d-233c-44ea-a4bf-aa491c81dee9");

    private EventFixtures() {}

    public static CustomerChangeEvent event(String type, CustomerChanges changes) {
        return new CustomerChangeEvent(
                EVENT_ID,
                type,
                Instant.parse("2026-11-09T18:04:18Z"),
                CUSTOMER_ID,
                "Ada",
                "Lovelace",
                "ada@example.com",
                "NY",
                changes);
    }

    public static String json(String type, String changes) {
        return """
                {"eventId":"%s","eventType":"%s","occurredAt":"2026-11-09T18:04:18Z","customerId":"%s",
                 "firstName":"Ada","lastName":"Lovelace","email":"ada@example.com","state":"NY","changes":%s}
                """.formatted(EVENT_ID, type, CUSTOMER_ID, changes);
    }
}
