# Kairos — Coding Guidelines

These rules are **mandatory** for every change. They apply to humans and to
Claude equally. If a rule conflicts with "just make it work", the rule wins —
ask before breaking one.

---

## Foundations

We follow **Clean Code** and **Clean Architecture**.

- **Clean Architecture / Hexagonal** — dependencies point inward; domain has
  zero framework dependencies; infrastructure implements ports. See
  [CLAUDE.md](./CLAUDE.md) for the layer/aggregate rules.
- **Clean Code** — code reads like prose. Intention-revealing names, small
  functions, no surprises.

---

## Hard rules (non-negotiable)

### 1. No literals in code — ever
Magic literals are **strictly forbidden**. Every value must live in a
**constant** or a **variable**, never inline.

- String literals → `private static final String` constants (or config).
- Numbers → named constants. The only allowed bare numbers are `0` and `1`
  in genuinely trivial idioms (loop start, `size - 1`); even then prefer a
  name if it carries meaning.
- Repeated/meaningful values → a single named constant, never duplicated.
- Configuration values (timeouts, URLs, topic names, intervals) → `.properties`
  files, not hardcoded.

```java
// ❌ forbidden
if (status.equals("PENDING")) { retry(5000); }

// ✅ required
private static final String STATUS_PENDING = "PENDING";
private static final int DEFAULT_RETRY_DELAY_MS = 5_000;
if (status.equals(STATUS_PENDING)) { retry(DEFAULT_RETRY_DELAY_MS); }
```

Prefer enums over string constants where the value is a closed set
(`ScheduleType`, `ExecutionStatus`, `DestinationType`).

### 2. Methods ≤ 40 lines
No method exceeds **40 lines** (body, excluding signature and braces). If it
grows past that, extract sub-methods with intention-revealing names. A long
method is a sign of a missing abstraction.

### 3. SRP — Single Responsibility Principle
Every class and method does **one** thing. One reason to change. Use cases do
one use case. Controllers translate HTTP, nothing more. Repositories persist,
nothing more.

### 4. KISS — Keep It Simple
Choose the simplest design that solves the actual problem. No speculative
generality, no abstraction without a second caller. Don't force domain richness
where the data is plain CRUD (see `Destination`).

### 5. DRY — Don't Repeat Yourself
No copy-pasted logic, no duplicated literals, no parallel implementations of
the same rule. Extract shared logic to one place. Shared contracts/models go
in `common`.

---

## Naming

- Classes: nouns (`CreateTaskUseCase`, `JooqTaskRepository`, `TaskId`).
- Methods: verbs (`save`, `findById`, `claim`, `succeed`).
- Booleans: `is/has/supports` (`supportsRetry`, `isDeleted`).
- Constants: `UPPER_SNAKE_CASE`.
- No abbreviations or single-letter names (except trivial loop indices).
- Names reveal intent — no comments needed to explain what a name means.

---

## Methods & classes

- Small methods, small classes. Keep classes cohesive; split when a class
  starts to have multiple reasons to change.
- Minimize parameters. 3+ related parameters → a parameter object / value
  object. Use the **Builder pattern** for complex domain objects (per project
  convention).
- No boolean flag parameters that switch behavior — split into two methods.
- Fail fast: validate inputs / invariants at the start with guard clauses.
- Prefer immutability. Value objects (`TaskId`, `DestinationId`) are immutable.

---

## Domain layer specifics

- **Zero framework dependencies.** No Spring, no JOOQ, no Jackson annotations
  in `domain`.
- **No Lombok in the domain layer.** Write explicit constructors/builders.
- Enforce invariants **inside** entities — don't push validation into the
  application layer where it can be bypassed.
- Use type-specific factory methods over generic constructors when some field
  combinations are invalid (e.g. `Schedule.once(...)` vs `Schedule.cron(...)`).
- Model illegal state as unrepresentable, not as a runtime check, where
  practical (especially `Execution` lifecycle transitions).

---

## Error handling

- No swallowed exceptions. No empty `catch` blocks.
- Throw meaningful domain exceptions; don't leak infrastructure exceptions
  through ports.
- Map errors to the documented HTTP codes at the API edge:
  `400` validation, `404` not found / soft-deleted, `409` conflict (e.g.
  deleting an already-deleted task).

---

## Comments

- Code should be self-explanatory; comments explain **why**, not **what**.
- No commented-out code. No noise comments restating the obvious.
- Keep doc-comments where they add real value (public ports, non-obvious
  invariants).

---

## Testing

- Unit tests for domain and use cases — **no DB**.
- Repository integration tests with **Testcontainers + real Postgres**.
- API tests for every endpoint, including edge cases (404 after delete,
  soft-deleted excluded from list, etc.).
- Tests follow the same rules: no magic literals, clear names, one assertion
  focus per test where reasonable.

---

## Quick checklist before finishing any change

- [ ] No inline literals — all in constants/enums/config
- [ ] No method longer than 40 lines
- [ ] Each class/method has a single responsibility
- [ ] No duplicated logic or values (DRY)
- [ ] Simplest solution that works (KISS)
- [ ] Layer boundaries respected; `domain` has no framework imports
- [ ] Invariants enforced in entities; illegal states hard to construct
- [ ] Tests added/updated and passing
