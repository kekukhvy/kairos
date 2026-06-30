---
description: Sync spec/docs/tests with the user's own code changes by delegating to the subagents
---

The user changed code themselves (in their IDE, outside Claude), so the
`PostToolUse` hook never fired. Bring the project's specification, user docs,
and tests back in sync with reality.

Steps:

1. Inspect what changed:
   - `git status`
   - `git diff` (unstaged) and `git diff --staged` (staged)
   - If `$ARGUMENTS` is non-empty, scope the sync to those files/paths only.
2. If there are no changes at all, report "nothing to sync" and stop.
3. Decide which subagents are relevant to the actual changes (skip the ones
   that don't apply):
   - **spec-keeper** — if the domain model, entities, schedule/execution
     semantics, API surface, DB schema, or architecture changed → updates `doc/`.
   - **user-docs-writer** — if endpoints, DTO/contract fields, or client-facing
     behavior changed → updates `doc/usage/`.
   - **test-author** — if domain logic, use cases, repositories, or endpoints
     changed → adds/updates unit + integration tests.
   - **javadoc-writer** — if Java types/methods were added or changed and lack
     Javadoc → adds Javadoc to public/protected types and members (skipping
     trivial getters and noise).
   - **logging-instrumenter** — if Java code outside `domain` was added or
     changed → adds/tunes SLF4J logging at the right levels (DEBUG/INFO/WARN/
     ERROR) for audit and analysis. Never touches `domain` (framework-free).
4. Delegate to each relevant subagent via the Task tool, passing the changed
   files and the diff as context. Run independent delegations in parallel.
5. Summarize what each subagent updated, and note anything skipped and why.

A pure refactor with no API/schema/behavior change may still need javadoc-writer,
logging-instrumenter, or test-author — but don't invoke a subagent whose output
wouldn't change. javadoc-writer and logging-instrumenter apply to almost any
Java change; spec-keeper and user-docs-writer only when contracts/docs move.
