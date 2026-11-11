package com.kafkademo.email.dto.event;

import java.util.List;

/** What changed. Which fields are present depends on the event type; see docs/kafka-events.md. */
public record CustomerChanges(
        List<String> fields, ChangedAddress address, ChangedPhone phone, ChangedPhone previousPhone) {}
