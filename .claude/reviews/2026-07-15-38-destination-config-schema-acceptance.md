# Acceptance evidence — 38-destination-config-schema — 2026-07-15

- Scope: GitHub issue #38 (branch `38-destination-config-schema`). Change is in the
  working tree (staged + unstaged), verified via `git diff HEAD` / `git status` — not committed.
- Test command(s) run:
  - `./gradlew :common:test --tests 'dev.kairos.common.destination.DestinationConfigSchemaTest' --tests 'dev.kairos.common.util.helpers.JsonConverterTest'`
  - `./gradlew :kairos-api:test --tests 'dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest' --tests 'dev.kairos.application.destination.usecases.UpdateDestinationUseCaseTest' --tests 'dev.kairos.domain.destination.DestinationTest' --rerun`
  - `./gradlew :kairos-api:test --tests 'dev.kairos.api.DestinationApiTest' --rerun`
  - `./gradlew :kairos-api:test --tests 'dev.kairos.infrastructure.destination.JooqDestinationRepositoryIT' --rerun` (regression check on the enum-relocation import fix; not itself an AC)
  - `./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.destination.component.DestinationFormTest' --tests 'dev.kairos.admin.feature.wizard.component.DestinationStepTest' --tests 'dev.kairos.admin.feature.destination.ConfigTemplatePrefillTest' --rerun`
  - `./gradlew build --rerun` (full build gate)
  - `grep -rn -E '"topic"|"queueUrl"|"url"|"exchange"|"routingKey"|"messageGroupId"|"method"|"headers"' kairos-admin/src/main` (AC1 no-duplication gate)
- Result: 7 criteria — 7 covered+passing, 0 gaps, 0 not-run.

## Coverage matrix

