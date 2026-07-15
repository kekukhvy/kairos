# Review findings — issue #38 — per-type destination config schema

- **Branch:** `38-destination-config-schema`
- **Base:** `develop` (`faa5ab5`)
- **Date:** 2026-07-15
- **Scope:** staged working tree (18 Java files, ~860 lines) + 4 doc files
- **Sources:** code-review (high effort, 8 angles), security-review, architecture-reviewer

---

## Summary

| Source | Findings | Outcome |
|---|---|---|
| code-review | 3 | all fixed |
| security-review | 1 (informational) | fixed |
| architecture-reviewer | folded into F3 | fixed (user decision) |

No must-fix correctness bugs. The change is sound: validation runs before the
aggregate is built (create) and before `updateConfig` (update), so a rejected
write never reaches the repository; read paths are untouched, so pre-existing
rows with non-conforming configs still load (no migration needed).

**All four findings were fixed.** `./gradlew clean build` green afterwards
(`common`, `kairos-api` incl. Testcontainers ITs, `kairos-admin`).

---

## F1 — `template()` iteration order is unspecified for `Set.of()` required-key sets

- **Severity:** should-fix (latent — not currently breaking)
- **Source:** code-review (Angle A / simplification)
- **File:** `common/src/main/java/dev/kairos/common/destination/DestinationConfigSchema.java:43-48, 95-99`

**Problem.** `template(type)` builds the JSON template by streaming
`requiredKeys(type)`. In `REQUIRED_KEYS`, only RABBITMQ's set is built with the
order-preserving `orderedSet(...)` (`LinkedHashSet`); KAFKA, SQS and WEBHOOK use
`Set.of(...)`, whose iteration order is **randomized per JVM run** (Java's
immutable-set SALT). The Javadoc on `template()` claims keys appear "in
declaration order" — a contract `Set.of` does not provide.

**Verified.** Iterating `Set.of("exchange","routingKey")` across 5 JVM runs
returned `exchange,routingKey` 4 times and `routingKey,exchange` once.

**Why it doesn't break today.** The three `Set.of` types each have exactly ONE
required key, so their template is order-independent. RABBITMQ — the only
multi-key template — correctly uses `LinkedHashSet`. So the bug is dormant.

**Failure scenario.** The moment a second required key is added to KAFKA, SQS or
WEBHOOK (e.g. KAFKA gains `bootstrapServers`), `template()` starts emitting a
randomly-ordered string, and the exact-string assertions in
`DestinationConfigSchemaTest.template_*` / `DestinationFormTest.selecting*` /
`ConfigTemplatePrefillTest` become flaky — passing locally, failing in CI on a
different JVM run. It is a trap laid for the next person to touch the table.

**Fix.** Use `orderedSet(...)` for **every** entry in `REQUIRED_KEYS` (and
`OPTIONAL_KEYS`, for consistency) so the declared order is the guaranteed order,
matching what the Javadoc already promises. One-line change per entry, no
behavior change today.

> **Outcome: FIXED.** Every entry in both maps now uses `orderedSet(...)`.
> Added `DestinationConfigSchemaTest.requiredKeys_everyType_preservesDeclarationOrder`,
> which asserts each required-key set is a `LinkedHashSet` — so reintroducing a
> `Set.of(...)` fails the build rather than producing a flaky template.

---

## F2 — `requiredKeys()` / `optionalKeys()` return `null` for an unmapped type

- **Severity:** should-fix
- **Source:** code-review (Angle A) + architecture-reviewer Q3
- **File:** `common/src/main/java/dev/kairos/common/destination/DestinationConfigSchema.java:62-69`

**Problem.** Both accessors are a bare `MAP.get(type)`. Today the maps cover all
four enum constants, so this cannot return null — but nothing *enforces* that.

**Failure scenario.** A fifth `DestinationType` constant is added (the enum and
the maps are 60 lines apart, in the same file but not coupled). `requiredKeys()`
returns `null`; `validate()` then throws a bare `NullPointerException` on
`requiredKeys(type).stream()`, which `GlobalExceptionHandler` maps to a **500**,
not the intended 400 — and `template()` NPEs in the admin UI on type selection.
The failure is far from its cause.

**Fix.** Fail fast and explicitly, e.g. have the accessors throw
`IllegalStateException("no config schema declared for type " + type)` on a
missing entry, or assert map completeness against `DestinationType.values()` in
a static initializer so a new constant breaks the build rather than production.

> **Outcome: FIXED.** Added a static initializer to `DestinationConfigSchema`
> that checks both maps cover every `DestinationType.values()` constant, throwing
> `IllegalStateException("no config schema declared for destination type …")` at
> class-load otherwise. Backed by
> `DestinationConfigSchemaTest.everyDestinationType_hasRequiredAndOptionalKeys`.

