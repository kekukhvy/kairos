# Sync-agent run markers — 63-extract-repositories

- test-author | 2026-08-31 | ran (via tdd-implementer): `ScheduleRepositoryReadinessIT` in `kairos-engine` — reads a FIXED schedule through the `ScheduleRepository` **port** (declared as the interface type, never the concrete class), backed by the real `JooqScheduleRepository`; could not have compiled from the engine before this move. `DestinationConfigValidatorTest` in `kairos-core` — drives the validator through a hand-written `ConfigKeyReader` lambda with no Jackson at all, written RED-first (compile failure before the port existed). Both mutation-checked: breaking an assertion makes them fail for the right reason | files: 2
- spec-keeper | 2026-08-31 | ran: `.claude/CLAUDE.md` (module map — `kairos-persistence` relabelled "Persistence adapter", `kairos-api` narrowed to handlers + JSON wiring; new paragraph stating ports live in `kairos-core` and implementations in `kairos-persistence`, why that lets the engine avoid depending on `kairos-api`, and why two package roots coexist inside one module), `doc/plan.md` (new `M3.7 — Repository Extraction` section; the M3.6 bullet about `kairos-api` holding repositories annotated rather than rewritten, so the historical record stays accurate) | files: 2
- user-docs-writer | 2026-08-31 | NOT NEEDED: no endpoint, DTO field, error code, or SDK surface changed. The REST layer's observable behavior is identical — `DestinationApiTest`/`TaskApiTest`/`ScheduleApiTest` pass unchanged. `ConfigKeyReader` is an internal port; a client calling POST /destinations sees the same 400 with the same message for a missing required key | files: 0
- javadoc-writer | 2026-08-31 | NOT NEEDED: the two genuinely new public types both shipped with Javadoc explaining intent and contract — `ConfigKeyReader` (states it is a port, why it exists, and the `ValidationException` contract on both failure modes) and `JacksonConfigKeyReader` (states it is the single place the API-layer destination wiring touches JSON parsing). The 8 moved files kept their existing Javadoc byte-identical | files: 0
- logging-instrumenter | 2026-08-31 | NOT NEEDED: no new production branch. The repositories moved byte-identical, keeping their existing statements; `JacksonConfigKeyReader` is a 3-line delegation to the already-tested `JsonConverter.topLevelKeys` with no branch to log; and `ConfigKeyReader` lives in `domain`, which must stay framework-free (no SLF4J) | files: 0

## Notes
- architecture-reviewer: **0 must-fix**, 2 suggestions — both applied rather than
  deferred, since the module genuinely changed character:
  1. `kairos-persistence` now hosts repository implementations, not just DB
     plumbing → module map updated to call it a "Persistence adapter".
  2. The `dev.kairos.infrastructure.*` / `dev.kairos.persistence.*` split inside
     one module needed explaining → documented in CLAUDE.md, including *why*
     (packages were preserved across two module moves, #51 and #63, so no import
     anywhere had to be rewritten).
  Reviewer also confirmed `api project(':kairos-core')` is the correct scope
  (domain types are on the repositories' public signatures) and that the
  duplicated `JacksonConfigKeyReaderForTests` is justified, not DRY debt — both
  it and the production impl are thin wrappers over one shared
  `JsonConverter.topLevelKeys`, and `kairos-core` cannot depend on `kairos-api`
  to reuse the production class without recreating the very cycle this slice removes.
- acceptance-verifier: **8/8 PASS**, 0 gaps. Criterion 5 ("no assertion changes")
  proven by `git hash-object` blob comparison rather than git's rename heuristic —
  all 8 moved files byte-identical. The two destination use-case tests that *did*
  change had constructor wiring updated for the new port; diff hunks read to
  confirm no assertion was altered. Evidence:
  `.claude/reviews/2026-08-31-63-extract-repositories-acceptance.md`.
- Honest caveat recorded in the evidence: criterion 3 passes **trivially** for
  `kairos-sdk`, which is an empty stub declaring no dependencies at all. The
  meaningful half of that check is `kairos-admin`, whose real 288-line runtime
  classpath was verified to contain no JOOQ/Hikari/Postgres.
- The 21 skipped tests are pre-existing `@Disabled(UPSERT_NEEDS_POSTGRES)`,
  present identically in `develop` (10 of them in `JooqTaskRepositoryIT` alone).
  Not a regression introduced by the move — verified before accepting the count.
- Closes #63 and #62 together. #62 was filed as a follow-up during #57's review;
  folding it in here was deliberate, since this slice already had to touch
  `DestinationConfigValidator`.
