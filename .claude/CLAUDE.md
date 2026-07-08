# Kairos — Project Instructions for Claude

> Universal event scheduler for microservice systems.
> `Store → Wait → Trigger → Retry → Track`. Kairos does **not** execute business
> logic — it only triggers events at the right time and delivers messages to
> destinations.

Read this file first. Code rules live in [GUIDELINES.md](./GUIDELINES.md) and
**must** be followed for every change. Source-of-truth design docs live in
`doc/` (see below).

---

## Source documents (read before non-trivial work)

| File | What it covers |
|---|---|
| `README.md` | Vision, problem, high-level architecture, tech stack, roadmap |
| `doc/specification.md` | The *why*: entities, schedule semantics, execution lifecycle, V1 API scope, DDD + Hexagonal rationale |
| `doc/plan.md` | Development plan by milestone (M1–M10+). M1 = Task CRUD ✅ |
| `doc/database.md` | Exact table fields (destinations, tasks, retry_policies, schedules, executions, execution_history) |

When a task touches the domain model, schedule rules, or the execution
lifecycle, re-read the relevant section of `doc/` rather than relying on
memory — several decisions there are deliberate and "should not be
re-litigated".

---

## Architecture: DDD (tactical) + Hexagonal (Ports & Adapters)

Dependencies point **inward**. Domain at the center, infrastructure at the edge.

- **`domain`** — pure Java, **zero framework dependencies**. Entities, value
  objects, invariants enforced inside entities, ports (interfaces).
- **`application`** — orchestrates the domain via use cases. Knows nothing
  about HTTP / Kafka / SQL.
- **`infrastructure`** — implements the ports (JOOQ repositories, messaging
  adapters, REST controllers). Depends inward only.

**Tactical DDD only** (aggregates, value objects, invariants) — not the
strategic toolkit (no bounded-context workshops). Kairos is a single-team
infra service with little business complexity.

### Aggregate boundaries (do not blur these)
- `Destination` — own aggregate; connectivity config only.
- `Task` — aggregate root. `RetryPolicy` lives **inside** it as a value-object
  collection (a retry step has no identity outside its task).
- `Schedule` — its **own** aggregate, references `Task` by id only. Never load
  schedules through `Task`. Use factory methods `Schedule.once/cron/fixed(...)`
  so invalid combinations can't be constructed.
- `Execution` — own aggregate, references `taskId`/`scheduleId` by id. This is
  the place for **rich** domain behavior: model the lifecycle as explicit
  methods with guard clauses (`claim(workerId)`, `succeed()`,
  `fail(retryPolicy)`), so illegal transitions are impossible to express.
- `ExecutionHistory` — **not** a rich aggregate. Append-only read-model/log
  (CQRS read side). Thin repository only, no domain behavior.

Don't force richness where the domain is plain CRUD (`Destination`, most of
`Task`'s fields). Invest modeling effort only where real invariants exist.

**Hexagonal payoff checkpoint:** the boundary is proven only when a *second*
delivery adapter (e.g. Webhook) ships with **zero** changes to
domain/application layers (milestone M7+).

---

## Module map (Gradle multi-project)

```
kairos-api/         # REST API (entry point)
kairos-engine/      # Scheduler engine: planner, claim loop, retry
kairos-worker/      # Delivery workers
kairos-adapters/    # Pluggable delivery adapters (kafka, sqs, webhook, rabbitmq)
kairos-admin/       # Admin UI (Vaadin + Spring) — the ONLY place Spring is allowed
kairos-sdk/         # Java SDK for client services
common/             # Shared models, DTO/API contracts
```

Package root: `dev.kairos`.

---

## Tech stack

- **Java 26** — pure Java, **no Spring** (except `kairos-admin`)
- **JOOQ** — type-safe SQL (`./gradlew :kairos-api:generateJooq`)
- **Flyway** — migrations (`V<n>__description.sql`)
- **HikariCP** — connection pooling
- **Kafka** — first delivery adapter
- **SLF4J + Logback** — logging
- **Docker Compose** — all local infrastructure
- **Testcontainers** — repository integration tests (real Postgres)

---

## Engine concurrency (core invariant)

The claim loop uses `SELECT ... FOR UPDATE SKIP LOCKED` so two workers never
pick the same job — safe for horizontal scaling.

- `ONCE` / `CRON` — materialized ahead of time; archived + removed on completion.
- `FIXED` — **recycled**: one row per schedule, advanced in place
  (`next_attempt_at += interval`); copied to `execution_history` and advanced
  **in the same transaction** (a crash between delivery and advance must not
  skip a tick).

---

## Build / run

```bash
docker compose up -d                      # infrastructure
./gradlew :kairos-api:generateJooq        # JOOQ codegen (after migration changes)
./gradlew build                           # build + tests
```

Definition of Done for a slice: migrations apply cleanly on a fresh DB, the
full happy path works end-to-end, and tests cover edge cases.

---

## Git workflow

- `main` — stable. `develop` — default development branch.
- `feature/xxx` — one branch per feature, branched **from `develop`**.
- Flow: Issue → branch → PR → merge to `develop`.
- Commit/push only when the user explicitly asks. Never commit directly to
  `main` or `develop`; branch first.
- Do **not** commit `.gradle/` or `.idea/` (IDE/build artifacts).
- Watch out: a stray `[submodule] active = .` in `.git/config` (sometimes
  added by the IDE) makes git treat every nested module as a submodule and
  silently stops tracking their contents. Remove it with
  `git config --unset submodule.active`.

---

## Keeping spec / docs / tests in sync

There are six subagents (`.claude/agents/`):
- **spec-keeper** — updates `doc/` when the domain model, API, schema, or
  execution lifecycle changes.
- **user-docs-writer** — updates `doc/usage/` when endpoints or DTO fields change.
- **test-author** — writes/updates unit + integration tests.
- **javadoc-writer** — adds Javadoc to public/protected types and methods that
  lack it (skips trivial getters and noise comments).
- **logging-instrumenter** — adds/tunes SLF4J logging at the right levels
  (DEBUG/INFO/WARN/ERROR) for audit and analysis. Never logs in `domain`
  (it must stay framework-free).
- **architecture-reviewer** — read-only review of changed code for Clean
  Architecture + Clean Code (layer boundaries, SRP, DRY, KISS, methods ≤40
  lines, design patterns where they help). Reports findings and fixes; never
  edits code. Guards against over-engineering (no deep generics, no speculative
  abstraction) — readability wins.

**When *I* (Claude) change `.java`/`.sql` files**, a `PostToolUse` hook reminds
me to delegate the relevant follow-ups to these subagents automatically.

**When the *user* writes code themselves** (in their IDE, outside Claude), the
hook does NOT fire. The user runs the **`/sync`** slash command
(`.claude/commands/sync.md`), which diffs their changes and delegates the
relevant updates to the subagents above.

## When writing code

1. Follow [GUIDELINES.md](./GUIDELINES.md) — no exceptions.
2. Respect the layer boundaries above. No framework imports in `domain`.
3. Match the conventions in the doc/ for the milestone you're working on.
4. Test: unit tests for domain/use cases (no DB); Testcontainers for
   repositories; API tests for every endpoint including edge cases.