---

## F3 — Duplicate `DestinationType` enum bridged by a string round-trip

- **Severity:** should-fix (design) — **flagged for human decision**
- **Source:** code-review (Angle C) + architecture-reviewer Q1
- **Files:** `common/src/main/java/dev/kairos/common/destination/DestinationType.java` (new)
  vs `kairos-api/src/main/java/dev/kairos/domain/destination/DestinationType.java` (existing);
  bridge at `kairos-api/.../usecases/DestinationConfigValidator.java:32-34`

**Problem.** There are now two `DestinationType` enums with identical constants,
bridged by `dev.kairos.common.destination.DestinationType.valueOf(domainType.name())`.
The stated rationale is real: `kairos-admin` depends only on `common`, and
`common → kairos-api` would invert the module graph.

But the coupling is now a **string contract enforced at runtime, not by the
compiler**. Adding a constant to the domain enum without adding it to the
`common` mirror compiles cleanly and fails at runtime with
`IllegalArgumentException` (→ 500) the first time that type is used.

The issue explicitly required "no key names are duplicated in the admin module."
That holds — the *keys* are single-sourced. But the *type enum* is now
duplicated, which is arguably the same class of problem one level up.

**Options.**
1. **Keep as-is**, but add a guard test asserting the two enums have identical
   constant sets — turns silent drift into a red test. (Cheapest; keeps the
   module graph clean.)
2. **Single enum in `common`**, with the domain importing it. `common` is pure
   Java, so `domain → common` does not violate "domain must be framework-free"
   (the domain already depends on `common` for `Validation`/`ValidationException`).
   This removes the mirror and the bridge entirely.

**Recommendation:** Option 2 is the structurally correct fix and is consistent
with the domain *already* depending on `common`. Option 1 is the safe minimum.
**Needs a human call** — it moves a domain type, which is a deliberate
architectural decision, not a mechanical cleanup.

> **Outcome: FIXED — user chose Option 2 (single enum in `common`).**
> `kairos-api/.../domain/destination/DestinationType.java` **deleted**; the enum in
> `dev.kairos.common.destination` is now the only one, imported by the domain,
> the application layer, the JOOQ repository, and `kairos-admin`. The
> `toSchemaType` / `valueOf(name())` bridge in `DestinationConfigValidator` is
> **removed** — it now calls `DestinationConfigSchema.validate(type, keys)` with
> the same enum, so drift is impossible by construction rather than caught by a
> test. `domain → common` was already an existing edge (`Validation`,
> `ValidationException`) and `common` is plain Java, so the domain stays
> framework-free. `doc/specification.md` records the rationale.
> Verified: `grep -rn "domain\.destination\.DestinationType"` → no matches.

---

## F4 — Parse-error message echoes the submitted payload (informational)

- **Severity:** nit / consistency
- **Source:** security-review
- **File:** `common/src/main/java/dev/kairos/common/util/helpers/JsonConverter.java:64`

`topLevelKeys` throws `new ValidationException("config is not valid JSON: " + e.getMessage())`,
and `GlobalExceptionHandler` echoes `ValidationException.getMessage()` verbatim
into the 400 body. Jackson's message can quote a fragment of the submitted
payload and its parser position.

**Not a vulnerability:** the content is echoed only to the caller who sent it,
the response is JSON-encoded (no XSS), and no server internals leak. But note the
established pattern one line away: the existing `JacksonException` handler
deliberately returns an opaque `"Malformed request body"`. The new path is
marginally less conservative than the codebase's own convention.

**Fix (optional):** drop the `e.getMessage()` concatenation and use a fixed
`"config is not valid JSON"`.

> **Outcome: FIXED.** `topLevelKeys` now throws a fixed
> `"config is not valid JSON"` and logs Jackson's detail at DEBUG instead of
> returning it to the caller — matching the existing `JacksonException` handler's
> opaque `"Malformed request body"` convention.

---

## Not findings (checked, deliberately fine)

- **Domain unchanged / JSON parsed in the application layer** — a design decision
  taken with the user; `Destination` stays framework-free by intent.
- **Values never inspected (`{"topic": ""}` valid)** — intentional; the prefilled
  UI template must itself be submittable.
- **Unknown keys accepted** — intentional; users add custom params for adapters.
- **`ConfigTemplatePrefill` holds mutable `lastAppliedTemplate`** — fine: one
  instance per dialog/step component, single-threaded per Vaadin UI session.
- **`DestinationType.valueOf(selectedType)` in the admin prefill** — safe: the
  `Select`'s values are `DestinationText.TYPE_*`, which are exactly the enum names.
- **No DB migration** — correct: this is an application-level contract; the column
  stays free-form JSONB and existing rows still read back.
