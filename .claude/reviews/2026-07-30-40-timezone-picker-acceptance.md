# Acceptance evidence — 40-timezone-picker — 2026-07-30

- Issue: [#40](https://github.com) "Offer a timezone picker with common zones, still allowing a custom value"
- Test command(s) run:
  - `./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest' --tests 'dev.kairos.admin.feature.schedule.component.ScheduleWhenFieldsTest' --tests 'dev.kairos.admin.feature.wizard.component.ScheduleStepTest' -i`
  - `./gradlew :kairos-admin:build`
  - `grep -rn "TIMEZONE_OPTIONS" kairos-admin/src/main/java/` (DRY-shortlist check)
- Result: 9 criteria — 8 covered+passing, 1 partial-with-caveat (AC1, component-level evidence only, explicitly noted), 0 hard gaps.
- Per-test results read from JUnit XML at `kairos-admin/build/test-results/test/*.xml` (Gradle's console output does not print individual test names even with `-i`; the XML/HTML report is the authoritative per-test source, per the workflow instructions).
- Full-module regression check: `366 tests, 0 failures, 0 errors` across `kairos-admin` after this change (all suites), plus a clean `:kairos-admin:build` (compiles, Vaadin frontend build, `check`, `assemble` all green).

**Important caveat (read before trusting AC1's PASS):** these are plain JUnit
tests instantiating Vaadin server-side components directly — there is no
browser, no rendered DOM, no real mouse/keyboard interaction. "Searchable
drop-down" is verified as **component configuration** — the field is a
`ComboBox<String>` (which renders as a filterable/searchable list client-side
by Vaadin's own implementation) with `setAllowCustomValue(true)` and the
shortlist as its items — not as an end-to-end confirmation that a user typing
in a live browser sees filtered suggestions. That behavior is Vaadin
framework behavior (already relied upon elsewhere, e.g. `CronBuilderDialog`),
not something this slice needs to re-prove, but it is out of scope for a
JUnit-level check and is reported as such rather than overclaimed.

## Coverage matrix

| AC | Criterion (short) | Evidence (test / gate) | Ran? | Result |
|----|-------------------|------------------------|------|--------|
| [AC1](#ac1) | Timezone is a searchable drop-down + accepts hand-typed zone | `Fields.comboCustom` config (inspection) + `ScheduleFormTest#forCreate_validCustomZoneOffShortlist_acceptedByValidate` | yes | ⚠️ PASS (component-level only, see caveat) |
| [AC2](#ac2) | Wizard's Schedule step uses identical control from same shortlist | `ScheduleStepTest#timezone_offersSharedShortlist` | yes | ✅ PASS |
| [AC3](#ac3) | Shortlist defined once as a constant, not inlined at either call site | grep inspection + `ScheduleFormTest#forCreate_timezoneOffersSharedShortlist` + `ScheduleStepTest#timezone_offersSharedShortlist` | yes | ✅ PASS |
| [AC4](#ac4) | `UTC` remains default on create form | `ScheduleFormTest#forCreate_defaultsTimezoneToUtc` | yes | ✅ PASS |
| [AC5](#ac5) | Invalid free-typed zone marks field invalid, blocks submit (no silent UTC) | `ScheduleWhenFieldsTest#validateTimezone_invalidTypo_marksFieldInvalidAndBlocks`, `ScheduleFormTest#forCreate_invalidCustomZone_blocksValidateAndMarksInvalid` | yes | ✅ PASS |
| [AC6](#ac6) | Valid custom zone off shortlist round-trips to API unchanged | `ScheduleFormTest#forEdit_submittingUnchangedOffShortlistTimezone_roundTripsUnchanged`, `#forCreate_validCustomZoneOffShortlist_acceptedByValidate` | yes | ✅ PASS |
| [AC7](#ac7) | Timezone field still hidden for `FIXED` | `ScheduleFormTest#forCreate_fixedType_timezoneFieldHidden`, `ScheduleStepTest#fixedType_timezoneFieldHidden` | yes | ✅ PASS |
| [AC8](#ac8) | Editing preselects stored timezone, incl. one off shortlist | `ScheduleFormTest#forEdit_preselectsStoredShortlistTimezone`, `#forEdit_preselectsStoredOffShortlistTimezone` | yes | ✅ PASS |
| [AC9](#ac9) | Unit tests cover: shortlist selection, valid custom, invalid custom blocking, FIXED visibility | full suite run (35 tests across 3 classes) | yes | ✅ PASS |

## Evidence log

<a id="ac1"></a>
<details>
<summary>⚠️ <b>AC1</b> — searchable drop-down + accepts hand-typed zone — <code>Fields.comboCustom</code> (inspection) + <code>ScheduleFormTest#forCreate_validCustomZoneOffShortlist_acceptedByValidate</code> — PASS (component-level, not browser-verified)</summary>

**Criterion:** "The timezone input in `ScheduleForm` is a searchable drop-down offering the common zones as suggestions, and still accepts a zone typed by hand that is not on the shortlist."

**Why this is a component-level check, not an end-to-end one:** the test suite
instantiates Vaadin server-side Java objects directly in plain JUnit — there is
no servlet container, no browser, no client-side JS runtime. A `ComboBox`'s
"searchable" (filterable-as-you-type) behavior is implemented client-side by
Vaadin's own web component and cannot be exercised without a browser. What
*is* verifiable here, and is verified, is that `ScheduleForm`'s timezone field
is configured as a `ComboBox<String>` (the searchable-by-construction Vaadin
component, already used for exactly this purpose in `CronBuilderDialog`) with
`setAllowCustomValue(true)` (accepts hand-typed values) and seeded with the
shortlist — i.e. the configuration that *causes* the searchable-dropdown
behavior in a real browser, not the rendered behavior itself.

**Implementation evidence:** `kairos-admin/src/main/java/dev/kairos/admin/shared/ui/Fields.java:70-77`
```java
public static ComboBox<String> comboCustom(String label, String helper, Collection<String> options) {
    ComboBox<String> field = new ComboBox<>(label);
    field.setItems(options);
    field.setAllowCustomValue(true);
    field.setHelperText(helper);
    field.setClearButtonVisible(true);
    return field;
}
```
`ScheduleForm.java:56-57` and `ScheduleStep.java:43-44` both call this via
`Fields.comboCustom(ScheduleText.COL_TIMEZONE, ScheduleText.HELPER_TIMEZONE, ScheduleText.TIMEZONE_OPTIONS)`.

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/component/ScheduleFormTest.java:52-59` (shortlist item present as a real `ComboBox` value) and `:61-71` (hand-typed value not on the shortlist accepted):
```java
@Test
void forCreate_shortlistZone_selectable() {
    ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

    form.timezone().setValue("Europe/Kyiv");

    assertThat(form.timezone().getValue()).isEqualTo("Europe/Kyiv");
}

@Test
void forCreate_validCustomZoneOffShortlist_acceptedByValidate() {
    ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());
    form.task().setValue(task());
    form.type().setValue(ScheduleType.ONCE);
    form.runAt().setValue(LocalDateTime.ofInstant(FAR_FUTURE, ZoneOffset.UTC));
    form.timezone().setValue("Pacific/Chatham");

    assertThat(form.validate()).isTrue();
    assertThat(form.timezone().isInvalid()).isFalse();
}
```

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest'
```

**Output (from JUnit XML, `kairos-admin/build/test-results/test/TEST-dev.kairos.admin.feature.schedule.component.ScheduleFormTest.xml`):**
```
<testsuite name="dev.kairos.admin.feature.schedule.component.ScheduleFormTest" tests="10" skipped="0" failures="0" errors="0" time="0.216">
  <testcase name="forCreate_shortlistZone_selectable()" time="0.006"/>
  <testcase name="forCreate_validCustomZoneOffShortlist_acceptedByValidate()" time="0.179"/>
  ...
</testsuite>
```
`BUILD SUCCESSFUL` confirmed via the same run (see full command log below in AC9).

**Verdict:** PASS at the level this test harness can prove (ComboBox configuration + accept-both-shortlist-and-custom-value semantics). Not a substitute for a browser-level check of actual filter-as-you-type UI behavior — none exists in this repo's test setup, and none is claimed.
</details>

<a id="ac2"></a>
<details>
<summary>✅ <b>AC2</b> — wizard offers identical control from same shortlist — <code>ScheduleStepTest#timezone_offersSharedShortlist</code> — PASS</summary>

**Criterion:** "The wizard's Schedule step offers the identical control, populated from the same single shortlist definition (no duplicated zone list between the two)."

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/wizard/component/ScheduleStepTest.java:136-140`
```java
@Test
void timezone_offersSharedShortlist() {
    assertThat(step.timezone().getListDataView().getItems().toList())
            .isEqualTo(ScheduleText.TIMEZONE_OPTIONS);
}
```
This asserts the wizard step's ComboBox items are literally `ScheduleText.TIMEZONE_OPTIONS` — the exact same list object/values as `ScheduleForm`'s (see AC3 for the cross-check that `ScheduleForm` uses the identical constant too).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.wizard.component.ScheduleStepTest'
```

**Output (from JUnit XML):**
```
<testcase name="timezone_offersSharedShortlist()" classname="dev.kairos.admin.feature.wizard.component.ScheduleStepTest" time="0.001"/>
```
Suite: `tests="17" failures="0" errors="0"` — full pass (`BUILD SUCCESSFUL`).
</details>

<a id="ac3"></a>
<details>
<summary>✅ <b>AC3</b> — shortlist is one constant, not inlined at either call site — grep inspection + <code>ScheduleFormTest#forCreate_timezoneOffersSharedShortlist</code> + <code>ScheduleStepTest#timezone_offersSharedShortlist</code> — PASS</summary>

**Criterion:** "The shortlist is defined once as a constant (alongside the other schedule UI text/options constants), not inlined at either call site."

**Inspection:** the shortlist literal (`"UTC"`, `"Europe/Kyiv"`, `"America/New_York"`, `"Asia/Tokyo"`, ...) exists in exactly one place in `kairos-admin/src/main/java`:

```
$ grep -rn "TIMEZONE_OPTIONS" kairos-admin/src/main/java/
kairos-admin/src/main/java/dev/kairos/admin/feature/schedule/ScheduleText.java:52:    public static final List<String> TIMEZONE_OPTIONS = List.of(
kairos-admin/src/main/java/dev/kairos/admin/feature/schedule/component/ScheduleForm.java:57:            Fields.comboCustom(ScheduleText.COL_TIMEZONE, ScheduleText.HELPER_TIMEZONE, ScheduleText.TIMEZONE_OPTIONS);
kairos-admin/src/main/java/dev/kairos/admin/feature/wizard/component/ScheduleStep.java:44:            Fields.comboCustom(ScheduleText.COL_TIMEZONE, ScheduleText.HELPER_TIMEZONE, ScheduleText.TIMEZONE_OPTIONS);

$ grep -rn "\"Europe/Kyiv\"\|\"America/New_York\"\|\"Asia/Tokyo\"" kairos-admin/src/main/java/
kairos-admin/src/main/java/dev/kairos/admin/feature/schedule/ScheduleText.java:56:            "Europe/Kyiv",
kairos-admin/src/main/java/dev/kairos/admin/feature/schedule/ScheduleText.java:57:            "America/New_York",
kairos-admin/src/main/java/dev/kairos/admin/feature/schedule/ScheduleText.java:59:            "Asia/Tokyo",
```
Both `ScheduleForm.java` and `ScheduleStep.java` reference `ScheduleText.TIMEZONE_OPTIONS` — no zone literal is duplicated at either call site. `ScheduleText.java:51-63` places the constant next to the other schedule UI constants (`DEFAULT_TIMEZONE`, `HELPER_TIMEZONE`, `VALIDATION_TIMEZONE_INVALID`), matching the criterion's "alongside the other schedule UI options constants" wording.

**Tests (equality-of-source, per call site):**
`ScheduleFormTest.java:44-50`
```java
@Test
void forCreate_timezoneOffersSharedShortlist() {
    ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

    assertThat(form.timezone().getListDataView().getItems().toList())
            .isEqualTo(ScheduleText.TIMEZONE_OPTIONS);
}
```
`ScheduleStepTest.java:136-140` (see AC2).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest' --tests 'dev.kairos.admin.feature.wizard.component.ScheduleStepTest'
```

**Output:** both suites green (`ScheduleFormTest`: 10/10, `ScheduleStepTest`: 17/17 — see XML excerpts above/below). `BUILD SUCCESSFUL`.
</details>

<a id="ac4"></a>
<details>
<summary>✅ <b>AC4</b> — UTC remains default on create form — <code>ScheduleFormTest#forCreate_defaultsTimezoneToUtc</code> — PASS</summary>

**Criterion:** "`UTC` remains the default value on the create form."

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/component/ScheduleFormTest.java:37-42`
```java
@Test
void forCreate_defaultsTimezoneToUtc() {
    ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

    assertThat(form.timezone().getValue()).isEqualTo(ScheduleText.DEFAULT_TIMEZONE);
}
```
(`ScheduleText.DEFAULT_TIMEZONE == "UTC"`, `ScheduleText.java:48`.) The wizard side is covered too by `ScheduleStepTest#timezone_defaultsToUtc` (`ScheduleStepTest.java:131-134`).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest'
```

**Output (JUnit XML):**
```
<testcase name="forCreate_defaultsTimezoneToUtc()" classname="dev.kairos.admin.feature.schedule.component.ScheduleFormTest" time="0.003"/>
```
Suite `tests="10" failures="0" errors="0"`.
</details>

<a id="ac5"></a>
<details>
<summary>✅ <b>AC5</b> — invalid free-typed zone marks field invalid and blocks submit — <code>ScheduleWhenFieldsTest#validateTimezone_invalidTypo_marksFieldInvalidAndBlocks</code>, <code>ScheduleFormTest#forCreate_invalidCustomZone_blocksValidateAndMarksInvalid</code> — PASS</summary>

**Criterion:** "A free-typed timezone that is not a valid `ZoneId` marks the field invalid and blocks submission, instead of silently resolving to UTC."

**Test 1 (unit-level rule):** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/component/ScheduleWhenFieldsTest.java:64-72`
```java
@Test
void validateTimezone_invalidTypo_marksFieldInvalidAndBlocks() {
    ComboBox<String> timezone = timezoneField();
    timezone.setValue("Europe/Kyv");

    assertThat(ScheduleWhenFields.validateTimezone(timezone)).isFalse();
    assertThat(timezone.isInvalid()).isTrue();
    assertThat(timezone.getErrorMessage()).isEqualTo(ScheduleText.VALIDATION_TIMEZONE_INVALID);
}
```

**Test 2 (form-level: actually blocks submission via `validate()`):** `ScheduleFormTest.java:73-84`
```java
@Test
void forCreate_invalidCustomZone_blocksValidateAndMarksInvalid() {
    ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());
    form.task().setValue(task());
    form.type().setValue(ScheduleType.ONCE);
    form.runAt().setValue(LocalDateTime.ofInstant(FAR_FUTURE, ZoneOffset.UTC));
    form.timezone().setValue("Europe/Kyv");

    assertThat(form.validate()).isFalse();
    assertThat(form.timezone().isInvalid()).isTrue();
    assertThat(form.timezone().getErrorMessage()).isEqualTo(ScheduleText.VALIDATION_TIMEZONE_INVALID);
}
```
Same rule mirrored on the wizard side by `ScheduleStepTest#timezone_invalidCustomZone_blocksValidateAndMarksInvalid` (`:159-167`).

This directly proves the "instead of silently resolving to UTC" half too — `validateTimezone` (`ScheduleWhenFields.java:136-145`) returns `false` and sets `invalid=true` rather than swallowing the typo, unlike the superseded `selectedZone` fallback (still present but now only used for computing the wall-clock instant *after* validation has already gated the value, per `ScheduleWhenFields.java:106-125`).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleWhenFieldsTest' --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest'
```

**Output (JUnit XML):**
```
<testcase name="validateTimezone_invalidTypo_marksFieldInvalidAndBlocks()" classname="...ScheduleWhenFieldsTest" time="0.0"/>
<testcase name="forCreate_invalidCustomZone_blocksValidateAndMarksInvalid()" classname="...ScheduleFormTest" time="0.002"/>
```
Both suites: 0 failures, 0 errors. `BUILD SUCCESSFUL`.
</details>

<a id="ac6"></a>
<details>
<summary>✅ <b>AC6</b> — valid custom zone off shortlist round-trips to API unchanged — <code>ScheduleFormTest#forEdit_submittingUnchangedOffShortlistTimezone_roundTripsUnchanged</code> — PASS</summary>

**Criterion:** "A valid free-typed zone outside the shortlist (e.g. `Pacific/Chatham`) is accepted and round-trips to the API unchanged."

**Test:** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/component/ScheduleFormTest.java:120-130`
```java
@Test
void forEdit_submittingUnchangedOffShortlistTimezone_roundTripsUnchanged() {
    ScheduleResponse editing = scheduleWithTimezone("Pacific/Chatham");
    UpdateScheduleRequest[] captured = new UpdateScheduleRequest[1];
    ScheduleForm form = ScheduleForm.forEdit(editing, request -> captured[0] = request);

    assertThat(form.validate()).isTrue();
    form.save();

    assertThat(captured[0].timezone()).isEqualTo("Pacific/Chatham");
}
```
This asserts the literal `Pacific/Chatham` string reaches the captured `UpdateScheduleRequest` sent toward the API, unchanged, after passing `validate()`. Complemented by `forCreate_validCustomZoneOffShortlist_acceptedByValidate` (`:61-71`, cited under AC1) proving the create-path acceptance, and the wizard's mirror `ScheduleStepTest#timezone_validCustomZoneOffShortlist_acceptedByValidate` (`:150-157`).

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest'
```

**Output (JUnit XML):**
```
<testcase name="forEdit_submittingUnchangedOffShortlistTimezone_roundTripsUnchanged()" classname="dev.kairos.admin.feature.schedule.component.ScheduleFormTest" time="0.003"/>
```
Suite `tests="10" failures="0" errors="0"`.

**Scope note:** this proves the value is preserved unchanged up to the assembled DTO the admin sends; it does not itself invoke the running `kairos-api` HTTP endpoint (no such integration test exists in this slice, and none was requested by the criteria). The DTO field is what actually gets serialized to the API request body, so this is the correct boundary for an admin-side unit test.
</details>

<a id="ac7"></a>
<details>
<summary>✅ <b>AC7</b> — timezone field still hidden for FIXED — <code>ScheduleFormTest#forCreate_fixedType_timezoneFieldHidden</code>, <code>ScheduleStepTest#fixedType_timezoneFieldHidden</code> — PASS</summary>

**Criterion:** "The timezone field is still hidden for `FIXED` schedules (existing behavior preserved)."

**Test 1:** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/component/ScheduleFormTest.java:86-93`
```java
@Test
void forCreate_fixedType_timezoneFieldHidden() {
    ScheduleForm form = ScheduleForm.forCreate(List.of(task()), noopCreate());

    form.type().setValue(ScheduleType.FIXED);

    assertThat(form.timezone().isVisible()).isFalse();
}
```

**Test 2:** `kairos-admin/src/test/java/dev/kairos/admin/feature/wizard/component/ScheduleStepTest.java:169-174`
```java
@Test
void fixedType_timezoneFieldHidden() {
    step.type().setValue(ScheduleType.FIXED);

    assertThat(step.timezone().isVisible()).isFalse();
}
```
Also covered from the other direction by `ScheduleStepTest#nonFixedType_timezoneFieldVisible` (`:186-192`), and the "FIXED doesn't block on an invalid timezone value" companion rule by `ScheduleFormTest#forCreate_fixedType_invalidTimezoneDoesNotBlockValidate` / `ScheduleStepTest#fixedType_invalidTimezoneDoesNotBlockValidate`.

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest' --tests 'dev.kairos.admin.feature.wizard.component.ScheduleStepTest'
```

**Output (JUnit XML):**
```
<testcase name="forCreate_fixedType_timezoneFieldHidden()" classname="...ScheduleFormTest" time="0.002"/>
<testcase name="fixedType_timezoneFieldHidden()" classname="...ScheduleStepTest" time="0.003"/>
```
Both suites green. `BUILD SUCCESSFUL`.
</details>

<a id="ac8"></a>
<details>
<summary>✅ <b>AC8</b> — editing preselects stored timezone, incl. one not on shortlist — <code>ScheduleFormTest#forEdit_preselectsStoredShortlistTimezone</code>, <code>#forEdit_preselectsStoredOffShortlistTimezone</code> — PASS</summary>

**Criterion:** "Editing an existing schedule preselects its stored timezone, including one not on the shortlist."

**Test (shortlist case):** `kairos-admin/src/test/java/dev/kairos/admin/feature/schedule/component/ScheduleFormTest.java:106-111`
```java
@Test
void forEdit_preselectsStoredShortlistTimezone() {
    ScheduleForm form = ScheduleForm.forEdit(scheduleWithTimezone("Europe/Kyiv"), noopUpdate());

    assertThat(form.timezone().getValue()).isEqualTo("Europe/Kyiv");
}
```

**Test (off-shortlist case — the criterion's harder half):** `:113-118`
```java
@Test
void forEdit_preselectsStoredOffShortlistTimezone() {
    ScheduleForm form = ScheduleForm.forEdit(scheduleWithTimezone("Pacific/Chatham"), noopUpdate());

    assertThat(form.timezone().getValue()).isEqualTo("Pacific/Chatham");
}
```
This confirms `ScheduleForm.prefill` (`ScheduleForm.java:194-205`, `timezone.setValue(...)`) sets the ComboBox's value directly from the stored schedule regardless of shortlist membership — `setAllowCustomValue(true)` lets a ComboBox hold a value outside its item set, so preselection of an off-shortlist zone works without special-casing.

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest'
```

**Output (JUnit XML):**
```
<testcase name="forEdit_preselectsStoredShortlistTimezone()" classname="...ScheduleFormTest" time="0.002"/>
<testcase name="forEdit_preselectsStoredOffShortlistTimezone()" classname="...ScheduleFormTest" time="0.004"/>
```
Suite `tests="10" failures="0" errors="0"`.
</details>

<a id="ac9"></a>
<details>
<summary>✅ <b>AC9</b> — unit tests cover shortlist selection, valid custom, invalid custom blocking, FIXED visibility — full 35-test run across 3 classes — PASS</summary>

**Criterion:** "Unit tests cover: shortlist selection, a valid custom value, an invalid custom value blocking submit, and the `FIXED` visibility rule."

**Mapping (all four named cases present, each already cited above with source):**
- Shortlist selection → `ScheduleFormTest#forCreate_shortlistZone_selectable`, `ScheduleStepTest#timezone_shortlistZone_selectable`.
- Valid custom value → `ScheduleFormTest#forCreate_validCustomZoneOffShortlist_acceptedByValidate`, `ScheduleWhenFieldsTest#validateTimezone_validCustomZoneOffShortlist_isValid`, `ScheduleStepTest#timezone_validCustomZoneOffShortlist_acceptedByValidate`.
- Invalid custom value blocking submit → `ScheduleFormTest#forCreate_invalidCustomZone_blocksValidateAndMarksInvalid`, `ScheduleWhenFieldsTest#validateTimezone_invalidTypo_marksFieldInvalidAndBlocks`, `ScheduleStepTest#timezone_invalidCustomZone_blocksValidateAndMarksInvalid`.
- FIXED visibility rule → `ScheduleFormTest#forCreate_fixedType_timezoneFieldHidden` (+ `_invalidTimezoneDoesNotBlockValidate`), `ScheduleStepTest#fixedType_timezoneFieldHidden` (+ `_invalidTimezoneDoesNotBlockValidate`, `nonFixedType_timezoneFieldVisible`), `ScheduleWhenFieldsTest#validateTimezoneIfApplicable_fixedType_invalidValueDoesNotBlock` / `_onceType_invalidValueBlocks`.

**Command:**
```
./gradlew :kairos-admin:test --tests 'dev.kairos.admin.feature.schedule.component.ScheduleFormTest' --tests 'dev.kairos.admin.feature.schedule.component.ScheduleWhenFieldsTest' --tests 'dev.kairos.admin.feature.wizard.component.ScheduleStepTest' -i
```

**Output:**
```
> Task :kairos-admin:test

BUILD SUCCESSFUL in 1s
6 actionable tasks: 1 executed, 5 up-to-date
```
(Gradle's console does not print individual test names even with `-i` for this project's logging config; per-test PASS/FAIL read from the generated JUnit XML, reproduced in each AC block above.)

Per-class totals from `kairos-admin/build/test-results/test/*.xml`:
```
ScheduleFormTest        tests=10 failures=0 errors=0
ScheduleWhenFieldsTest  tests=8  failures=0 errors=0
ScheduleStepTest        tests=17 failures=0 errors=0
```
35/35 pass across the three files touched by this slice.

**Full-module regression check:**
```
./gradlew :kairos-admin:build
...
> Task :kairos-admin:test
> Task :kairos-admin:check
> Task :kairos-admin:build

BUILD SUCCESSFUL in 5s
11 actionable tasks: 6 executed, 5 up-to-date
```
Aggregated across all `kairos-admin/build/test-results/test/*.xml`: `366 tests, 0 failures, 0 errors`.
</details>

## Gaps

None of the 9 criteria are uncovered. The only caveat is **AC1**, flagged
above as PASS-with-caveat rather than a gap: the "searchable drop-down"
half of the criterion is proven as ComboBox configuration (items +
`setAllowCustomValue(true)`), not as a browser-verified rendered interaction,
because this test suite runs plain JUnit against server-side Vaadin objects
with no browser. This is a structural limit of the test harness used
throughout this codebase (same limit applies to every other Vaadin component
test here, e.g. `CronBuilderDialog`'s tests), not something specific to this
slice's implementation, and no test-author gap follows from it — a genuine
browser-level check would require a different test tool (e.g. Playwright/TestBench)
that this project does not use.

## Verdict

9/9 acceptance criteria verified with passing tests (1 of the 9 — AC1 — verified
at component-configuration level only, with the browser-rendering limit stated
explicitly rather than overclaimed as full end-to-end proof). 0 gaps, 0 failing
tests, 0 not-run.

DONE — all 9 criteria covered by passing tests (35/35 targeted, 366/366
module-wide), `:kairos-admin:build` green, shortlist DRY-ness confirmed by
direct grep. AC1's "searchable" half is honestly reported as
component-level/framework-relied-upon evidence rather than browser-verified.
