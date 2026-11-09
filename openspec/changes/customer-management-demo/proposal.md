# Proposal

## Why

The email service only sends a welcome email for `CUSTOMER_CREATED`. The demo needs an email for each kind of customer change, a durable record of incoming events so redeliveries are ignored, and a paged, filterable list of sent emails limited to `customer-admin`.

## What Changes

- **BREAKING** Consume `customer-change-event` instead of `customer-events`, with one email template per event type.
- Save each incoming event in MongoDB and skip events already processed.
- Add a CRUD API for incoming events, because every table has a CRUD controller.
- **BREAKING** `GET /api/emails` becomes paged, newest first, with `lastName` and `state` filters.
- Copy `lastName` and `state` from the event onto each email.
- **BREAKING** Remove the `EMAIL_SENT` event and the `email-events` topic.
- **BREAKING** All endpoints require `customer-admin`, and the `local` profile requires tokens.

## Capabilities

### New Capabilities

- `customer-change-email`: reacting to customer change events by sending the right email exactly once in normal operation.
- `incoming-event-records`: storing incoming events and managing them through an API.
- `sent-email-browsing`: paging and filtering sent emails.
- `email-api-access`: token and role requirements for the email API.

### Modified Capabilities

None. `openspec/specs/` is empty.

## Impact

- **Code:** listener, `WelcomeEmailService`, email entity and DTOs, publisher removal, `SecurityConfig`, MongoDB migrations, tests and docs in this repo.
- **Event contract:** depends on `customer-management-demo` in `kafka-demo-customer`, which publishes `customer-change-event`. `docs/kafka-events.md` changes identically in all three service repos.
- **`kafka-demo-analytics`:** stops consuming `email-events`, which this change removes.
- **`kafka-demo-ui`:** must call the paged email API with a token.
- **`local-docker`:** topic changes are tracked in the customer change.
