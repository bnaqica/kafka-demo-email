# Spec Delta

## Purpose

Limits the email API, including sent emails and incoming event records, to callers holding the customer-admin role.

## ADDED Requirements

### Requirement: Token required in every profile
Every endpoint except the health check SHALL require a valid `kafka-demo` realm bearer token in every profile. In the `local` profile only, the Swagger pages SHALL be open.

#### Scenario: No token
- **WHEN** a request without a token calls an email endpoint
- **THEN** the API responds 401

#### Scenario: Swagger in local
- **WHEN** the Swagger page is requested without a token in the `local` profile
- **THEN** the page is returned

### Requirement: Admin only
Every email and incoming event endpoint SHALL require `customer-admin`.

#### Scenario: Viewer lists emails
- **WHEN** a `customer-viewer` calls `GET /api/emails`
- **THEN** the API responds 403

#### Scenario: Admin lists emails
- **WHEN** a `customer-admin` calls `GET /api/emails`
- **THEN** the API responds 200
