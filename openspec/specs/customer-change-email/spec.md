# customer-change-email Specification

## Purpose

Sends the right email for each customer change event and ensures a redelivered event does not send it twice.

## Requirements

### Requirement: Email for each event type
The service SHALL consume `customer-change-event` and send one email to the event's `email` for each type: welcome for `customer-addition`, removal notice for `customer-delete`, and a matching change notice for the five other types.

#### Scenario: New customer
- **WHEN** a `customer-addition` event arrives
- **THEN** a welcome email is sent to the customer

#### Scenario: Customer removed
- **WHEN** a `customer-delete` event arrives
- **THEN** a removal email is sent using the name and email in the event

#### Scenario: Address changed
- **WHEN** a `customer-address-change` event arrives
- **THEN** an email telling the customer their address changed is sent

#### Scenario: Phone added
- **WHEN** a `customer-phone-add` event arrives
- **THEN** an email naming the added phone is sent

### Requirement: Incoming event saved before sending
The service SHALL save each handled event with status `RECEIVED` before sending, and set it to `PROCESSED` after the email is stored.

#### Scenario: Normal processing
- **WHEN** an event is handled successfully
- **THEN** its record has status `PROCESSED` and exactly one email exists for its `eventId`

### Requirement: Processed events are skipped
An event whose `eventId` is already `PROCESSED` SHALL be skipped without sending an email.

#### Scenario: Redelivery
- **WHEN** an event that is already `PROCESSED` is delivered again
- **THEN** no email is sent and no record changes

### Requirement: Unfinished events are retried
An event with status `RECEIVED` SHALL be processed again when redelivered. If an email already exists for that event, the service SHALL only set the status to `PROCESSED`.

#### Scenario: Send failed earlier
- **WHEN** an event is `RECEIVED` with no stored email and is delivered again
- **THEN** the email is sent and the event becomes `PROCESSED`

#### Scenario: Crash after storing the email
- **WHEN** an event is `RECEIVED` with a stored email and is delivered again
- **THEN** no second email is sent and the event becomes `PROCESSED`

### Requirement: Failures and unknown types
Unknown event types SHALL be ignored and not saved. Malformed events SHALL go to `customer-change-event-dlq` without retries, and send failures after three retries.

#### Scenario: Unknown type
- **WHEN** an event with an unrecognized `eventType` arrives
- **THEN** no email is sent and no record is saved

#### Scenario: Malformed JSON
- **WHEN** a record is not valid JSON
- **THEN** it is published to `customer-change-event-dlq` without retries

### Requirement: No email-sent event
The service SHALL NOT publish an event after sending an email.

#### Scenario: Email sent
- **WHEN** an email is sent
- **THEN** nothing is published except dead-letter records for failures
