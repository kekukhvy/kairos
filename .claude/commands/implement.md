---
description: Implement a Kairos issue with TDD, then sync spec/docs/logging/Javadoc — from an issue number to tested, documented code on a feature branch
---

Take a GitHub issue and produce **tested, documented** code for it on a feature
branch, test-first. This is the build stage of the SDLC: it drives
implementation via TDD and then brings the surrounding artifacts (spec, user
docs, logging, Javadoc, tests) back in sync. It does **not** review, fix, verify
acceptance, or open a PR — those are later stages (`/review-cycle`,
`/verify-coverage`, `/create-pr`), tied together by `/ship`.

`$ARGUMENTS` — the issue: a number (`31` or `#31`). Optionally a spec path to use
as the source of acceptance criteria. If empty, ask which issue to implement.

## Step 1 — Load the issue and its acceptance criteria

```bash
gh issue view <n> --json title,body,labels,milestone
git branch --show-current
```

- Read the issue body; locate the linked spec under `doc/specs/` (issues created
  by `/create-issue` reference one) and read its `## Acceptance criteria`.
- If neither issue nor spec yields acceptance criteria, stop and ask — TDD needs
  a concrete target.

## Step 2 — Create the feature branch

Per the git workflow (CLAUDE.md): branch from `develop`, one branch per feature.

```bash
git switch develop && git pull        # if develop exists locally
git switch -c <issue-number>-<slug>   # match the existing branch naming
```

If already on a matching feature branch for this issue, stay on it. Never
implement on `main` or `develop` — branch first.

## Step 3 — Implement with TDD (delegate)

Invoke the **tdd-implementer** subagent via the Task tool, passing the issue
number, the spec path, and the numbered acceptance criteria. It runs strict
red → green → refactor cycles (domain → application → infrastructure),
honoring the layer + Clean Code rules, and writes both tests and production
code. Wait for it to report the cycles completed and the final build result.

If it reports a criterion it could not satisfy, surface that — don't paper over
it. Decide with the user whether to adjust scope or continue.

## Step 4 — Sync the surrounding artifacts (delegate)

Now that code exists, bring the rest in sync. Decide which subagents apply to
the actual change (skip those that don't) and run the relevant ones in parallel
via the Task tool — the same set `/sync` uses:

- **spec-keeper** — if domain model, entities, schedule/execution semantics, API
  surface, DB schema, or architecture changed → updates `doc/`.
- **user-docs-writer** — if endpoints, DTO/contract fields, or client-facing
  behavior changed → updates `doc/usage/`.
- **test-author** — to backfill edge-case tests the TDD cycles didn't cover
  (TDD covers the driven behaviors; test-author widens coverage).
- **javadoc-writer** — Javadoc for new/changed public/protected types & members.
- **logging-instrumenter** — SLF4J logging at the right levels for new code
  outside `domain` (never touches `domain`).

(Note: when *Claude* edits `.java`/`.sql`, the `PostToolUse` hook already nudges
these; running them explicitly here guarantees the sync for the whole slice.)

## Step 5 — Build green and report

```bash
./gradlew :<module>:build     # or ./gradlew build for cross-module changes
```

Report: the branch, acceptance criteria and their test status, cycles run, files
added/changed, which sync subagents ran and what they updated, and the final
build result. If the build is red, say so with the output — do not claim done.

Point the user at the next stage: `/review-cycle` (review→validate→fix→verify)
then `/create-pr`, or `/ship` to run the whole remaining pipeline. Do not commit
or push here unless the user asks.
