# Tasks

## 1. Event consumption

- [ ] 1.1 Add `CustomerChangeEvent` in `dto.event`, replace `CustomerCreatedEvent`, and point the listener and DLQ at `customer-change-event`; verify with a deserialization test for all seven types
- [ ] 1.2 Add the template composer in `processors`; verify with a unit test for each event type and an unknown type
- [ ] 1.3 Remove `EmailEventPublisher`, `EmailSentEvent` and the `email-events` config; verify the build passes and nothing references them
- [ ] 1.4 Rewrite `docs/kafka-events.md` to match the customer repo's text exactly; verify with a diff against that file

## 2. Incoming events

- [ ] 2.1 Add the Flyway migration for `incoming_event`, the `IncomingEvent` document and `IncomingEventStatus` enum, and the repository; verify with a Testcontainers test
- [ ] 2.2 Add `IncomingEventService` with implementation, DTOs, `IncomingEventController` and exceptions; verify with unit and controller tests for each `incoming-event-records` scenario

## 3. Processing flow

- [ ] 3.1 Add `lastName`, `state` and `eventType` to the email document, request and response; verify with `EmailServiceImpl` tests
- [ ] 3.2 Replace `WelcomeEmailService` with `CustomerChangeEmailService` per design.md; verify with unit tests for each `customer-change-email` scenario, including a failed send and a crash after storing the email
- [ ] 3.3 Update `docs/architecture.md` and `README.md` for the flow; verify the old welcome-only description is gone

## 4. Email browsing

- [ ] 4.1 Add the indexes migration and the paged, filtered repository query; verify against Testcontainers, including combined filters, sort order and literal special characters
- [ ] 4.2 Add `PageResponse` and change `GET /api/emails` with paging validation; verify with controller tests for defaults, `size=101` and filters
- [ ] 4.3 Update the README API table; verify it matches the OpenAPI output

## 5. Security

- [ ] 5.1 Update `SecurityConfig` per design.md and `SecurityConfigTests`; verify 401 without a token and open Swagger only under `local`
- [ ] 5.2 Add admin-only rules to all controllers and role tokens in controller tests; verify each `email-api-access` scenario
- [ ] 5.3 Add the springdoc bearer scheme and update the README's run and auth sections; verify the Authorize button appears under `local`

## 6. Integration

- [ ] 6.1 Run `./gradlew spotlessApply build` and verify it passes
- [ ] 6.2 With `kafka-demo-customer` running against `local-docker`, verify each event type produces the right email and that replaying a record sends nothing
- [ ] 6.3 Leave all changes uncommitted for developer review and verify with `git status` that nothing was committed
