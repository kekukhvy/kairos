# Sync-agent run markers — 51-extract-persistence-module

- test-author | 2026-07-31 | ran: TDD-driven via tdd-implementer — EngineHealthTest (not-ready before marking, ready after markReady), EngineBootstrapTest (migrate-then-mark ordering against in-memory H2), DatabaseMigratorTest (the new String-locations overload), SchemaReadinessCheckTest (missing table -> explicit IllegalStateException). Gate flagged one hole afterwards: `EngineHealth.clear()` was only exercised transitively through the bootstrap test — closed by hand with two direct cases (clears an existing marker; clearing a missing marker does not throw) rather than respawning a cold agent for two assertions | files: 4
- spec-keeper | 2026-07-31 | ran: doc/database.md (migration path -> kairos-persistence), doc/specification.md (ApplicationContext now calls SchemaReadinessCheck.verify, API no longer runs Flyway, fail-fast message quoted), .claude/CLAUDE.md (module map gains kairos-persistence; composition scenarios A/B/C reworded for engine-owned schema and the readiness signal; tech stack points generateJooq at :kairos-persistence), doc/plan.md (new M3.5 section, 6 ticked items) | files: 4
- user-docs-writer | 2026-07-31 | ran: doc/usage/getting-started.md — Prerequisites gains a "start the Kairos Engine" step before the API step, documents the health marker file and that the engine is long-running, and states the API's fail-fast behavior; steps renumbered. Verified doc/usage/api.md and README.md needed no change (no startup/migration instructions there) | files: 1
- javadoc-writer | 2026-07-31 | NOT NEEDED: every new public type and member already carries Javadoc explaining intent and contract — KairosEngine (why it blocks on a latch rather than exiting), EngineBootstrap (why clear-then-migrate-then-mark ordering matters), EngineHealth (why a file marker rather than an HTTP endpoint, and why a stale marker must be cleared), SchemaReadinessCheck (why one well-known table is enough) | files: 0
- logging-instrumenter | 2026-07-31 | NOT NEEDED: the startup path is already instrumented at the right levels — KairosEngine logs start/stop at INFO, EngineBootstrap logs schema-ready at INFO, DatabaseMigrator logs the applied count and resulting version at INFO. No new non-domain branch is silent, and nothing in `domain` was touched (it must stay framework-free) | files: 0

## Notes
- architecture-reviewer: 2 must-fix findings, both fixed in-slice.
  1. `EngineHealth` marker survived a crash — on restart the previous run's file
     still reported "ready" while the new run was only starting to migrate,
     defeating the signal's whole purpose. Fixed with `EngineHealth.clear()`,
     called by `EngineBootstrap` before migrating. The regression test was
     verified to genuinely fail when `health.clear()` is removed, so it is
     testing the fix rather than passing incidentally.
  2. `SchemaReadinessCheck` was a generic DB-schema check stranded in
     `kairos-api`; moved to `kairos-persistence` (package
     `dev.kairos.persistence`) next to `DatabaseMigrator`, with its test.
- security-review: no HIGH/MEDIUM findings. The slice adds no external input
  path — every value (`db.url`, `db.password`, `flyway.locations`,
  `engine.health.file`) comes from `application.properties` on the classpath,
  and `SchemaReadinessCheck` uses JDBC metadata with a constant table name
  rather than composed SQL.
- acceptance-verifier: 13/13 criteria PASS, 0 gaps. Criteria 6/8/11/12 were
  proven against a real Postgres — the schema was dropped to get a genuinely
  empty database, the API was observed failing fast, the engine was observed
  applying all 8 migrations and writing the health marker only afterwards, and
  the database was restored and verified before finishing. Evidence:
  `.claude/reviews/2026-07-31-51-persistence-module-acceptance.md`.
- Carried forward to the packaging slice: `engine.health.file` defaults to
  `/tmp/kairos-engine/engine.health`. With `clear()` in place a stale marker is
  no longer a correctness risk, but when the engine is containerised the path
  should stay on the container's ephemeral filesystem rather than a mounted
  volume. `kairos-engine` has no Dockerfile yet — packaging is deliberately out
  of this slice's scope, as is the `depends_on: condition: service_healthy`
  entry in `compose.yml` that this health signal exists to serve.
