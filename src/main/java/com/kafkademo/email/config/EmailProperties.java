package com.kafkademo.email.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.email} in application.yml. The topic name is read straight from {@code app.kafka.topics} by the
 * listener's placeholder, so it isn't bound here.
 */
@ConfigurationProperties(prefix = "app")
public record EmailProperties(Email email) {

    public record Email(String from) {}
}
