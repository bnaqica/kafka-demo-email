# kafka-demo-email

Listens to `customer-change-event`, saves each incoming event in MongoDB, skips any it has already processed, and sends the matching email: a welcome, a change notice, or a removal notice.

Part of the Kafka Demo system — see [docs/architecture.md](docs/architecture.md).

| | |
|---|---|
| Stack | Java 25, Spring Boot 4.1, Gradle 9.9 (wrapper), Lombok, SLF4J (Logback), Jib |
| Data store | MongoDB, schema managed by Flyway |
| Port | 8082 |
| Produces | — |
| Consumes | `customer-change-event` (group `kafka-demo-email`) |

## Prerequisites

- Docker, with the [local-docker](../../local-docker) stacks running (the tests start their own containers, so Docker must be running for them too)
- A JDK 17+ to run Gradle. The Java 25 toolchain is downloaded automatically by Gradle
  (foojay resolver) if you don't have it.
- In your IDE, the Lombok plugin with annotation processing enabled (IntelliJ bundles the plugin;
  VS Code's Java extension supports Lombok out of the box). Gradle builds need nothing extra.

## Run locally

Start the local-docker `mongoDB` and `kafka` stacks, then:

```bash
./gradlew bootRun
```

`bootRun` uses the `local` profile (`src/main/resources/application-local.yml`), which points at
local-docker. Override with `./gradlew bootRun -Dspring.profiles.active=<profile>`.
Swagger UI is at <http://localhost:8082/swagger-ui.html> in this profile.

Try it:

Create a customer in kafka-demo-customer, then:

```bash
curl -s -H "Authorization: Bearer $TOKEN" localhost:8082/api/emails
```

## API

Every endpoint except `/actuator/health` needs a Keycloak bearer token in the `Authorization` header in every profile,
and the `customer-admin` role: 401 without a token, 403 without the role. To get a token, see the Keycloak section of the
local-docker README. Keycloak must be running to call the API locally.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/emails` | Create an email record, without sending it. Body: `{"customerId","from","to","subject","body","status"}`, plus optional `sourceEventId`. 201 + `Location`; 409 if `sourceEventId` is already used. |
| `GET` | `/api/emails` | One page of emails, newest first. Query: `lastName` (start of the name, ignoring case), `state` (exact, ignoring case), `page` (from 0), `size` (default 20, max 100). Returns `{content, page, size, totalElements, totalPages}`. |
| `GET` | `/api/emails/{id}` | One email; 404 if unknown. |
| `GET` | `/api/emails/customer/{customerId}` | Emails for one customer. |
| `PUT` | `/api/emails/{id}` | Update an email. Same body as create; `sourceEventId` is ignored. 404 if unknown. |
| `DELETE` | `/api/emails/{id}` | Delete an email. 204; 404 if unknown. |
| `POST` | `/api/emails/incoming-events` | Create an incoming event record. 201; 409 if the `eventId` exists. |
| `GET` | `/api/emails/incoming-events` | All incoming event records. |
| `GET` | `/api/emails/incoming-events/{eventId}` | One record; 404 if unknown. |
| `PUT` | `/api/emails/incoming-events/{eventId}` | Update a record. 404 if unknown. |
| `DELETE` | `/api/emails/incoming-events/{eventId}` | Delete a record. 204; 404 if unknown. |
| `GET` | `/actuator/health` | Health check. |

## Test and format

```bash
./gradlew build
```

The build runs the tests, writes a coverage report to `build/reports/jacoco/test/html/index.html`, and fails if
the code isn't formatted. To format it:

```bash
./gradlew spotlessApply
```

## Build a container (Jib)

```bash
./gradlew jibDockerBuild     # build into your local Docker daemon as kafka-demo-email:latest
IMAGE_REGISTRY=ghcr.io/my-org ./gradlew jib     # build and push to that registry
```

The image needs these environment variables (the `local` profile is not active in the container):

| Variable | Purpose | Local value |
|---|---|---|
| `MONGODB_URI` | MongoDB connection string | `mongodb://kafka_demo_email_local:local@localhost:27017/kafka_demo_email_db?authSource=kafka_demo_email_db` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `JWT_ISSUER_URI` | Issuer of accepted tokens | `http://localhost:8180/realms/kafka-demo` |

From a container, reach the local-docker databases via `host.docker.internal`. Kafka advertises
`localhost:9092`, which doesn't work from inside a container, so join the Kafka compose network and use
`kafka:29092`:

```bash
docker run --rm --network kafka_default -p 8082:8082 \
  -e MONGODB_URI='mongodb://kafka_demo_email_local:local@host.docker.internal:27017/kafka_demo_email_db?authSource=kafka_demo_email_db' \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  kafka-demo-email:latest
```

## Database migrations

JSON files in `src/main/resources/db/migration` (`V<n>__<description>.json`), run on startup by Flyway's native MongoDB connector (`config/MongoFlywayConfig`). **Each file holds exactly one MongoDB command** (what you would pass to `db.runCommand`); Flyway only runs the first command in a file. History is kept in the `flyway_schema_history` collection.

## Documentation

- [docs/architecture.md](docs/architecture.md) — where this service fits, design decisions, known gaps
- [docs/kafka-events.md](docs/kafka-events.md) — event contracts shared across services
- [docs/local-development.md](docs/local-development.md) — local setup and troubleshooting
- [openspec/](openspec/) — spec-driven change proposals (OpenSpec)

## Contributing

Claude Code is configured for this repo (`CLAUDE.md`, `.claude/`). Claude never commits: review the
working tree and commit yourself.
