# SDLC pipeline audit — idea → PR

Read-only audit of the Kairos AI-SDLC pipeline, step by step, from idea to PR.
Findings only — no edits applied. One section per pipeline step.

- Started: 2026-07-28
- Method: read each command/agent that owns the step, cross-check its contract
  against upstream/downstream steps and the real repo, record gaps/risks.
- Severity: **must-fix** (breaks or silently corrupts the pipeline) ·
  **should-fix** (real gap / friction) · **nit** (polish).

## Applied fixes (2026-07-28)

- ✅ **Testcontainers purged repo-wide → H2 is the sole convention** (S3-3 / S4-3 /
  S6-2). Updated: `GUIDELINES.md`, `CLAUDE.md` (×2), `tdd-implementer` (incl. the
  `h2-schema.sql` mirror duty, closing S3-4), `test-author` (×3), `acceptance-verifier`
  (no more "not-run (environment)" for repo ITs), `verify-coverage`, `doc/plan.md` (×2).
- ✅ **Every sync agent now posts its result to the issue** (S4-2). Added a shared
  contract `.claude/agents/ISSUE-POSTING.md` (≤15 lines, no-confirm) + a per-agent
  posting block to all 7 (test-author, acceptance-verifier, architecture-reviewer,
  logging-instrumenter, javadoc-writer, spec-keeper, user-docs-writer).
- ✅ **POSTING.md reconciled** (S5-2 / S6-3): explicit exception — automated
  sync-agent result comments post without confirmation; human-authored summaries
  still confirm.

Still open (not in this pass): S2-1 (spec↔issue anchor), S4-1 (logging↔javadoc
write-collision), S4-5 (parallel join/rebuild), S6-1 (real E2E drive), S7-1/S7-2
(gate realignment to the 7-agent + issue-post model), S8-1/S8-2 (create-pr staging
+ gate-bypass), S9-1 (un-owned final review), and the left-shift testable-criteria
theme (S1-2/S2-2/S3-1).

---

## Step 1 — Idea → Specification

**Owner:** `/specification` (`.claude/commands/specification.md`) — inline command,
no subagent. Discusses a raw idea with the user and writes `doc/specs/<slug>.md`.
**Adjacent (not this step):** `spec-keeper` agent keeps `doc/` in sync with *code*,
post-implementation — a different concern, correctly separate.

### What works
- **Discuss-before-write is explicit and enforced** — "The conversation (Step 1)
  is the main event… Do **not** write the file while questions are still open."
  Good: the human decision (shaping the idea) is not delegated to a cold subagent.
- **SRP for specs** — "One spec = one concern… suggest splitting into two." Matches
  the project's own KISS/SRP rules.
- **Clean hand-off boundary** — explicitly does *not* run `gh` / create issues;
  that's `/create-issue`. The output `Issue metadata (suggested)` block is a
  hand-off hint, and `/create-issue` re-validates labels/milestones against the
  live repo (confirmed in `create-issue.md` Steps 1–2). Contract is consistent.
- **Grounding requirement** — "Ground every opinion in the real codebase and
  `doc/`… read the relevant area before asking." Pushes concrete, not generic Qs.

### Findings

- **S1-1 (should-fix) — No template section for "alternatives considered / rejected".**
  The `## Design` bullet says "key decisions made during discussion and the
  rationale", but the structure has no explicit home for *rejected* options. In
  practice the good specs we've written (e.g. `task-schedule-count-badge.md`) do
  record "Rejected: …" inline — but that's convention, not prompted by the
  template. Downstream (`/create-issue`, reviewers) benefit from seeing why an
  approach was *not* taken. Cost: rationale for rejected designs is inconsistently
  captured, so a later reader re-litigates a settled decision.

