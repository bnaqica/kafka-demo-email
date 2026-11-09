package com.kafkademo.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code app.*} block of application.yml.
 */
@ConfigurationProperties(prefix = "app")
public record EmailProperties(Kafka kafka, Email email) {

    public record Kafka(Topics topics) {}

    public record Topics(String customerEvents, String emailEvents) {}

    public record Email(String from) {}
}