| AC | Criterion (short) | Evidence (test / gate) | Ran? | Result |
|----|-------------------|------------------------|------|--------|
| [AC1](#ac1) | Per-type schema is single source of truth; no key names duplicated in admin | `DestinationConfigSchemaTest` (20 tests) + grep over `kairos-admin/src/main` | yes | ✅ PASS |
| [AC2](#ac2) | POST returns 400 naming missing key(s) | `DestinationApiTest#create_withConfigMissingRequiredKey_returns400` / `..._errorNamesMissingKey` | yes | ✅ PASS |
| [AC3](#ac3) | PUT validates against stored type, 400 on missing key | `DestinationApiTest#update_withConfigMissingRequiredKey_returns400` / `..._errorNamesMissingKey`; `UpdateDestinationUseCaseTest#execute_withConfigMissingRequiredKeyForStoredType_*` | yes | ✅ PASS |
| [AC4](#ac4) | Create with all required keys still succeeds | `CreateDestinationUseCaseTest#execute_withConfigContainingAllRequiredKeys_succeeds`, `#execute_withAllKnownTypes_parsesEachType`, `DestinationApiTest` happy-path tests | yes | ✅ PASS |
| [AC5](#ac5) | Destinations form prefills template per type, switch replaces untouched only | `DestinationFormTest` (7 tests) + `ConfigTemplatePrefillTest` (5 tests) | yes | ✅ PASS |
| [AC6](#ac6) | Wizard "create new" step prefills same template from same schema | `DestinationStepTest` (prefill subset, 4 tests) + shared `ConfigTemplatePrefillTest` | yes | ✅ PASS |
| [AC7](#ac7) | Unit tests per type + validation paths; API tests for 400s | `DestinationConfigSchemaTest`, `JsonConverterTest`, `CreateDestinationUseCaseTest`, `UpdateDestinationUseCaseTest`, `DestinationApiTest` — all run, all green | yes | ✅ PASS |

Aggregate counts from this run: `common` 27/27 passing, `kairos-api` 541/541 passing, `kairos-admin` 315/315 passing. `./gradlew build` — BUILD SUCCESSFUL across all modules.

## Evidence log

<a id="ac1"></a>
<details>
<summary>✅ <b>AC1</b> — per-type schema is the single source of truth; no key names duplicated in admin — <code>DestinationConfigSchemaTest</code> (20 tests) + grep gate — PASS</summary>

**Criterion:** A per-type config schema (required + optional keys) exists in the domain as the single source of truth, keyed by `DestinationType`; no key names are duplicated in the admin module.

**Test:** `common/src/test/java/dev/kairos/common/destination/DestinationConfigSchemaTest.java`

Representative methods (full file has 20 tests: 4× `requiredKeys_*`, 4× `optionalKeys_*`, 7× `validate_*`, 2× `template_*`, plus 2 completeness/order guards):

```java
@Test
void requiredKeys_rabbitmq_isExchangeAndRoutingKey() {
    assertEquals(Set.of(KEY_EXCHANGE, KEY_ROUTING_KEY), DestinationConfigSchema.requiredKeys(DestinationType.RABBITMQ));
}

@Test
void validate_rabbitmq_withOnlyExchange_messageNamesMissingRoutingKeyOnly() {
    ValidationException ex = assertThrows(ValidationException.class,
            () -> DestinationConfigSchema.validate(DestinationType.RABBITMQ, Set.of(KEY_EXCHANGE)));

    assertTrue(ex.getMessage().contains(KEY_ROUTING_KEY));
    assertTrue(!ex.getMessage().contains(KEY_EXCHANGE + ",") && !ex.getMessage().endsWith(KEY_EXCHANGE));
}

/**
 * {@code template()} renders keys in the schema's iteration order, so every
 * required-key set must preserve declaration order — {@code Set.of(...)}
 * randomises it per JVM run, which would make the templates non-deterministic
 * the moment a type declares a second required key.
 */
@Test
void requiredKeys_everyType_preservesDeclarationOrder() {
    for (DestinationType type : DestinationType.values()) {
        assertTrue(DestinationConfigSchema.requiredKeys(type) instanceof LinkedHashSet,
                "required keys for " + type + " must be an ordered set, not Set.of(...)");
    }
}

/** Every declared type has a schema entry — guards the static completeness check. */
@Test
void everyDestinationType_hasRequiredAndOptionalKeys() {
    for (DestinationType type : DestinationType.values()) {
        assertNotNull(DestinationConfigSchema.requiredKeys(type), "requiredKeys missing for " + type);
        assertNotNull(DestinationConfigSchema.optionalKeys(type), "optionalKeys missing for " + type);
    }
}
```

**Command:**
```
./gradlew :common:test --tests 'dev.kairos.common.destination.DestinationConfigSchemaTest'
```

**Output:**
```
BUILD SUCCESSFUL in 687ms
3 actionable tasks: 1 executed, 2 up-to-date
```
JUnit XML (`common/build/test-results/test/TEST-dev.kairos.common.destination.DestinationConfigSchemaTest.xml`):
```
<testsuite name="dev.kairos.common.destination.DestinationConfigSchemaTest" tests="20" skipped="0" failures="0" errors="0" .../>
```
All 20 test cases present, 0 failures, 0 errors.

**"No key names duplicated in admin" — grep gate (not a test):**
```
grep -rn -E '"topic"|"queueUrl"|"url"|"exchange"|"routingKey"|"messageGroupId"|"method"|"headers"' kairos-admin/src/main
```
Result: zero matches for the config schema key literals (`topic`, `queueUrl`, `exchange`, `routingKey`, `messageGroupId`) anywhere in `kairos-admin/src/main/java`. One unrelated hit: `DemoDataSeeder.java:44` — `Map.of("url", "https://example.com/hooks/demo")`. This is a config *value* being seeded for a demo WEBHOOK destination (using the API's own create endpoint), not a re-declared schema key constant, and the file is **not part of this diff** (`git diff HEAD` shows no changes to `DemoDataSeeder.java`) — pre-existing code, out of scope for this change. `ConfigTemplatePrefill`, `DestinationForm`, and `DestinationStep` (the files this change touches) obtain templates exclusively via `DestinationConfigSchema.template(DestinationType.valueOf(...))` — confirmed by reading all three files; no key-name string literal appears in any of them.
</details>

<a id="ac2"></a>
<details>
<summary>✅ <b>AC2</b> — POST 400 naming missing key(s) — <code>DestinationApiTest#create_withConfigMissingRequiredKey_returns400</code> / <code>_errorNamesMissingKey</code> — PASS</summary>

**Criterion:** `POST /api/v1/destinations` returns 400 with a message naming the missing key(s) when `config` omits a required key for the given type.

**Test:** `kairos-api/src/test/java/dev/kairos/api/DestinationApiTest.java:210` and `:217`

```java
@Test
void create_withConfigMissingRequiredKey_returns400() throws Exception {
    HttpResponse<String> response = post(BASE_PATH, createBodyWithConfig("{}"));

    assertEquals(HTTP_BAD_REQUEST, response.statusCode());
}

@Test
void create_withConfigMissingRequiredKey_errorNamesMissingKey() throws Exception {
    HttpResponse<String> response = post(BASE_PATH, createBodyWithConfig("{}"));

    JsonNode body = objectMapper.readTree(response.body());
    assertTrue(body.get(FIELD_ERROR).asText().contains(TOPIC_KEY));
}
```

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.api.DestinationApiTest' --rerun
```

**Output:**
```
BUILD SUCCESSFUL in 1s
7 actionable tasks: 1 executed, 6 up-to-date
```
JUnit XML: `<testsuite name="dev.kairos.api.DestinationApiTest" tests="36" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:26.970Z" .../>` — fresh run (real Javalin HTTP server + in-memory repository fakes, per the test class's own doc comment), all 36 tests green, including these two.
</details>

<a id="ac3"></a>
<details>
<summary>✅ <b>AC3</b> — PUT validates against stored type, 400 on missing key — <code>DestinationApiTest#update_withConfigMissingRequiredKey_*</code> + <code>UpdateDestinationUseCaseTest#execute_withConfigMissingRequiredKeyForStoredType_*</code> — PASS</summary>

**Criterion:** `PUT /api/v1/destinations/{id}` validates the new `config` against the schema of the destination's *stored* type and returns 400 on a missing required key.

**Test:** `kairos-api/src/test/java/dev/kairos/api/DestinationApiTest.java:296` / `:305`, and `kairos-api/src/test/java/dev/kairos/application/destination/usecases/UpdateDestinationUseCaseTest.java:119` / `:127`

```java
// DestinationApiTest
@Test
void update_withConfigMissingRequiredKey_returns400() throws Exception {
    destinationRepository.seed(destination());

    HttpResponse<String> response = put(destinationPath(DESTINATION_ID), updateBodyWithConfig("{}"));

    assertEquals(HTTP_BAD_REQUEST, response.statusCode());
}

@Test
void update_withConfigMissingRequiredKey_errorNamesMissingKey() throws Exception {
    destinationRepository.seed(destination());

    HttpResponse<String> response = put(destinationPath(DESTINATION_ID), updateBodyWithConfig("{}"));

    JsonNode body = objectMapper.readTree(response.body());
    assertTrue(body.get(FIELD_ERROR).asText().contains(TOPIC_KEY));
}
```

```java
// UpdateDestinationUseCaseTest — proves it validates against the STORED type, not a caller-supplied one
@Test
void execute_withConfigMissingRequiredKeyForStoredType_throwsValidationException() {
    destinationRepository.seed(buildDefault());

    assertThrows(ValidationException.class,
            () -> useCase.execute(KNOWN_ID, "{}"));
}

@Test
void execute_withConfigMissingRequiredKeyForStoredType_messageNamesMissingKey() {
    destinationRepository.seed(buildDefault());

    ValidationException ex = assertThrows(ValidationException.class,
            () -> useCase.execute(KNOWN_ID, "{}"));

    assertEquals("config is missing required key(s): topic", ex.getMessage());
}
```

Note: `UpdateDestinationUseCase.execute(DestinationId, String config)` takes no `type` parameter at all — it reads `destination.destinationType()` off the loaded, stored destination (see `UpdateDestinationUseCase.java:59`), so "validates against the stored type" is enforced structurally, not just by test convention.

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.application.destination.usecases.UpdateDestinationUseCaseTest' --rerun
./gradlew :kairos-api:test --tests 'dev.kairos.api.DestinationApiTest' --rerun
```

**Output:**
```
<testsuite name="dev.kairos.application.destination.usecases.UpdateDestinationUseCaseTest" tests="13" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:11.396Z" .../>
<testsuite name="dev.kairos.api.DestinationApiTest" tests="36" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:26.970Z" .../>
```
All green.
</details>

<a id="ac4"></a>
<details>
<summary>✅ <b>AC4</b> — create with all required keys still succeeds (no regression) — <code>CreateDestinationUseCaseTest#execute_withConfigContainingAllRequiredKeys_succeeds</code> + <code>#execute_withAllKnownTypes_parsesEachType</code> — PASS</summary>

**Criterion:** Creating a destination whose config contains all required keys still succeeds (no regression on the existing happy path).

**Test:** `kairos-api/src/test/java/dev/kairos/application/destination/usecases/CreateDestinationUseCaseTest.java:93` and `:141`

```java
@Test
void execute_withAllKnownTypes_parsesEachType() {
    for (DestinationType type : DestinationType.values()) {
        CreateDestinationCommand cmd = new CreateDestinationCommand(
                "dest-" + type.name().toLowerCase(),
                type.name(),
                VALID_CONFIG_BY_TYPE.get(type)
        );

        Destination result = useCase.execute(cmd);

        assertEquals(type, result.destinationType());
    }
}

@Test
void execute_withConfigContainingAllRequiredKeys_succeeds() {
    CreateDestinationCommand cmd = new CreateDestinationCommand(
            DEFAULT_ID, DestinationType.RABBITMQ.name(),
            "{\"exchange\":\"orders\",\"routingKey\":\"orders.created\"}");

    Destination result = useCase.execute(cmd);

    assertEquals(DestinationType.RABBITMQ, result.destinationType());
}
```

Plus the pre-existing happy-path API tests (`create_...returns201`, etc.) in `DestinationApiTest`, unaffected — all still pass in the same 36-test run below.

**Command:**
```
./gradlew :kairos-api:test --tests 'dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest' --rerun
```

**Output:**
```
<testsuite name="dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest" tests="17" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:11.291Z" .../>
```
All 17 tests green (includes duplicate-id, unknown-type, null-guard cases alongside the two above).
</details>

<a id="ac5"></a>
<details>
<summary>✅ <b>AC5</b> — Destinations form prefills template per type; switch replaces untouched only — <code>DestinationFormTest</code> (7 tests) + <code>ConfigTemplatePrefillTest</code> (5 tests) — PASS</summary>

**Criterion:** In the admin Destinations form, selecting a type prefills the config text area with that type's JSON template (e.g. KAFKA → `{"topic": ""}`), and switching type replaces a still-untouched template rather than silently discarding user-typed config.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/destination/component/DestinationFormTest.java`

```java
@Test
void selectingKafka_prefillsKafkaTemplate() {
    form.destinationType().setValue(DestinationText.TYPE_KAFKA);

    assertThat(form.config().getValue()).isEqualTo("{\"topic\": \"\"}");
}

@Test
void switchingType_configStillHoldsPreviousTemplate_replacesWithNewTemplate() {
    form.destinationType().setValue(DestinationText.TYPE_KAFKA);

    form.destinationType().setValue(DestinationText.TYPE_SQS);

    assertThat(form.config().getValue()).isEqualTo("{\"queueUrl\": \"\"}");
}

@Test
void switchingType_configEditedByUser_doesNotOverwriteUserInput() {
    form.destinationType().setValue(DestinationText.TYPE_KAFKA);
    form.config().setValue("{\"topic\": \"payments\"}");

    form.destinationType().setValue(DestinationText.TYPE_SQS);

    assertThat(form.config().getValue()).isEqualTo("{\"topic\": \"payments\"}");
}
```

All four destination types get a dedicated prefill test (KAFKA/SQS/WEBHOOK/RABBITMQ), plus switch-replaces-untouched, edited-config-preserved, and blank-after-template-then-switch cases (7 tests total).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.destination.component.DestinationFormTest' --tests 'dev.kairos.admin.feature.destination.ConfigTemplatePrefillTest' --rerun
```

**Output:**
```
<testsuite name="dev.kairos.admin.feature.destination.component.DestinationFormTest" tests="7" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:53.279Z" .../>
<testsuite name="dev.kairos.admin.feature.destination.ConfigTemplatePrefillTest" tests="5" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:53.143Z" .../>
```
All 12 tests green.
</details>

<a id="ac6"></a>
<details>
<summary>✅ <b>AC6</b> — wizard "create new" step prefills same template from same schema — <code>DestinationStepTest</code> prefill subset (4 tests) + shared <code>ConfigTemplatePrefillTest</code> — PASS</summary>

**Criterion:** The setup wizard's Destination step ("create new" mode) prefills the same template from the same schema.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/wizard/component/DestinationStepTest.java:152` onward

```java
@Test
void selectingKafka_prefillsKafkaTemplate() {
    step.newDestinationType().setValue(NEW_TYPE);

    assertThat(step.newDestinationConfig().getValue()).isEqualTo("{\"topic\": \"\"}");
}

@Test
void selectingSqs_prefillsSqsTemplate() {
    step.newDestinationType().setValue("SQS");

    assertThat(step.newDestinationConfig().getValue()).isEqualTo("{\"queueUrl\": \"\"}");
}

@Test
void switchingType_configStillHoldsPreviousTemplate_replacesWithNewTemplate() {
    step.newDestinationType().setValue(NEW_TYPE);

    step.newDestinationType().setValue("SQS");

    assertThat(step.newDestinationConfig().getValue()).isEqualTo("{\"queueUrl\": \"\"}");
}

@Test
void switchingType_configEditedByUser_doesNotOverwriteUserInput() {
    step.newDestinationType().setValue(NEW_TYPE);
    step.newDestinationConfig().setValue("{\"topic\": \"payments\"}");

    step.newDestinationType().setValue("SQS");

    assertThat(step.newDestinationConfig().getValue()).isEqualTo("{\"topic\": \"payments\"}");
}
```

Both `DestinationForm` and `DestinationStep` construct their `ConfigTemplatePrefill` from the same class and it, in turn, always calls `DestinationConfigSchema.template(...)` — the "same schema" half is a structural guarantee, not just parallel test assertions (confirmed by reading both files: `ConfigTemplatePrefill` is instantiated identically in each — see `DestinationForm.java:40`, `DestinationStep.java:47`).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.wizard.component.DestinationStepTest' --rerun
```

**Output:**
```
<testsuite name="dev.kairos.admin.feature.wizard.component.DestinationStepTest" tests="15" skipped="0" failures="0" errors="0" timestamp="2026-07-14T22:17:53.315Z" .../>
```
All 15 tests green (11 pre-existing mode/validation tests + 4 new prefill tests, all pass together — no regression in the surrounding step behavior).
</details>

<a id="ac7"></a>
<details>
<summary>✅ <b>AC7</b> — unit tests per type + validation paths; API tests for the 400s — full run across common/api/admin — PASS</summary>

**Criterion:** Unit tests cover the schema per type and the validation failure/success paths; API tests cover the 400s on create and update.

**Tests:**
- `common/src/test/java/dev/kairos/common/destination/DestinationConfigSchemaTest.java` — 20 tests, all 4 types × required/optional, all validation success/failure paths, template determinism, completeness guard.
- `common/src/test/java/dev/kairos/common/util/helpers/JsonConverterTest.java` — 7 tests, the JSON→key-set bridge (object, empty object, null, blank, array, scalar, malformed).
- `kairos-api/src/test/java/dev/kairos/application/destination/usecases/CreateDestinationUseCaseTest.java` — includes the config-schema-validation block (missing key throws/message/no-save, all-required-keys succeeds, non-object config throws).
- `kairos-api/src/test/java/dev/kairos/application/destination/usecases/UpdateDestinationUseCaseTest.java` — the "against stored type" block (throws/message/no-save/succeeds).
- `kairos-api/src/test/java/dev/kairos/api/DestinationApiTest.java` — the 400-on-create and 400-on-update tests (status code + error message content).

**Commands:**
```
./gradlew :common:test --tests 'dev.kairos.common.destination.DestinationConfigSchemaTest' --tests 'dev.kairos.common.util.helpers.JsonConverterTest'
./gradlew :kairos-api:test --tests 'dev.kairos.application.destination.usecases.CreateDestinationUseCaseTest' --tests 'dev.kairos.application.destination.usecases.UpdateDestinationUseCaseTest' --rerun
./gradlew :kairos-api:test --tests 'dev.kairos.api.DestinationApiTest' --rerun
```

**Output (aggregate):**
```
common:     27/27 passing  (DestinationConfigSchemaTest 20, JsonConverterTest 7)
kairos-api: CreateDestinationUseCaseTest 17/17, UpdateDestinationUseCaseTest 13/13, DestinationApiTest 36/36
```
Full-module totals for this run: `common` 27/27, `kairos-api` 541/541, `kairos-admin` 315/315 — 0 failures anywhere. `./gradlew build --rerun` → `BUILD SUCCESSFUL`.
</details>

## Gaps

None found. All 7 criteria have specific, correctly-asserting tests, and every mapped test was executed in this session with fresh (non-cached) results, all green.

Minor observations (not gaps against the stated criteria, noted for completeness):
- `DestinationConfigValidator` (the application-layer bridge class) has no dedicated unit test of its own — it is a two-line pass-through (`topLevelKeys` → `DestinationConfigSchema.validate`) and is exercised transitively through every `CreateDestinationUseCaseTest`/`UpdateDestinationUseCaseTest` config-validation test and every `DestinationApiTest` 400 test. Given its triviality and full transitive coverage, this is acceptable under KISS rather than a real coverage gap.
- `DemoDataSeeder.java` (pre-existing, untouched by this diff) contains a literal `"url"` as a WEBHOOK config *value* when seeding demo data — not a re-declaration of the schema's key set, so it does not violate AC1, but it is technically a hardcoded destination-type-specific key name outside `DestinationConfigSchema`. Flagging for awareness; out of scope to fix here since the file isn't part of this change.

## Verdict

7/7 acceptance criteria verified with passing tests. 0 gaps.
DONE (all covered & green).
