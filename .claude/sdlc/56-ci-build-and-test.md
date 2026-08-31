# Slice 56 — CI: build and test every pull request

Diff: one new file, `.github/workflows/build.yml` (+64 lines). No Java, no SQL,
no API surface, no DTOs.

- spec-keeper | 2026-08-31 | skipped: no domain model, schema, API or execution-lifecycle change — CI config only
- user-docs-writer | 2026-08-31 | skipped: no endpoint, DTO field or client-facing behavior change
- test-author | 2026-08-31 | skipped: a GitHub Actions workflow is not unit-testable; verified by executing its exact command (`./gradlew --no-daemon build`) locally, including forced red cases
- javadoc-writer | 2026-08-31 | skipped: no Java types or members added
- logging-instrumenter | 2026-08-31 | skipped: no application code; workflow output is the log
