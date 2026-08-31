# Kairos

> Universal event scheduler for microservice systems

## Vision

Kairos is a centralized scheduling platform for microservice architectures.

Instead of embedding Quartz in every service, writing custom `@Scheduled` tasks, or building delayed queues — services register a single Kairos API.

Kairos does not execute business logic.  
Kairos is responsible only for:

```
Store → Wait → Trigger → Retry → Track
```

---

## The Problem

In large microservice systems, scheduling becomes a mess:

- Every service contains its own Quartz instance
- Every service requires separate configuration
- Every service stores its own jobs
- No centralized schedule management
- No unified execution history
- Hard to reuse across projects

Kairos solves this through centralized scheduling.

---

## How It Works

A booking service wants to cancel a reservation after 15 minutes.

Instead of:
```java
@Scheduled
public void expireBooking() { ... }
```

It creates a task in Kairos:
```json
{
  "name": "expire-booking",
  "type": "ONCE",
  "runAt": "2026-06-15T10:00:00Z",
  "destinationId": "booking-kafka",
  "messageType": "booking.expire.v1",
  "payload": {
    "bookingId": "123"
  }
}
```

After 15 minutes, Kairos delivers the message to the destination.  
The booking service receives the event and executes its business logic.

---

## Core Concepts

### Schedule
Defines **when** to execute.

One-time:
```json
{ "type": "ONCE", "runAt": "2026-06-15T10:00:00Z" }
```

Recurring:
```json
{ "type": "CRON", "cron": "0 */5 * * * *", "timezone": "Europe/Vienna" }
```

### Destination
Defines **where** to deliver:
```json
{ "id": "booking-kafka", "type": "KAFKA" }
```

Supported types: `KAFKA` · `SQS` · `WEBHOOK` · `RABBITMQ`

### Message
Defines **what** to send:
```json
{
  "messageType": "booking.expire.v1",
  "payload": { "bookingId": "123" }
}
```

### Execution
Tracks the result: `SUCCESS` · `FAILED` · `RETRYING` · `DEAD_LETTER`

---

## Architecture

```
+------------------+
| Client Services  |
+---------+--------+
          |
          v
+------------------+
|   Kairos API     |   REST API — manage schedules
+---------+--------+
          |
          v
+------------------+
|    PostgreSQL    |   Store tasks and execution history
+---------+--------+
          |
          v
+------------------+
| Scheduler Engine |   Find due jobs, claim, retry
+---------+--------+
          |
          v
+------------------+
| Delivery Adapter |   Send to destination
+---------+--------+
          |
          v
+------------------+
| Kafka/SQS/etc    |
+------------------+
```

---

## Project Structure

```
kairos/
├── kairos-core/           # Domain model + application use cases (shared library)
├── kairos-api/            # REST API
├── kairos-engine/         # Scheduler engine (claim, retry, cron)
├── kairos-worker/         # Delivery workers
├── kairos-adapters/       # Pluggable delivery adapters
│   ├── kafka/
│   ├── sqs/
│   ├── webhook/
│   └── rabbitmq/
├── kairos-admin/          # Admin UI (Vaadin)
├── kairos-sdk/            # Java SDK for clients
└── common/                # Shared models, API contracts
```

---

## Tech Stack

- **Java 26** — pure Java, no Spring (except Admin UI)
- **JOOQ** — type-safe SQL
- **Flyway** — database migrations
- **HikariCP** — connection pooling
- **Kafka** — event delivery adapter
- **SLF4J + Logback** — logging
- **Vaadin + Spring** — Admin UI
- **Docker Compose** — local development
- **Gradle multi-project** — mono repo

---

## Retry Strategy

```
Attempt 1
↓ 5 sec
Attempt 2
↓ 30 sec
Attempt 3
↓ Dead Letter
```

Configurable per schedule.

---

## Local Development

### Prerequisites
- Java 26
- Docker

### Start infrastructure

```bash
docker compose up -d
```

### Run JOOQ codegen

```bash
./gradlew :kairos-persistence:generateJooq
```

### Build

```bash
./gradlew build
```

---

## Roadmap

### V1 — MVP
- [ ] Create / Get / Delete schedule
- [ ] One-time execution (`runAt`)
- [ ] Kafka adapter
- [ ] Retry with backoff
- [ ] Execution history
- [ ] Docker Compose

### V2 — Reliability
- [ ] Cron jobs
- [ ] Timezones
- [ ] Pause / Resume
- [ ] Dead Letter Queue

### V3 — Adapters
- [ ] SQS Adapter
- [ ] RabbitMQ Adapter
- [ ] Webhook Adapter

### V4 — Multi-tenancy
- [ ] API Keys
- [ ] Tenant isolation
- [ ] RBAC
- [ ] Credentials per tenant

### V5 — Observability
- [ ] Admin UI (Vaadin)
- [ ] Metrics
- [ ] Prometheus + Grafana

---

## Architecture Decisions

### Why not Temporal?

Temporal is a **Workflow Engine** — it manages complex multi-step business processes.

Kairos is a **Scheduler** — it is responsible only for triggering events at the right time.

### Why Hexagonal Architecture?

Domain and application layers have zero framework dependencies — pure Java.  
Infrastructure is pluggable: swap PostgreSQL, swap Kafka, swap HTTP — business logic stays untouched.

### Engine Concurrency

The core of the scheduler engine uses `SELECT FOR UPDATE SKIP LOCKED`:

```sql
SELECT * FROM schedules
WHERE status = 'PENDING'
  AND run_at <= NOW()
FOR UPDATE SKIP LOCKED
LIMIT 10
```

This prevents two workers from picking the same job — safe for horizontal scaling.

---

## Development Conventions

### Architecture
- DDD + Hexagonal Architecture (Ports & Adapters)
- `domain` — zero framework dependencies, pure Java
- `application` — orchestrates domain, no HTTP/Kafka knowledge
- `infrastructure` — implements ports (repositories, messaging)

### Git Workflow
- `main` — stable
- `develop` — default development branch
- `feature/xxx` — one branch per feature, from develop
- Every feature = Issue → branch → PR → merge to develop

### Code Style
- No Lombok in domain layer
- Builder pattern for complex domain objects
- Constants over string literals
- Configuration via `.properties` files
- Docker Compose for all infrastructure

### Admin UI (kairos-admin)
- The entire UI follows the Apple Human Interface Guidelines — clean, airy,
  restrained, with soft depth: system font, a single accent, subtle gradients
  and translucency ("frosted glass"). Restraint over decoration.
- This look is implemented **on top of Lumo by overriding Lumo design tokens**
  in a single global stylesheet (`src/main/resources/META-INF/resources/styles.css`,
  loaded via `@StyleSheet` on `AppShell`), never by per-component CSS. Because
  `StyleConfig`/`Tokens` reference `var(--lumo-*)`, they inherit the look
  automatically.
- **No inline CSS in components** — style only through `StyleConfig`/`Tokens`/the
  theme. Tokens that don't exist in Lumo (glass blur, translucent surface,
  gradients) live as named constants in `Tokens`.
- **Shared helpers, no duplication** — build buttons, notifications, fields and
  field validation only through the `shared/ui`, `shared/form` and `shared/util`
  helpers (`Buttons`, `Notifications`, `Fields`, `FieldValidation`, `Strings`).
  No ad-hoc `addThemeVariants` / `Notification.show` inside features.
- UI text is English-only (no i18n) and lives in `*Text` constant classes.