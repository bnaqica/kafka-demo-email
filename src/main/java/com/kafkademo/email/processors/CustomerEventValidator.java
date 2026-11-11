package com.kafkademo.email.processors;

import com.kafkademo.email.dto.event.ChangedAddress;
import com.kafkademo.email.dto.event.ChangedPhone;
import com.kafkademo.email.dto.event.CustomerChangeEvent;
import com.kafkademo.email.dto.event.CustomerChanges;
import com.kafkademo.email.exception.MalformedEventException;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** Checks an event has everything its email needs, against docs/kafka-events.md, before anything is saved or sent. */
@Component
public class CustomerEventValidator {

    private static final Map<String, Consumer<CustomerChanges>> REQUIRED_CHANGES = Map.of(
            "customer-addition", changes -> {},
            "customer-delete", changes -> {},
            "customer-change",
                    changes -> {
                        if (changes.fields() == null || changes.fields().isEmpty()) {
                            throw malformed("changes.fields");
                        }
                    },
            "customer-address-change", changes -> requireAddress(changes.address()),
            "customer-phone-add", changes -> requirePhone(changes.phone()),
            "customer-phone-delete", changes -> requirePhone(changes.phone()),
            "customer-phone-change",
                    changes -> {
                        requirePhone(changes.phone());
                        requirePhone(changes.previousPhone());
                    });

    public boolean handles(String eventType) {
        return REQUIRED_CHANGES.containsKey(eventType);
    }

    public void validate(CustomerChangeEvent event) {
        require(event.eventId(), "eventId");
        require(event.occurredAt(), "occurredAt");
        require(event.customerId(), "customerId");
        requireText(event.firstName(), "firstName");
        requireText(event.lastName(), "lastName");
        requireText(event.email(), "email");
        requireText(event.state(), "state");
        require(event.changes(), "changes");
        REQUIRED_CHANGES.get(event.eventType()).accept(event.changes());
    }

    private static void requireAddress(ChangedAddress address) {
        require(address, "changes.address");
        requireText(address.street(), "changes.address.street");
        requireText(address.city(), "changes.address.city");
        requireText(address.state(), "changes.address.state");
        requireText(address.zip(), "changes.address.zip");
    }

    private static void requirePhone(ChangedPhone phone) {
        require(phone, "changes.phone");
        requireText(phone.type(), "changes.phone.type");
        requireText(phone.number(), "changes.phone.number");
    }

    private static void require(Object value, String field) {
        if (value == null) {
            throw malformed(field);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw malformed(field);
        }
    }

    private static MalformedEventException malformed(String field) {
        return new MalformedEventException("Event is missing required field '" + field + "'");
    }
}
