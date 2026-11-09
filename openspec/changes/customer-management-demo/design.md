# Design

## Context

See proposal.md for motivation. `WelcomeEmailService` handles one event type, emails are deduplicated by a unique `sourceEventId`, and `GET /api/emails` returns everything. Project rules require a CRUD controller per collection and repositories called only from their CRUD service. The event contract is defined in `kafka-demo-customer`'s change.

## Goals / Non-Goals

**Goals:**
- No lost email on a send failure, and no repeat email on normal redelivery.

**Non-Goals:**
- A real mail provider; `LoggingEmailSender` remains.
- Notifying the old address when an email changes.
- Backfilling `lastName` and `state` on emails already stored.

## Decisions

### Processing flow

```
CustomerEventListener
        |
        v
CustomerChangeEmailService (action, replaces WelcomeEmailService)
  1. parse; unknown type? stop
  2. find incoming_event
       PROCESSED --------------------> stop
       missing   --> create RECEIVED
  3. email exists for eventId? ------+
       no --> send, then store email |
  4. mark PROCESSED <----------------+
```

Saving first means a failed send is retried. Checking `sourceEventId` before sending covers a crash after the email is stored. Records with the same key land on one partition and are handled one at a time, so two consumers won't race on one event. Alternative considered: only the unique `sourceEventId`, rejected because the incoming event must be saved and a send failure would leave no record.

Dependencies: action service to `IncomingEventService`, `EmailService` and `EmailSender`. Only the two CRUD implementations use repositories.

### Templates

A composer class in `processors` maps `eventType` to subject and body, and returns empty for an unknown type. The service owns the wording; customer only sends facts.

### Paged query

`EmailService` gains a paged search. The repository gets a custom fragment using `MongoTemplate` to build the optional criteria, with `lastName` escaped by `Pattern.quote` so input is plain text. A `PageResponse<T>` record in `dto.response` gives a stable JSON shape instead of Spring's `Page` serialization.

### Security

Same approach as the customer change: one secured chain, a `local`-only Swagger chain, `realm_access.roles` mapped to authorities, `@PreAuthorize("hasRole('customer-admin')")` on controllers.

## Risks / Trade-offs

- [Crash after sending but before the email is stored sends twice] → Accepted; delivery is at-least-once and the window is small.
- [Deleting an incoming event allows a replay after an offset reset] → Limited to `customer-admin`; the unique `sourceEventId` still blocks a second email while that email exists.
- [Case-insensitive prefix search can't use an index fully] → Acceptable at demo volume.

## Migration Plan

1. Add MongoDB migrations (one command per file): create `incoming_event`, and add indexes on `sentAt` descending, and `lastName` plus `state`.
2. Deploy after `kafka-demo-customer` publishes `customer-change-event`.

Existing emails stay listed but have no `lastName` or `state`, so filters exclude them. Rollback: restore the previous code; the new collection and indexes are harmless.
