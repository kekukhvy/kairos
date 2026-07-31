# Extract database plumbing into `kairos-persistence`; engine owns the schema

## Problem

Every piece of database plumbing lives inside `kairos-api`:

- `dev.kairos.config.DataSourceFactory` — HikariCP pool
- `dev.kairos.config.DatabaseMigrator` — Flyway runner
- `dev.kairos.config.AppConfig` — property loading
- `dev.kairos.infrastructure.DSLContextFactory` — shared JOOQ `DSLContext`
- `src/main/resources/db/migration/V1..V8` — the migrations themselves
- the JOOQ codegen configuration in `kairos-api/build.gradle`, generating into
  `kairos-api/src/main/generated`

`kairos-engine` has none of it. Its `build.gradle` is a one-line stub
(`description = 'TODO'`) with no dependencies, and its source tree contains only
`.gitkeep` files. The engine cannot open a connection, cannot run a query, and
cannot see the generated JOOQ tables.

This blocks all engine work — starting with `ONCE` materialization
(`doc/specs/once-materialization.md`), which must live in the engine so that the
engine-only deployment (scenario B) actually fires schedules.

Duplicating the plumbing into `kairos-engine` was rejected: it would mean two
copies of the datasource config and two JOOQ codegen setups generating the same
tables from the same schema.

## Scope

**In:**
- A new Gradle module `kairos-persistence` holding the shared database plumbing:
  datasource, migration runner, `DSLContext` factory, the migration scripts, and
  the JOOQ codegen.
- `kairos-api` consuming that module instead of its own copies; its duplicated
  classes, migrations, and codegen config are removed.
- `kairos-engine` wired up to consume it: real `build.gradle`, dependencies,
  ability to obtain a `DSLContext`.
- Moving schema ownership to the engine: `kairos-engine` runs Flyway on startup;
  `kairos-api` no longer migrates and instead fails fast with a clear message if
  the schema is absent.

**Out:**
- Any behavior change to existing endpoints. This is a structural refactor —
  every current API test must pass unchanged.
- `ONCE` materialization itself — that is the follow-up slice this unblocks.
- An engine main loop, claim loop, or delivery. This slice gives the engine a
  working database connection and a `main` that migrates and exits cleanly, not
  a scheduler.
- A Docker image for the engine, and adding engine/api services to
  `compose.yml`. Neither component is containerised today (`compose.yml` runs
  only postgres and the published admin image; `kairos-api` runs on the host),
  so the ordering wiring described under "Startup ordering" is specified here
  but lands with the packaging slice.
- Wiring `kairos-worker`, `kairos-sdk`, or the adapters to the new module.
- Splitting `AppConfig` into per-component configuration schemas.

## Design

### Why a new module and not `common`

`common` is described in its own build file as the "shared kernel (models,
contracts, base exceptions, utils)", and it is the module `kairos-sdk` will
depend on when it ships to client services as a Maven JAR. `kairos-admin`
already depends on it too.

Putting HikariCP, Flyway, and the PostgreSQL driver into `common` would drag the
whole persistence stack onto the classpath of a client SDK and a Spring UI,
neither of which touches the database. `kairos-persistence` is a separate module
depended on **only** by the components that own database access — today
`kairos-api` and `kairos-engine`.

### Module layout

```
kairos-persistence/
  src/main/java/dev/kairos/persistence/
      AppConfig.java            (moved from dev.kairos.config)
      DataSourceFactory.java    (moved from dev.kairos.config)
      DatabaseMigrator.java     (moved from dev.kairos.config)
      DSLContextFactory.java    (moved from dev.kairos.infrastructure)
  src/main/resources/db/migration/V1..V8   (moved from kairos-api)
  src/main/generated/dev/kairos/infrastructure/generated/  (JOOQ output)
```

The module is registered in `settings.gradle` and carries the dependencies
currently declared in `kairos-api`: HikariCP, `flyway-core`,
`flyway-database-postgresql`, the PostgreSQL driver, and `org.jooq:jooq`. It
exposes them with `api` (not `implementation`) so consumers can use `DSLContext`
and `DataSource` types directly.

The JOOQ codegen block moves from `kairos-api/build.gradle` verbatim, including
its resolution order for connection settings (env var → `local.properties` →
local default). The generated package name `dev.kairos.infrastructure.generated`
stays unchanged so no repository imports have to be rewritten.

### Schema ownership: the engine

The engine owns the schema and applies migrations on startup. It is the
component that owns `executions`, drives retries, and materializes schedules —
the database is its working state, not the API's.

Consequence, stated plainly: **`kairos-api` no longer creates the schema**, so
against a fresh empty database it will not start until the engine has migrated.
In deployment scenario C (engine + API, user builds their own UI) the engine is
present by definition, so this is acceptable.

