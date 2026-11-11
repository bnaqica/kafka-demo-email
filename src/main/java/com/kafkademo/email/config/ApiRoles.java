package com.kafkademo.email.config;

/** The {@code @PreAuthorize} rule used by the controllers, in terms of Keycloak realm roles. */
public final class ApiRoles {

    public static final String ADMIN = "hasRole('customer-admin')";

    private ApiRoles() {}
}
