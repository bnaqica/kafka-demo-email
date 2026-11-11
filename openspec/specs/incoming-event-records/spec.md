# incoming-event-records Specification

## Purpose

Stores each incoming customer change event with its processing status and exposes those records through a CRUD API.

## Requirements

### Requirement: Stored event content
Each record SHALL hold the `eventId` as its identifier, `eventType`, `customerId`, the full event payload as received, `status`, `receivedAt` and `processedAt`.

#### Scenario: Record after processing
- **WHEN** an event has been processed
- **THEN** its record holds the original payload unchanged and a `processedAt` time

### Requirement: Processing time follows the status
`receivedAt` SHALL be set when a record is created and never changed. `processedAt` SHALL be set the first time a record's status becomes `PROCESSED`, kept while it stays `PROCESSED`, and cleared when the status is set to anything else.

#### Scenario: Record marked processed
- **WHEN** a `RECEIVED` record is updated to `PROCESSED`
- **THEN** its `processedAt` is set and its `receivedAt` is unchanged

#### Scenario: Record set back to received
- **WHEN** a `PROCESSED` record is updated to `RECEIVED`
- **THEN** its `processedAt` is cleared

### Requirement: Incoming event CRUD
The API SHALL support `POST` and `GET` on `/api/emails/incoming-events` and `GET`, `PUT` and `DELETE` on `/api/emails/incoming-events/{eventId}`.

#### Scenario: Read a record
- **WHEN** a client requests an existing `eventId`
- **THEN** the API responds 200 with the record

#### Scenario: Duplicate create
- **WHEN** a record is posted with an `eventId` that exists
- **THEN** the API responds 409

#### Scenario: Unknown record
- **WHEN** a client requests an `eventId` that does not exist
- **THEN** the API responds 404

#### Scenario: Delete a record
- **WHEN** a record is deleted
- **THEN** the API responds 204