- **S1-2 (should-fix) — Acceptance criteria have no explicit "testable / falsifiable"
  gate.** Step 1 says converge on "what 'done' verifiably looks like" and fold in
  the DoD, but nothing tells the author to make each criterion *individually
  checkable by a test or a command*. `/verify-coverage` later must map **each**
  criterion to a concrete test — vague criteria ("works end to end") make that
  mapping impossible and surface only at the verify stage, far downstream. A
  one-line rule ("every criterion must be verifiable by a specific test or
  command") would move that failure left.

- **S1-3 (nit) — Slug uniqueness / collision not addressed.** Step 2 says "pick a
  short kebab-case slug" and create `doc/specs/<slug>.md`, but doesn't say to
  check for an existing file of that name (overwrite risk) or how slug relates to
  the eventual branch name (`<issue#>-<slug>`). Minor, but a colliding slug would
  silently overwrite a prior spec.

- **S1-4 (nit) — No pointer to the SDLC gate's expectations.** The spec is where
  acceptance criteria are born, and those criteria drive `/verify-coverage` and
  the `sdlc-gate`. The template doesn't hint that criteria touching schema/API/docs
  will later *require* spec-keeper/user-docs-writer runs. Not wrong, but a
  forward-reference ("criteria that change schema/API/DTOs will require doc sync")
  would set expectations early. Low value; listed for completeness.

### Verdict for Step 1
**Solid.** The step's core (human-led discussion, clean hand-off, SRP) is right and
the inline-no-subagent choice is correct. No must-fix. Two should-fix items
(S1-1 rejected-alternatives, S1-2 testable-criteria gate) would tighten the
hand-off to verify/gate downstream; both are template additions, not redesigns.

---

## Step 2 — Specification → GitHub Issue

**Owner:** `/create-issue` (`.claude/commands/create-issue.md`) — inline command,
no subagent. Reads a finished spec, classifies it, creates the issue via `gh`.
No agent for this step, correctly (it's mechanical classification + one `gh` call,
gated by user approval).

### What works
- **Does not re-open the design** — "The discussion already happened in
  `/specification` — do not re-open it." Clean stage boundary; the spec is the input.
- **Live metadata, not stale** — Step 2 fetches `gh label list` + milestones before
  classifying, and the *reference* taxonomy embedded in the command (lines 45–53)
  was verified against the live repo during this audit: **all 15 labels and all 6
  milestones match exactly** — the hard-coded list is currently accurate, and the
  command re-fetches anyway. Good belt-and-suspenders.
- **Fail-closed on labels** — "Never invent a label… leave it off rather than guess",
  and Step 5 says on `gh` failure, correct against the live list and retry, "do not
  silently drop metadata."
- **User-gated creation** — Step 5 shows title/body/labels/milestone and creates
  only after approval. Outward-facing action is confirmed, per project rules.

### Findings

- **S2-1 (must-fix) — No back-link written from the issue to the spec, and no
  record of the created issue number back into the spec.** The issue body links
  the spec (`Spec: doc/specs/<slug>.md`, line 100), but nothing writes the
  resulting **issue number** back into the spec's `## Issue metadata` block. This
  is the exact break we hit live: the spec `task-schedule-count-badge.md` and issue
  #34 drifted (spec said `module:common`, a non-existent label; issue said `V7`,
  spec-derived, but the real migration was `V8`). Because the spec has no issue
  number and the issue has no immutable spec commit ref, the two silently diverge
  and later steps (`/implement`, `sdlc-gate`) can't tell which is authoritative.
  **Cost:** the "living record" principle in CLAUDE.md ("keep the issue as the
  living record") has no mechanical anchor tying spec↔issue.

- **S2-2 (should-fix) — Acceptance criteria are copied, never reconciled.** Step 4
  says the spec's acceptance criteria "carry over directly." But if `/specification`
  produced a vague criterion (see S1-2), `/create-issue` faithfully copies the
  vague criterion into the issue — there's no "make each criterion testable" gate
  at this stage either. The pipeline never forces criteria to be falsifiable until
  `/verify-coverage`, which is 4 steps later. `/create-issue` is the natural last
  cheap checkpoint before the criteria become the issue's binding contract.

- **S2-3 (should-fix) — `module:common` trap is real and unguarded.** Specs
  routinely touch the `common` module (shared DTOs/contracts), but there is **no
  `module:common` label** in the repo (confirmed: only api/engine/adapters/admin/
  worker/sdk). The command lists the valid module labels (line 48) but never warns
  that `common`-touching work must be labelled by its *consumer* modules
  (`module:api` + `module:admin`) instead. We hit exactly this on #34. A one-line
  note ("`common` changes have no label — tag the consuming runnable module(s)")
  would prevent the recurring mislabel.

- **S2-4 (nit) — Milestone "leave unset" path is under-specified for closing.**
  Step 3.4 allows leaving the milestone unset "if nothing fits" — fine — but
  nothing tells `/create-issue` to surface that gap prominently so the user can
  add a milestone later. Minor; the confirm step (Step 5) does show it, so it's
  visible.

- **S2-5 (nit) — No dedup check against existing open issues.** The command never
  searches for an already-open issue covering the same spec/slug before creating
  a new one. Re-running `/create-issue` on the same spec would create a duplicate
  issue. Low probability (user-gated), but a `gh issue list --search` guard would
  be cheap insurance.

### Verdict for Step 2
**Mostly solid**, but carries the pipeline's **one genuine must-fix so far**
(S2-1): there is no mechanical spec↔issue anchor, so the two drift — which we
observed live on #34 (module:common, V7/V8). The classification logic, live-label
fetch, and user gating are all correct. S2-2 (criteria reconciliation) and S2-3
(`module:common` guard) are recurring real-world friction worth a small fix.

---

## Step 3 (partial) — Orchestration + `tdd-implementer` ONLY

Scope of this pass: the orchestration entry into the build stage and the **first**
worker it launches (`tdd-implementer`). The five sync agents that run afterward
(spec-keeper, user-docs-writer, test-author, javadoc-writer, logging-instrumenter)
are **out of scope here** — audited separately later.

**Terminology note (worth stating):** there is **no "orchestrator agent"** in
Kairos. The orchestrator is the **command** `/implement` (and `/ship` above it) —
the controlling turn, not a subagent. `/implement` Step 3 delegates to the
`tdd-implementer` subagent and waits; the other workers run in Step 4. The user's
mental model ("an orchestrator launches tdd-implementer first, the rest wait") is
correct — it's just implemented as command-orchestration, which is the right
pattern (you don't spawn a subagent to spawn subagents).

### `/implement` as orchestrator (Steps 1–3)

**What works**
- **Concrete target before coding** — Step 1 loads the issue + locates the linked
  spec's `## Acceptance criteria`, and **stops if neither yields criteria** ("TDD
  needs a concrete target"). Fail-closed, correct.
- **Branch hygiene** — Step 2 branches from `develop`, never implements on
  `main`/`develop`, matches the `<issue#>-<slug>` naming. Aligns with CLAUDE.md.
- **Delegation contract is explicit** — Step 3 passes the issue number, spec path,
  and the numbered criteria to `tdd-implementer`, waits for the cycle/build report,
  and **surfaces any unmet criterion to the user** rather than papering over it.
- **Marker discipline now present** — Step 4 records sync-agent run markers
  (`.claude/sdlc/<slice>.md`), added during the gate work; the `sdlc-gate` reads
  these. (Step 4 itself is out of scope for this pass.)

**Findings**
- **S3-1 (should-fix) — The orchestrator hands the criteria to `tdd-implementer`
  but doesn't first check they are testable.** This is the same soft spot as
  S1-2/S2-2 landing at the point of no return: `tdd-implementer`'s job is to make
  "every acceptance criterion have at least one asserting test" — but if a
  criterion is unfalsifiable, the agent either invents a weak test or silently
  can't cover it, and the orchestrator only learns at its end-of-run report.
  `/implement` Step 1 is the last cheap place to reject a vague criterion before a
  cold subagent burns a full run on it.
- **S3-2 (nit) — No explicit "give the subagent the scope" instruction in Step 3.**
  Step 4 has a strong "put the scope in the prompt — a subagent inherits none of
  this conversation" paragraph, but Step 3 (the *first* and biggest delegation)
  lacks the same reminder. In practice the `tdd-implementer` prompt must carry the
  issue's real file map / conventions or it starts fully cold; the command doesn't
  say so for Step 3 the way it does for Step 4.

### `tdd-implementer` agent

**What works**
- **Genuinely test-first and strict** — RED (confirm it fails *for the right
  reason*), GREEN (minimal, no speculative generality), REFACTOR (clean while
  green, ≤40-line methods, literals→constants), repeat. "Never weaken a test to go
  green… the code is wrong — fix the code." This is real TDD, not test-after.
- **Binding rules restated in-agent** — Clean Architecture (domain has zero
  framework imports), aggregate boundaries (Schedule/Execution by id, factory
  methods), no-literals, no-Spring-outside-admin. A cold subagent gets the rules
  without needing the whole of CLAUDE.md.
- **Stays in its lane** — explicitly does NOT open PRs, write user docs, tune
  logging, or update the spec — leaves those to the Step-4 agents. Clean SRP; no
  overlap with the sync workers. Ends with the report the orchestrator needs
  (cycles, files, build result, any unmet criterion).
- **"Plan the cycle list before coding"** (step 0) — forces ordered smallest
  behaviors, domain→application→infrastructure. Good.

**Findings**
- **S3-3 (must-fix) — Agent mandates "Testcontainers + Postgres" for repository
  ITs, but the repo uses in-process H2 (`H2DatabaseBase`).** Line 63: "Integration
  tests for repositories use Testcontainers + Postgres." Verified against the
  repo: **3 repository ITs extend `H2DatabaseBase`** (H2 in PostgreSQL-compat mode,
  "no Docker / Testcontainers" per its own Javadoc); Testcontainers is effectively
  unused. CLAUDE.md's stack list says the same ("Testcontainers — repository
  integration tests (real Postgres)"). A cold `tdd-implementer` believes the
  prompt, then discovers H2 on the ground — this cost real churn on **both #34 and
  #12** in this session (H2 can't express partial indexes; the agent had to
  improvise). **Fix direction (record only):** the agent + CLAUDE.md must state the
  *actual* convention — repository ITs extend `H2DatabaseBase` (Postgres-dialect
  JOOQ on H2), and any behavior H2 can't express (partial/filtered indexes) is
  verified against real Postgres out-of-band. Until the docs match reality, every
  cold subagent re-discovers this.
- **S3-4 (should-fix) — `generateJooq` guidance is present but the H2-schema mirror
  duty is not.** Step 4 of the workflow says add the Flyway migration and run
  `generateJooq` "if JOOQ types are needed." But the H2-based ITs require the new
  schema to be mirrored into the `h2-schema.sql` test resource, or the IT won't see
  the new table/column. This is a real, easy-to-miss step (it bit the #12 slice) and
  the agent has no instruction about it — a direct consequence of S3-3 (the prompt
  pretends Testcontainers, so the H2 mirror doesn't "exist" in the agent's model).
- **S3-5 (nit) — No mention of the run-marker the orchestrator now expects.** Not
  the agent's job to write sync markers, but it could note in its final report
  which criteria are test-covered vs gate-satisfied in a shape the orchestrator can
  drop straight into `.claude/sdlc/<slice>.md`. Minor; the orchestrator can derive it.

### Verdict for Step 3 (orchestration + tdd-implementer)
The orchestration contract and the TDD discipline are **strong** — strict
red/green/refactor, clean SRP against the sync workers, fail-closed on missing
criteria. But this pass surfaces the pipeline's **second must-fix (S3-3):** the
agent's stated test-infra convention (Testcontainers+Postgres) **contradicts the
actual repo (H2)**, and that contradiction has already cost churn twice this
session. S3-1 (testable-criteria) is the same left-shift theme recurring, and
S3-4 (H2-schema mirror) is a direct downstream of the S3-3 doc/reality gap.

---

## Step 4 — Post-TDD sync agents (target model: parallel, each posts to the issue)

**User's target model for this step (decided 2026-07-28):**
- After `tdd-implementer` finishes, the orchestrator launches the remaining agents
  **in parallel**.
- **Two test agents kept:** `test-author` (fills coverage gaps / writes missing
  tests) + `acceptance-verifier` (proves coverage + **end-to-end**).
- **Every agent posts its own result to the issue** — one `gh issue comment` each,
  no asking, just record what it did. Six writers to the issue: test-author,
  acceptance-verifier, architecture-reviewer, logging-instrumenter, javadoc-writer,
  spec-keeper, user-docs-writer.
- **Parallelism safety by write-zone separation:** code-writers on non-overlapping
  zones; read-only agents + doc-writers run alongside freely.
- **Testcontainers is dropped entirely — H2 is the only convention** (this reverses
  the *direction* of the S3-3 / CLAUDE.md fix: bring the docs to H2, not H2 to
  Testcontainers).

This pass audits the **current** agents against that target model. Findings are the
gap between "what the agents say/do today" and "what the target requires."

### Findings

- **S4-1 (must-fix) — Parallel code-writers collide: `logging-instrumenter` and
  `javadoc-writer` both Edit the same `src/main/*.java` files.** Verified: both have
  `tools: Read, Edit, Write, …` and both target production `.java` (logging adds
  `logger.*` calls + logger fields; javadoc adds `/**…*/` to the same public types
  and methods). Run in parallel on the same slice, they edit overlapping files →
  lost writes / corrupted edits. **The write-zone separation the target relies on
  does NOT hold for these two.** Options to record: (a) serialize just these two
  (logging → javadoc, or vice-versa) while everything else parallels; (b) give each
  a disjoint file set per run; (c) worktree-per-agent + merge. Simplest correct:
  **serialize logging-instrumenter and javadoc-writer relative to each other**;
  parallelize them against the read-only + doc agents.
  - `test-author` writes only `src/test/**` → disjoint from logging/javadoc
    (`src/main`), so it CAN parallelize with them safely.
  - `spec-keeper` (`doc/specification.md`, `doc/database.md`, `doc/plan.md`) and
    `user-docs-writer` (primary zone `doc/usage/**`) only **overlap on reads** of
    database/specification — writes are disjoint, so they parallelize safely.
  - `architecture-reviewer` and `acceptance-verifier` are read-only on source →
    always safe to parallelize.

- **S4-2 (must-fix) — None of the sync agents currently post to the issue.** The
  target requires each to `gh issue comment` its own result. Today: `test-author`,
  `logging-instrumenter`, `javadoc-writer`, `spec-keeper`, `user-docs-writer` have
  **no issue-posting instruction at all**; `architecture-reviewer` reports findings
  to its caller, doesn't post; `acceptance-verifier` writes an evidence *file* but
  doesn't post. So every agent needs a new "record your result to the issue as a
  comment" step. Note this collides with an existing project rule
  (`.claude/reviews/POSTING.md` + CLAUDE.md: "All of this is outward-facing: show it
  and confirm before `gh issue comment`") — the user now wants these specific
  sync-result comments posted **without** confirmation. That exception must be made
  explicit in the agents and reconciled with POSTING.md, or the agents will follow
  the old "confirm first" rule.

- **S4-3 (must-fix) — `test-author` still mandates "Testcontainers + Postgres".**
  Its description: "Testcontainers + Postgres integration tests for repositories."
  Same contradiction as S3-3, now in a second agent. Per the decision to drop
  Testcontainers, this must become "repository ITs extend `H2DatabaseBase` (Postgres
  dialect on in-memory H2); mirror new schema into `h2-schema.sql`." **Sweep for
  Testcontainers across all agents + CLAUDE.md** — it appears in at least
  `tdd-implementer`, `test-author`, and CLAUDE.md's stack list.

- **S4-4 (should-fix) — "end-to-end" ownership is ambiguous between the two test
  agents.** The target says the coverage agent also does E2E. `acceptance-verifier`
  today "runs the mapped tests and build gates" — that's test-execution, not
  necessarily a real end-to-end drive of the flow (there's a separate `/verify`
  skill for exercising the app). Decide and state explicitly: does
  `acceptance-verifier` now own an actual E2E run (drive the API/UI, not just run
  JUnit)? If yes, its prompt + tools need to say so (it may need to start the app /
  hit endpoints), and it must not duplicate `test-author`'s gap-filling.

- **S4-5 (should-fix) — Parallel launch + orchestrator report need a join point.**
  With six agents posting independently and some editing code, the orchestrator
  must: (a) wait for all, (b) re-run the build once after the code-writers finish
  (parallel edits mean no single agent saw the final tree), (c) summarize. Today
  `/implement` Step 4 runs them "once per slice" but assumes sequential, self-report
  to the caller. The command needs an explicit fan-out → join → single build →
  gate (`/sdlc-check`) sequence.

- **S4-6 (nit) — Model/cost note stays valid.** CLAUDE.md assigns haiku to the
  mechanical agents (spec-keeper, user-docs-writer, javadoc-writer,
  logging-instrumenter) and sonnet to the judgment ones. Parallelizing doesn't
  change that, but posting-to-issue adds a `gh` call to each — trivial cost, worth
  keeping the haiku assignment.

### Verdict for Step 4
The target model (parallel sync + per-agent issue comments + H2-only) is coherent,
but the **current agents are not ready for it** — three must-fixes: **S4-1**
(logging vs javadoc write-collision breaks the parallel-safety assumption),
**S4-2** (no agent posts to the issue yet, and the no-confirm exception conflicts
with POSTING.md), **S4-3** (Testcontainers still mandated in `test-author`, must be
purged repo-wide). E2E ownership (S4-4) and the orchestrator join/rebuild (S4-5)
must be specified before this step can run parallel safely.

---

## Step 4 (deep) — Per-agent audit: any redundant? does each do what it claims?

Read all six sync agents end-to-end. Question per agent: (a) is it **redundant**
with another? (b) does the **body deliver what the `description` frontmatter
promises**?

**No agent is redundant.** The six own six disjoint concerns — tests / coverage+E2E
/ architecture / logging / javadoc / design-spec / user-docs. SRP is clean; each
prompt explicitly disclaims the others' lanes ("You do not write docs", "that's
spec-keeper", etc.). The one pair that *overlaps in file-writes* is
logging+javadoc (S4-1), but that's a parallelism-safety issue, not a
duplicated-responsibility one — they do different things to the same files.

Per-agent verdicts:

- **`test-author`** — ⚠️ **description over-promises vs body vs repo.** Frontmatter
  says "Testcontainers + Postgres integration tests for repositories"; body line 31
  repeats it. Repo reality: H2 (`H2DatabaseBase`). This is **S4-3** — the agent
  does NOT do what it claims because what it claims is wrong for this repo. Also,
  per the target model it must now **fill coverage gaps** (its stated job) *and*
  **post to the issue** (not present — S4-2). Otherwise faithful: test-only,
  no-production-edits, no-literals, ≤40-line tests, "never weaken a test" — all
  real in the body. **Verdict: keep, fix the Testcontainers claim + add issue-post.**

- **`acceptance-verifier`** (read earlier) — ⚠️ **"end-to-end" is claimed loosely.**
  It proves each criterion maps to a passing test and runs them, writing an
  evidence file. That is coverage+execution, not a literal end-to-end *drive* of
  the running system. The target wants this agent to own E2E (S4-4) — today the
  body doesn't start the app or hit real endpoints. **Verdict: keep, but pin down
  whether it now owns a real E2E run, and add issue-post.** Not redundant with
  test-author: test-author *writes* tests, verifier *proves+runs+E2E*. The line is
  clean as long as verifier doesn't start writing gap tests (that's test-author's).

- **`architecture-reviewer`** (read earlier) — ✅ **does exactly what it claims.**
  Read-only Clean-Arch/Clean-Code review, reports findings + fixes, never edits.
  Guards against over-engineering. Not redundant with the reviews in
  `/review-cycle`: it's the *architecture* lens specifically. For the target it
  only needs the **issue-post** step (S4-2); being read-only it parallelizes freely.

- **`logging-instrumenter`** — ✅ **exemplary; body fully delivers the description.**
  Clear ERROR/WARN/INFO/DEBUG/TRACE policy, "audit trail lives at INFO", never logs
  in `domain` (hard constraint, correct), parameterized messages, throwable-last,
  flags swallowed exceptions. Genuinely SRP. For the target: add issue-post (S4-2)
  and **serialize against javadoc-writer** (S4-1). No redundancy.

- **`javadoc-writer`** — ✅ **does what it claims, with good restraint.** Documents
  only public/protected types+members lacking Javadoc; explicitly refuses noise
  comments and getter-doc; contract/why over what; never changes code. Strong SRP.
  For the target: add issue-post (S4-2) and serialize against logging (S4-1). Not
  redundant.

- **`spec-keeper`** — ✅ **does what it claims.** Owns `doc/specification.md`,
  `doc/database.md`, `doc/plan.md`; ticks plan items only when actually
  implemented; flags (doesn't silently rewrite) code-vs-settled-decision
  contradictions; `doc/`-only, never source. Clean boundary against
  user-docs-writer ("the *why*" vs "how to use"). For the target: add issue-post
  (S4-2). Not redundant.

- **`user-docs-writer`** — ✅ **does what it claims;** ⚠️ **minor scope-vs-reality
  gap.** Owns `doc/usage/**` (api.md, getting-started.md, sdk.md) + README usage
  sections; reads real DTOs, never invents fields; example-first. Clean split from
  spec-keeper. The one soft edge: it lists `doc/usage/sdk.md` but the SDK doesn't
  exist yet — the body already hedges ("once it exists"), so it's honest, not a
  defect. For the target: add issue-post (S4-2). Not redundant.

### Deep-audit verdict
**Roster is right — zero redundant agents, six clean SRP lanes.** Two agents have a
description-vs-reality gap: `test-author` (Testcontainers claim → must become H2,
part of S4-3) and `acceptance-verifier` (loose "end-to-end" → must be pinned,
S4-4). `user-docs-writer`'s SDK mention is a benign forward-reference. The three
✅ agents (logging, javadoc, spec-keeper) fully deliver their descriptions today.
Every one of the six still needs the **issue-post step (S4-2)** to meet the target
model, and logging↔javadoc need **serialization (S4-1)** before parallel run.

---

## Step 5 — Review cycle (`/review-cycle` → review-all · validate-findings · fix-findings)

**Owner:** `/review-cycle` (command orchestrator). Chains three sub-commands over
one change: `/review-all` (gather) → `/validate-findings` (judge, via
`finding-validator` subagent) → `/fix-findings` (apply). A separate pipeline stage,
after the sync agents, before verify.

### What works
- **Clean gather→judge→apply separation, each independently runnable.** review-all
  only collects (explicitly told NOT to pass `--fix`/`--comment`); validate only
  annotates (read-only on source); fix only applies VALID. Textbook SRP across the
  three, and the `finding-validator` agent mirrors it ("Do not edit source code…
  fix-findings owns code changes").
- **Over-report-then-filter is deliberate and well-modeled.** review-all runs
  `/code-review high` for recall; `finding-validator` is the recall→precision gate
  ("it is correct and expected to mark many findings INVALID"), and demands concrete
  evidence for every INVALID ("name the code/doc that disproves it"). Strong.
- **Settled decisions are protected.** finding-validator: "A finding that
  contradicts a settled decision in `doc/` or CLAUDE.md is INVALID" — reviewers
  can't re-litigate the architecture. Matches the project's "should not be
  re-litigated" rule.
- **VALID-only by default; `--all` is opt-in.** fix-findings refuses to apply raw
  unvalidated findings unless `--all` — a safe default.
- **NEEDS-HUMAN is a real gate.** Both the orchestrator and fix-findings stop on
  unresolved NEEDS-HUMAN rather than guessing. Correct.
- **Post-fix artifact sync is centralized.** fix-findings Step 3: "If the fixes
  change behavior the artifacts document… delegate those follow-ups **once at the
  end** — not per edit." Consistent with the /implement cost rule.

### Findings
- **S5-1 (should-fix) — Depends on two built-in skills (`/code-review`,
  `/security-review`) that are NOT repo commands.** Verified: neither exists under
  `.claude/commands/`; they're Claude Code built-ins. review-all references them
  directly. This is fine *today* but is an undocumented external dependency: if a
  session lacks those built-ins (or they change flags), review-all silently loses
  2 of its 3 sources and no gate notices. Worth a one-line note in review-all
  ("these are Claude Code built-ins, not repo commands; if unavailable, fall back
  to architecture-reviewer only and say so").
- **S5-2 (should-fix) — POSTING "confirm before posting" conflicts with the Step-4
  target (sync agents post without asking).** POSTING.md §3 is emphatic: "Show the
  exact comment body and the target issue, and ask for confirmation before running
  `gh issue comment`." review-cycle Step 3.5 correctly follows it ("ask before
  posting"). But the new Step-4 model wants sync agents to post **without**
  confirmation (S4-2). These two rules now contradict. **Decide the boundary
  explicitly:** e.g. "automated per-stage sync/result comments post without
  confirmation; human-authored summary comments still confirm." Until reconciled,
  review-cycle and the sync agents will behave inconsistently on the same issue.
- **S5-3 (nit) — Two overlapping ways to reach the same posting.** review-cycle
  Step 3.5 posts the "Code review" comment, but `/ship` also relies on the review
  stage to post. If both /ship and a standalone /review-cycle run, the ordering of
  the three history comments (review→fix→acceptance) could double-post. POSTING.md
  handles idempotence loosely; a "check for an existing comment of this kind first"
  note would prevent duplicates.
- **S5-4 (nit) — `/review-cycle` explicitly excludes verify, which is correct but
  easy to forget.** It says so ("does not verify acceptance… run /verify-coverage
  next"). Good — just flagging that the /ship stage numbering must keep verify as
  its own stage (it does). No action.

### Verdict for Step 5
**The strongest, most mature part of the pipeline.** gather→judge→apply is cleanly
separated, recall-then-precision is deliberate, settled decisions are protected,
and the human gate (NEEDS-HUMAN) is real. No must-fix. Two should-fix: S5-1 (the
built-in `/code-review`+`/security-review` dependency is unguarded) and S5-2 (the
confirm-before-posting rule now contradicts the Step-4 no-confirm target — must be
reconciled repo-wide, since it's the same POSTING.md both stages cite).

---

## Step 6 — Verify coverage (`/verify-coverage` → `acceptance-verifier`)

**Owner:** `/verify-coverage` (command) → delegates to `acceptance-verifier`
(subagent). Proves every acceptance criterion maps to a passing test, runs them
for real, writes an evidence report, posts it to the issue. A separate pipeline
stage after review-cycle.

### What works
- **Evidence-or-it-didn't-happen is the core, and it's rigorous.** "No green check
  without a run behind it." The evidence report demands the **actual test source**
  + the **real captured output**, pasted verbatim, per criterion — "Never invent
  output. If you didn't run it, the block says so." This is the anti-hallucination
  backbone of the whole pipeline; it's very well specified.
- **Honest failure modes.** GAP (no covering test), FAIL (paste real trace), and
  **not-run (environment)** are distinct outcomes — a criterion whose test couldn't
  run is never silently marked PASS. The coverage matrix is 1 row/criterion so
  nothing is dropped.
- **Clean lane vs test-author.** Verifier maps+runs+reports, does NOT write tests
  ("If a gap exists, describe the missing test precisely and hand it to
  test-author; don't fill it yourself"). No overlap; the read-only-on-source
  boundary is explicit (Write/Edit only the evidence report).
- **Build/inspection gates count as verifiable.** Non-test criteria ("no literals",
  "methods ≤40 lines", "build passes") are handled via build/grep/inspection, not
  forced into a unit test. Pragmatic and correct.
- **Gap loop is explicit.** On gaps → recommend test-author → re-run verify. The
  cycle to close a gap is stated.

### Findings
- **S6-1 (must-fix, resolves S4-4) — "End-to-end" is claimed but NOT actually
  performed.** The agent cites the DoD ("the happy path works end-to-end… You are
  checking exactly that", lines 20–21) but its workflow (steps 1–4) only
  **maps criteria → runs JUnit/build → reports**. There is **no step that drives
  the running system** — no app start, no endpoint hit, no UI drive. A slice can be
  "verified" with green unit/API tests while the real end-to-end path was never
  exercised. Note: a Claude Code built-in `/verify` skill exists specifically for
  "exercise the flow end-to-end, not just tests" — the verifier neither uses it nor
  acknowledges it. **Per the Step-4 target (the coverage agent owns E2E), the
  agent must either: (a) add a real end-to-end drive step (start the affected
  component, exercise the happy path, capture output as evidence), or (b) explicitly
  delegate to the `/verify` built-in and fold its result into the evidence report.**
  Until then, "end-to-end" in the report is aspirational, not evidenced.
- **S6-2 (must-fix, part of S4-3/S3-3) — Both the command and the agent say
  "Testcontainers + Postgres."** verify-coverage line 41 and acceptance-verifier
  line 62 both instruct treating repo/integration tests as Testcontainers, recording
  "not-run (environment)" if Docker is absent. Repo reality: H2, no Docker needed.
  This is actively harmful here — a cold verifier may mark a perfectly runnable
  H2 IT as **not-run (environment)** because it "expected Docker," under-reporting
  real coverage. Must become H2 (in-process, always runnable — so there is no
  "environment not-run" excuse for repo ITs anymore).
- **S6-3 (should-fix) — Posts "Acceptance evidence" with "ask before posting"
  (POSTING.md) — same S5-2 conflict.** verify-coverage Step 3 confirms before
  posting; the Step-4 target wants automated posting. Same reconciliation needed;
  listed so the fix covers all three posting sites (review-cycle, fix-findings,
  verify-coverage).
- **S6-4 (nit) — Report filename can collide across same-day runs on one branch.**
  `<date>-<branch>-acceptance.md` — a second verify run the same day overwrites the
  first evidence file. review-all handles this with `-2`/`-3` suffixes; the
  acceptance report doesn't. Low impact (usually you *want* the latest), but the
  prior evidence is lost.

### Verdict for Step 6
The **evidence discipline is excellent** — the verbatim-output, GAP/FAIL/not-run
honesty, and clean lane vs test-author make this a strong gate against fake-green.
But it carries **two must-fixes**: **S6-1** — the agent *claims* end-to-end but its
workflow never drives the running system, which directly blocks the Step-4 target
of the coverage agent owning E2E; and **S6-2** — the Testcontainers assumption can
make it under-report H2 coverage as "not-run." Both are the recurring themes
(missing real E2E; Testcontainers-vs-H2) landing on the verification gate, where
they're most dangerous because this is the step that's supposed to *prove* done.

---

## Step 7 — SDLC integrity gate (`/sdlc-check` → `sdlc-gate`)

**Owner:** `/sdlc-check` (command) → `sdlc-gate` (subagent). Pre-PR gate (/ship
Stage 3.5): verifies every sync agent the diff needed actually ran + had an effect;
BLOCK → run the missing → re-gate → PASS. **Built this session** — audited here with
extra scrutiny (own work gets the strictest read).

### What works
- **Two-signal design is sound.** Marker (did it run) + diff (did it have an effect)
  cross-checked, "trust the diff" on disagreement — this is exactly what caught the
  real #12 gaps (spec-keeper/user-docs-writer skipped). The failure mode it targets
  is real and it demonstrably works (blocked #12 correctly this session).
- **Fail-closed.** "If you can't read the diff or the repo… return BLOCK." Correct
  default for a gate.
- **Concrete required-vs-not table** mirroring `/implement` step 4, with a "name the
  migration/DTO/method — that's your why-required" rule → not hand-wavy.
- **Read-only, clear hand-off.** Gate judges; orchestrator (`/sdlc-check`) runs the
  missing agents and re-gates. Clean SRP.

### Findings
- **S7-1 (must-fix) — The gate is now stale vs the Step-4 target model it's supposed
  to guard.** The target (decided *after* this gate was written) is: sync agents run
  **in parallel** and **each posts its result to the issue**. The gate checks only
  "did the agent run + affect the diff" — it does **not** check whether each agent
  **posted its issue comment**, which is now a first-class deliverable of the step.
  So an agent could run, edit files, and skip its issue post, and the gate passes.
  The gate must add a third signal: **issue-comment presence** (`gh issue view
  <n> --comments` → is there a comment from each required agent?).
- **S7-2 (must-fix) — Scope mismatch with the target roster.** The gate audits
  **five** agents and explicitly excludes `architecture-reviewer` and
  `acceptance-verifier` ("covered by /review-cycle and /verify-coverage"). But the
  Step-4 target folds those two into the same parallel post-TDD fan-out (both now
  post to the issue). If they're part of Step 4, the gate must verify them too —
  otherwise the gate guards 5 of the 7 agents the user wants guaranteed. Either the
  gate's roster grows to match the target, or the pipeline keeps arch-review/verify
  as separate stages (Step 5/6) and the gate stays at five — **this depends on the
  final Step-4 shape and must be reconciled**, not left implicitly contradictory.
- **S7-3 (should-fix) — Markers are not actually produced automatically yet.**
  Verified: only one marker file exists (`12-unique-task-name-per-service.md`) and
  it was written **by hand** this session. `/implement` step 4 now *instructs*
  writing markers, but no automated run has ever produced one. Signal 1 is therefore
  mostly theoretical today — the gate leans on Signal 2 (diff) in practice. Not
  wrong (the diff cross-check is the stronger signal anyway), but the marker
  infrastructure is aspirational until a real `/implement` run populates it. The
  gate should state that a missing marker file is **expected** for pre-marker slices
  and to lean on the diff, rather than treating "no marker" as a strong negative.
- **S7-4 (should-fix) — Agent-not-registered-until-restart is a real operational
  trap, undocumented.** We hit it live: `sdlc-gate` (and `/sdlc-check`) were not
  invokable the session they were created — Claude Code loads agent defs at session
  start. Nothing in the command warns that a freshly-added gate agent needs a
  session restart, so `/ship` Stage 3.5 would fail confusingly on the very session
  someone adds it. A one-line note ("if the sdlc-gate agent isn't available, the
  orchestrator runs the checklist inline") would prevent that.
- **S7-5 (nit) — No guard against the gate's own output not being posted.** The
  gate's "Ran — what each did" summary is described as "the feedback the user
  asked for," but nothing says whether the gate report itself should be posted to
  the issue or just returned to the orchestrator. Given the target's post-everything
  ethos, clarify.

### Verdict for Step 7
The gate's **core mechanism is correct and proven** (two-signal, fail-closed, caught
real gaps on #12). But it was written **before** the Step-4 target was finalized, so
it's now **out of sync with the model it must guard**: **S7-1** (doesn't check the
issue-comment deliverable each agent now owes) and **S7-2** (guards 5 agents while
the target fan-out has 7) are must-fixes to realign the gate with the parallel +
post-to-issue model. S7-3 (markers still manual) means Signal 1 is aspirational — fine,
since the diff signal carries the weight, but worth stating. S7-4 (restart trap) is a
real operational gotcha we already hit.

---

## Step 8 — Open the PR (`/create-pr`)

**Owner:** `/create-pr` (command, no subagent). Turns the branch's work into a PR to
`develop`, diff-based description, labels, `Closes #<n>`. The counterpart to
`/create-issue`. Final outward-facing gate before human merge.

### What works
- **Diff is the source of truth** — "Build the description from what actually changed
  on this branch, not from memory or from what was planned." Correct; the PR reflects
  reality, not intent.
- **Branch protection** — refuses to PR from `main`/`develop`; base defaults to
  `develop` with a real-remote check and a fallback to the repo default branch.
- **Labels live-fetched + classified from the diff**, same discipline as
  `/create-issue`; fail-closed on invented labels.
- **`Closes #<n>` is verified + user-confirmed** — Step 4 checks the issue is real
  and open (`gh issue view`), shows it, and only wires `Closes` after a yes; if no
  issue is found it opens without the line rather than guessing a number. Good.
- **Push/create is user-gated** — Step 6 shows title/body/labels/base/issue in one
  preview and creates only after approval; on `gh` failure, correct and retry, "do
  not silently drop labels or the Closes link." Matches project outward-facing rules.
- **Commit trailer + attribution** required (Co-Authored-By, Claude Code line).

### Findings
- **S8-1 (must-fix) — Blind staging risks pulling unrelated files into the PR.**
  Step 6: "If there are uncommitted changes or unpushed commits, commit… and push."
  It never says to stage **only the slice's files** — a `git add -A` / `git commit -a`
  would sweep in whatever else is in the working tree. This is not hypothetical: in
  **this very session**, #34 and #12 both had unrelated untracked files loitering
  (`sdlc-presentation.html`, another issue's `.claude/reviews/*` docs), and the only
  thing that kept them out of the PRs was **manual selective `git add`**. The command
  must instruct: review `git status`, stage only the files belonging to this slice,
  and explicitly exclude stray untracked/unrelated files (or stop and ask if the tree
  is dirty with unrelated work). Without this, `/create-pr` run naively pollutes the PR.
- **S8-2 (must-fix) — Standalone `/create-pr` does not confirm the pre-PR gates ran.**
  Inside `/ship` the order is correct (Stage 3.5 `/sdlc-check` → Stage 4 `/create-pr`).
  But `/create-pr` is independently runnable, and on its own it checks **nothing**
  about whether the sync agents ran (`/sdlc-check`) or acceptance was verified
  (`/verify-coverage`). A user who runs `/create-pr 12` directly gets a PR with no
  guarantee the pipeline was whole — exactly the shortcut the gate exists to prevent,
  bypassed by entering at the last step. The command should, at minimum, check for
  the gate's evidence (marker file / acceptance report / a PASS record) and warn +
  offer to run `/sdlc-check` first if absent. (The `/ship` path is fine; the naked
  entry point is the hole.)
- **S8-3 (should-fix) — No "does a PR already exist for this branch?" check.**
  Re-running `/create-pr` on a branch that already has an open PR would attempt a
  duplicate (or fail confusingly). A `gh pr list --head <branch>` guard → "update the
  existing PR body instead" would be cleaner. We effectively updated PRs by pushing
  more commits this session; the command doesn't mention that a push to an existing
  PR branch is the update path.
- **S8-4 (nit) — "Testing" section invites an unverified E2E claim.** Step 5's
  template says "call out… end-to-end happy path when relevant." Given S6-1 (no agent
  actually drives E2E), a PR author could write "happy path works end-to-end" with no
  evidence behind it. Tie the Testing section to the acceptance-evidence report so the
  claim is backed, not asserted.

### Verdict for Step 8
The PR mechanics are **well-guarded on the outward-facing axis** — branch protection,
verified `Closes`, live labels, user-gated push. But two must-fixes sit on the
**inputs** to that gate: **S8-1** (blind staging can pollute the PR with unrelated
files — which only manual care prevented twice this session) and **S8-2** (the naked
`/create-pr` entry point bypasses the whole pipeline's gates, defeating `/sdlc-check`
by skipping to the end). Both are "guard the entry, not just the exit" gaps.

---

## Step 9 — Review the PR (`/review`)

**Owner:** `/review <PR#>` — referenced as `/ship` Stage 5 and in the README
("Reviews an existing GitHub pull request"). **There is no `.claude/commands/review.md`.**
Verified: `.claude/commands/` has `review-all`, `review-cycle`, `sync-review` — but
no `review`. `/review` is a **Claude Code built-in skill**, not a repo command.

### What works
- **Conceptually the right final gate** — a fresh review pass over the *actual PR
  diff* (not the local working tree) as a last check before human merge, after
  everything else. The distinction from `/code-review` (local diff) vs `/review`
  (the pushed PR) is real and correctly placed at the end.
- **README is honest about the distinction** — it explicitly disambiguates `/review`
  (GitHub PR) from `/code-review` (local diff) from `/review-cycle` (the local
  loop). Good — this is the exact confusion the "Easily confused" section prevents.

### Findings
- **S9-1 (should-fix) — The pipeline's final stage is an un-owned external built-in.**
  Unlike every other stage, Step 9 has **no repo artifact** — no command file, no
  agent, no project-specific instructions. It inherits none of Kairos's rules: it
  won't know the aggregate boundaries, the H2 test convention, the no-literals rule,
  or the "settled decisions not re-litigated" principle that `finding-validator`
  enforces locally. So the *last* look at the code is the *least* Kairos-aware review
  in the pipeline — the opposite of what you'd want at the merge gate. A thin
  `.claude/commands/review.md` that wraps the built-in but injects the project
  context (CLAUDE.md/GUIDELINES.md pointers, "flag but don't re-litigate settled
  decisions", H2 not Testcontainers) would fix this.
- **S9-2 (should-fix) — No loop-back contract on findings.** `/ship` Stage 5 says
  "if it turns up must-fix issues, they can loop back through `/fix-findings`." But
  `/review` (built-in) produces findings in its own format, not the
  `.claude/reviews/<...>.md` findings-doc shape that `/validate-findings` /
  `/fix-findings` consume. So the "loop back" is manual re-typing, not a real
  hand-off. Either normalize `/review` output into a findings doc, or state that PR
  findings are handled ad hoc (and drop the implied clean loop-back).
- **S9-3 (should-fix) — No result posted / recorded per project convention.** Every
  other review/verify stage posts to the issue (POSTING.md) or writes a
  `.claude/reviews/` artifact. The PR review's outcome has no defined home — it's
  just surfaced to the user. For an audit trail parity with the rest of the
  pipeline, the PR-review verdict should land somewhere durable (a PR comment, or
  the issue).
- **S9-4 (nit) — Stage 5 runs *after* the PR is already open**, so any must-fix it
  finds means pushing more commits to an open PR — which S8-3 notes the pipeline
  doesn't describe as the update path. Minor; ties into S8-3.

### Verdict for Step 9
The final stage is **conceptually correct but the least-owned step in the pipeline** —
it's an external built-in with no repo command, no Kairos context, and no defined
loop-back or result-recording. **S9-1** (un-owned, context-free final review) is the
core gap: the merge-gate review knows the least about the project's own rules. No
must-fix (it does *work*, via the built-in), but three should-fixes to bring the last
stage up to the ownership/traceability standard of the other eight.

---

# Audit summary — all 9 steps

**Overall:** a genuinely strong, well-thought pipeline — clear SRP per stage, human
gates at the outward-facing points, and (Steps 5–6) excellent recall→precision and
evidence discipline. The weaknesses cluster around **three recurring themes** plus a
set of **target-model realignments** the user's new decisions introduced.

### Recurring themes (each hits multiple steps)
1. **Testcontainers-vs-H2 doc/reality gap** — the docs mandate Testcontainers+Postgres
   but the repo uses H2. Lands in `tdd-implementer` (S3-3), `test-author` (S4-3),
   `acceptance-verifier` + `verify-coverage` (S6-2), and CLAUDE.md. **User decision:
   drop Testcontainers entirely, H2 is the convention** → sweep all of these.
2. **"End-to-end" claimed but never performed** — DoD says happy-path E2E; no agent
   drives the running system (S6-1, echoed in S8-4). **User wants the coverage agent
   to own real E2E.**
3. **Spec/issue/reality drift with no mechanical anchor** — S2-1 (spec↔issue), and the
   whole reason `sdlc-gate` exists. The living-record principle has weak enforcement.

### Must-fixes (7)
- **S2-1** — no spec↔issue anchor (drift; observed on #34).
- **S3-3 / S4-3 / S6-2** — Testcontainers mandated but repo is H2 (one theme, three sites).
- **S4-1** — logging-instrumenter & javadoc-writer collide on the same `src/main` files
  under the new parallel model.
- **S4-2** — no sync agent posts to the issue yet (the new target requires it), and it
  conflicts with POSTING.md's confirm-first rule (S5-2/S6-3).
- **S6-1** — acceptance-verifier claims E2E but never drives the system.
- **S7-1 / S7-2** — the sdlc-gate is out of sync with the finalized Step-4 target
  (doesn't check issue-posting; guards 5 of the 7 agents).
- **S8-1 / S8-2** — `/create-pr` blind-stages (PR pollution) and the naked entry point
  bypasses the gates.

### Should-fixes (notable)
S1-2/S2-2/S3-1 (testable-criteria left-shift), S2-3 (`module:common` trap), S4-4 (E2E
ownership between the two test agents), S4-5 (parallel join/rebuild), S5-1 (built-in
`/code-review` dependency unguarded), S5-2 (POSTING confirm-vs-no-confirm), S7-3
(markers still manual), S7-4 (agent-registration-on-restart trap), S9-1..3 (un-owned
final review).

### Redundancy check
**No redundant agents.** All nine agents + the commands own distinct concerns.
The only *overlap* is a write-collision (logging↔javadoc, S4-1), not duplicated
responsibility.

### Where the pipeline is strongest
Steps 5 (review-cycle) and 6 (verify-coverage evidence discipline) — recall→precision
gating, settled-decision protection, verbatim-evidence, honest GAP/FAIL/not-run. These
are the model the weaker steps should be brought up to.
