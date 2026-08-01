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
kairos-engine/      # Scheduler engine: owns schema, planner, claim loop, retry
kairos-persistence/ # Shared DB layer: Flyway, JOOQ codegen, DataSource, migrations
kairos-worker/      # Delivery workers
kairos-adapters/    # Pluggable delivery adapters (kafka, sqs, webhook, rabbitmq)
kairos-admin/       # Admin UI (Vaadin + Spring) — the ONLY place Spring is allowed
kairos-sdk/         # Java SDK for client services
common/             # Shared models, DTO/API contracts
```

Package root: `dev.kairos`.

---

## Delivery & packaging model (composability principle)

Kairos is **composable**: users pick which components to run, not an
all-or-nothing monolith. Every change must keep this possible. Two component
natures, two packaging formats:

| Nature | Components | Packaged as | Boundary to the user |
|---|---|---|---|
| **Runnable** (a process) | `kairos-engine`, `kairos-api`, `kairos-admin`, each `kairos-adapters/*` | **Docker image** (version = image tag) | talks over DB / network only |
| **Embeddable** (a library) | `kairos-sdk`, shared contracts in `common` | **Maven JAR** | on the client's classpath |

**Each runnable component is always its own separate image / separate process.**
There is **no single "all-in-one" process** that merges engine+api+admin into
one JVM — development always targets the distributed layout. "Run everything
together" is a **packaging convenience**, not a code artifact: a ready-made
`docker-compose.yml` (or k8s manifest) that starts the separate containers
together. So all-in-one = several containers via compose, *not* one merged
binary.

Composition scenarios the design must support:
- **A. Full stack** — engine + api + admin + adapters, each its own container.
  A ready `docker-compose.yml` bundles them so the user runs one
  `docker compose up`; internally still separate processes. The engine starts
  first, migrates the schema, and signals readiness via `engine.health.file`;
  the api waits for that signal and then starts.
- **B. Engine-only** — `kairos-engine` image + the DB. The user writes to the
  tables directly (no api/admin). **`kairos-engine` owns and migrates the
  schema** (Flyway migrations in `kairos-persistence` are the single source of
  truth); the user writes against the *documented* table contract in
  `doc/database.md`.
- **C. Engine + API** — engine + api images; the user builds their own admin UI
  on the API. The api requires the engine to have migrated first.
- **D. Any of the above + selected adapters** at chosen versions.

Rules that follow from this — apply them to **every** change:
- **`kairos-engine` never embeds in another JVM** (only `kairos-sdk` is
  embeddable). Every runnable component is a standalone process; components talk
  to each other only over the DB / network, never via in-memory calls. That is
  what lets them be composed via compose/k8s and scaled independently.
- **Every runnable component must start independently** — its own bootstrap /
  `main`, no `System.exit`, clean start/stop — so it works on its own (scenario
  B/C) and inside the full-stack compose (scenario A).
- **Adapters are selected at runtime by image/tag** (each adapter is its own
  service/image, chosen in compose/k8s), never swapped on a shared classpath.
  This is the concrete form of the "second adapter, zero domain changes"
  hexagonal payoff — respect it when touching the adapter port.
- A change to one component must not force a lockstep change in another it
  doesn't depend on (that would break independent shipping). If it seems to,
  the boundary is wrong — surface it.

**Independent versioning (per-component) and release automation (release-please
monorepo) are the intended end state, but not being built yet.** Do not add
per-module version files or release tooling unless asked. Just don't design
anything that *prevents* it — keep components independently packageable. See
`doc/specs/` if/when a modular-distribution spec is written.

---

## Tech stack

- **Java 26** — pure Java, **no Spring** (except `kairos-admin`)
- **JOOQ** — type-safe SQL (`./gradlew :kairos-persistence:generateJooq`)
- **Flyway** — migrations in `kairos-persistence/src/main/resources/db/migration/`
  (`V<n>__description.sql`), run by `kairos-engine` on startup
- **HikariCP** — connection pooling
- **Kafka** — first delivery adapter
- **SLF4J + Logback** — logging
- **Docker Compose** — all local infrastructure
- **H2 (PostgreSQL mode)** — repository integration tests, in-process via
  `H2DatabaseBase` (no Docker/Testcontainers)

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
./gradlew :kairos-persistence:generateJooq        # JOOQ codegen (after migration changes)
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

## AI SDLC — commands & subagents

The `.claude/` directory holds a full issue → PR pipeline. The map of every
command and agent, and how they chain, lives in
[.claude/README.md](./README.md) — read it to see the whole flow (`/specification`
→ `/create-issue` → `/implement` → `/review-cycle` → `/verify-coverage` →
`/create-pr` → `/review`, or `/ship` to run the slice end to end).

**Keep the issue as the living record.** As the pipeline runs on a slice linked
to a GitHub issue, mirror progress back to that issue (see
[.claude/reviews/POSTING.md](./reviews/POSTING.md)):
- Post the review, the fixes, and the acceptance evidence as **comments**
  (history: review → fix → acceptance), summary on top + full report collapsed.
- At the verify stage, **tick the acceptance-criteria checkboxes in the issue
  body** for what's proven, noting any UI-mock/environment limits honestly.
- If the code has diverged from the criteria (design changed / dropped), don't
  leave stale criteria — **rewrite the issue body** so every criterion is valid
  and matches what shipped (code-first, same call `spec-keeper` acts on).
- All of this is outward-facing: show it and confirm before `gh issue comment` /
  `gh issue edit`.

## Keeping spec / docs / tests in sync

The **sync** subagents (`.claude/agents/`) keep artifacts aligned with the code:
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

These subagents are invoked **explicitly**, at the point in the pipeline where
the whole slice is ready to be synced — not per file edit:

- **Inside the pipeline** — `/implement` step 4 runs the relevant ones (and
  `/ship` runs `/implement`). Syncing once per slice, rather than after every
  `.java`/`.sql` write, is deliberate: each subagent starts from a cold context,
  so re-running them mid-slice is the single biggest driver of usage.
- **For code the user writes by hand** (in their IDE, outside Claude) — the
  **`/sync`** slash command (`.claude/commands/sync.md`) diffs their changes and
  delegates the relevant updates to the subagents above.

Model choice per subagent is a cost lever: the mechanical ones (`spec-keeper`,
`user-docs-writer`, `javadoc-writer`, `logging-instrumenter`) run on **haiku**;
the ones that need judgment (`tdd-implementer`, `architecture-reviewer`,
`finding-validator`, `acceptance-verifier`, `test-author`) stay on **sonnet**.

## When writing code

1. Follow [GUIDELINES.md](./GUIDELINES.md) — no exceptions.
2. Respect the layer boundaries above. No framework imports in `domain`.
3. Match the conventions in the doc/ for the milestone you're working on.
4. Test: unit tests for domain/use cases (no DB); in-process H2
   (`H2DatabaseBase`) for repository ITs; API tests for every endpoint including
   edge cases.