The engine's `main` in this slice does exactly: load config → build datasource →
run Flyway → log the resulting version → report the schema as ready → stay up.
No `System.exit` (per the "every runnable component starts and stops cleanly"
rule in `.claude/CLAUDE.md`). The materialization pass hangs off this in the next
slice.

### Startup ordering

Two mechanisms, addressing different situations — they are complementary, not
alternatives.

**1. Orchestrated ordering (the normal path).** The engine exposes a health
signal that flips to healthy only *after* Flyway has finished, so dependents can
wait on schema readiness rather than on process start. In compose this is the
same pattern `compose.yml` already uses for postgres:

```yaml
api:
  depends_on:
    engine:
      condition: service_healthy
```

Plain `depends_on` without `condition` is **not** sufficient: it only orders
container start, so the API would come up while the engine is still applying
V1. The health signal must represent "migrations complete", not "process
running".

Note this only ships once both components are containerised (see Scope → Out).
The health signal itself is in scope here; the compose entry is not.

**2. Fail-fast in the API (the fallback).** Orchestration does not cover every
way the API gets started: Kubernetes has no `depends_on` (it needs
initContainers or its own readiness gating), developers run the API from an IDE,
and a user following the docs may point it at a database by hand. In those
cases the API must not emit a raw JOOQ error about a missing relation. It checks
on startup that the expected tables exist and fails with an explicit message
naming the cause ("schema not initialized — start kairos-engine first").

### Naming

The moved classes change package from `dev.kairos.config` /
`dev.kairos.infrastructure` to `dev.kairos.persistence`. `DSLContextFactory`
currently sits in `dev.kairos.infrastructure` in `kairos-api`, which would
collide conceptually with each component's own infrastructure package; the new
package makes the ownership obvious.

## Acceptance criteria

- [ ] `kairos-persistence` exists as a Gradle module, is listed in
      `settings.gradle`, and builds on its own.
- [ ] `AppConfig`, `DataSourceFactory`, `DatabaseMigrator`, and
      `DSLContextFactory` live in `kairos-persistence` under
      `dev.kairos.persistence`, and no copies remain in `kairos-api`.
- [ ] Migrations `V1`–`V8` live in `kairos-persistence`, and
      `kairos-api/src/main/resources/db/migration` is gone.
- [ ] JOOQ codegen runs from `kairos-persistence`, still generating into
      package `dev.kairos.infrastructure.generated`; the stale generated sources
      under `kairos-api/src/main/generated` are removed.
- [ ] `./gradlew build` passes, and every existing `kairos-api` test passes
      without modification to its assertions.
- [ ] Migrations apply cleanly on a fresh, empty database.
- [ ] `kairos-engine` has a real `build.gradle` depending on
      `kairos-persistence`, and a `main` that loads config, migrates, logs the
      applied version, and starts/stops cleanly without `System.exit`.
- [ ] Starting `kairos-engine` against a fresh empty database creates the full
      schema (all tables from `doc/database.md`).
- [ ] The engine exposes a health signal that reports healthy only after Flyway
      has completed — never while migrations are still running.
- [ ] `kairos-api` no longer invokes Flyway; grep for `DatabaseMigrator` in
      `kairos-api` returns nothing.
- [ ] Starting `kairos-api` against an empty database fails with an explicit
      message identifying the uninitialized schema, not a raw SQL error.
- [ ] Starting `kairos-api` against a schema the engine has already migrated
      works exactly as before.
- [ ] `kairos-sdk`, `kairos-admin`, and `common` do not gain a dependency on
      HikariCP, Flyway, JOOQ, or the PostgreSQL driver (verified via
      `./gradlew :kairos-admin:dependencies`).

## Notes

- Unblocks `doc/specs/once-materialization.md`, which cannot start until the
  engine can reach the database.
- `.claude/CLAUDE.md` → "Delivery & packaging model": scenario B (engine-only)
  is why the engine, not the API, owns the schema; the "every runnable component
  must start independently, no `System.exit`" rule constrains the engine's
  `main`.
- `doc/database.md` is the documented table contract users write against in
  scenario B — unchanged by this refactor, but the module that now owns those
  migrations changes, so `spec-keeper` should reflect the new location.
- Deliberately deferred: per-component configuration (the engine currently
  reuses `AppConfig` and `application.properties` wholesale, including keys it
  does not need, such as `server.port`).
- Deliberately deferred: whether `H2DatabaseBase` and the repository integration
  tests should also move into `kairos-persistence` as a shared test fixture. For
  this slice they stay in `kairos-api`; the engine's own repository tests will
  reveal whether the duplication is real.

## Issue metadata (suggested)

- **Type:** chore
- **Module(s):** module:engine, module:api, module:common
- **Priority:** priority:high
- **Milestone:** M5 — Planner (Materializer)
