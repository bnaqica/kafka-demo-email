# Spec Delta

## Purpose

Lets clients page through sent emails, newest first, and filter them by the customer's last name and state.

## ADDED Requirements

### Requirement: Paged email list
`GET /api/emails` SHALL return one page of emails with `content`, `page`, `size`, `totalElements` and `totalPages`. `page` defaults to 0 and `size` to 20, with a maximum of 100. Emails SHALL be sorted by `sentAt`, newest first.

#### Scenario: Default page
- **WHEN** a client calls `GET /api/emails` with 25 stored emails
- **THEN** the response holds the 20 newest and `totalElements` is 25

#### Scenario: Size too large
- **WHEN** a client requests `size=101`
- **THEN** the API responds 400

### Requirement: Filters
The list SHALL accept optional `lastName` and `state` filters. `lastName` matches the start of the name, ignoring case, and treats its value as plain text. `state` matches exactly, ignoring case. Both combine with AND.

#### Scenario: Filter by last name
- **WHEN** a client filters with `lastName=lov`
- **THEN** only emails for last names starting with "lov" in any case are returned

#### Scenario: Both filters
- **WHEN** a client filters by `lastName` and `state`
- **THEN** only emails matching both are returned, newest first

#### Scenario: Special characters
- **WHEN** a client filters with `lastName=.*`
- **THEN** only last names starting with those literal characters are returned

### Requirement: Email carries filter fields
Each email created from an event SHALL store the event's `lastName`, `state` and `eventType`, and the API SHALL return them.

#### Scenario: Stored fields
- **WHEN** an email is created from an event for a customer in NY
- **THEN** the email response has `state` NY and the customer's `lastName`

### Requirement: Email CRUD
The API SHALL support `POST` on `/api/emails`, `GET`, `PUT` and `DELETE` on `/api/emails/{id}`, and `GET` on `/api/emails/customer/{customerId}`, which returns that customer's emails, newest first. A created email without a `sourceEventId` SHALL get a generated one. An update SHALL keep the email's `sourceEventId` and `sentAt`.

#### Scenario: Create an email
- **WHEN** a valid email is posted
- **THEN** the API responds 201 with the email and its id

#### Scenario: Duplicate source event
- **WHEN** an email is posted with a `sourceEventId` another email has
- **THEN** the API responds 409

#### Scenario: Unknown email
- **WHEN** a client requests an email id that does not exist
- **THEN** the API responds 404

#### Scenario: Emails for a customer
- **WHEN** a client calls `GET /api/emails/customer/{customerId}`
- **THEN** only that customer's emails are returned, newest first

#### Scenario: Delete an email
- **WHEN** an email is deleted
- **THEN** the API responds 204
