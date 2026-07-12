# Dashboard / Home Landing Page (Admin UI)

> **Reconciled with shipped code (2026-07-12).** This spec originally described
> a static dashboard (four fixed stat cards + a non-clickable "Platform
> Features" grid, all hardcoded, explicitly with no `DashboardService`). The
> implementation diverged during build-out into a live, user-customizable
> dashboard. Per project decision, **the code is the source of truth** — the
> sections below now describe the shipped behavior. The original static-mockup
> design is no longer accurate and has been removed rather than kept as
> "future work".

## Problem

The Kairos admin console used to drop the operator straight onto the Tasks
grid — there was no overview screen, no sense of "what's going on" at a
glance, and no guided entry point into the console's main actions. Operators
want a **home dashboard**: a summary of the system's health (active/total
tasks and schedules, ownership breakdowns) plus fast paths into the main
workflows.

The engine / execution side isn't wired up yet, so metrics that depend on
executions (throughput, success rate, next run, failures) don't exist yet.
Metrics that **can** be derived from tasks and schedules (which already exist
via the API) are computed live; the rest ship as clearly labeled preview
cards with placeholder data, so the layout and customization mechanics don't
need reworking once executions land.

**For whom:** operators / admins landing in the Kairos admin console.

## Scope

**In:**
- A new **Dashboard** screen (`DashboardView`) registered under `MainLayout`,
  mapped to the **empty route** `""` so it is the console's **landing page**,
  and added as the **first** `SideNavItem` in `MainLayout` (above Tasks).
  `TaskView` moved off the empty route to the explicit route `tasks`
  (`TaskRoutes.TASKS`).
- **`DashboardService`** — computes real `DashboardStats` from the tasks and
  schedules already exposed by `TaskService` / `ScheduleService`: total/active
  task counts, total/active schedule counts, a "top services by task count"
  leaderboard, and a "most scheduled tasks" leaderboard (top 5 each,
  `DashboardStats.MAX_LEADERBOARD_SIZE`). On any failure it logs the error and
  returns `DashboardStats.empty()` so the view still renders.
- **A customizable card grid**, not a fixed row. Ten cards, identified by the
  `CardId` enum:
  - Real data (from `DashboardService`): `ACTIVE_TASKS`, `TOTAL_TASKS`,
    `ACTIVE_SCHEDULES`, `TOTAL_SCHEDULES`, `SERVICE_LEADERBOARD`,
    `SCHEDULE_LEADERBOARD`.
  - Preview cards (placeholder data, no execution data exists yet):
    `NEXT_RUN`, `COMPLETED_TODAY`, `SUCCESS_RATE`, `FAILURE_LEADERBOARD`.
    Preview cards render a **"Preview" badge** (`DashboardText.PREVIEW_BADGE`)
    so the placeholder nature is visible in the UI, not just in code comments.
  - `DashboardCardMeta` maps each `CardId` to its display title and whether
    it's a preview card — the single place the view and the settings dialog
    both read from, so they can't drift apart.
- **Per-user layout customization**, persisted client-side:
  - `DashboardPrefs` holds the current card order and the set of hidden cards,
    loaded from / saved to browser `localStorage` (`WebStorage`) under the
    keys `kairos.dashboard.order` and `kairos.dashboard.hidden`, as
    comma-separated `CardId` names. Unknown/stale names are skipped with a
    warning log; any card new since the value was stored is appended so it's
    never silently lost.
  - **Drag-to-reorder**: every rendered card is both a Vaadin `DragSource` and
    `DropTarget`; dropping one card onto another moves it to that position
    (`DashboardPrefs.reorder`) and persists immediately.
  - **`DashboardSettingsDialog`** ("Customize dashboard", opened via a
    `Customize` button in the header): one checkbox per `CardId` to toggle
    visibility, applied and persisted live (the dashboard re-renders without a
    page reload), plus a **Reset to defaults** action
    (`DashboardPrefs.resetToDefaults()` — full card set, `CardId` declaration
    order, nothing hidden).
- **Card components** (`feature/dashboard/component/`):
  - `DashboardCard` — abstract base: shared glass-card surface (via
    `StyleConfig` + `Tokens`), optional "Preview" badge, optional clickable
    affordance.
  - `StatCard` — icon badge + large value + caption; clickable when an
    `onClick` action is supplied (stat CTAs navigate via
    `UI.getCurrent().navigate(ViewClass.class, ...)` to `TaskView` /
    `ScheduleView` — no literal URLs; `ACTIVE_TASKS` navigates to
    `TaskView` pre-filtered via `TaskRoutes.QUERY_STATUS`).
  - `LeaderboardCard` — title + ranked label/count rows, with an empty-state
    message (`DashboardText.LEADERBOARD_EMPTY`) when there are no rows.
- Kairos "Apple-style" look: cards built from `StyleConfig` + `Tokens`
  (glass/translucent surface, radius, shadow, gradient accents), matching the
  existing views. Responsive CSS-grid card layout
  (`Tokens.GRID_CARDS_STATS`).
- All copy (titles, captions, badges, dialog labels, preview placeholder
  values) in `DashboardText` — no literals in the components.

**Out:**
- **The setup wizard.** A guided "connect destination → create task →
  configure schedule" onboarding wizard is a **separate future spec** — not
  built here.
- **Real execution-derived metrics.** `NEXT_RUN`, `COMPLETED_TODAY`,
  `SUCCESS_RATE`, and `FAILURE_LEADERBOARD` remain hardcoded preview values in
  `DashboardText` (each with a `// TODO` marking the eventual real stats
  source) until the engine/executions are wired. They are visually marked as
  previews, not silently presented as live data.
- **Server-side persistence of layout preferences.** Card order/visibility is
  stored in browser `localStorage` only — no per-user backend preference,
  no new `common` DTO, no new endpoint for this.
- Any `domain` / `application` / `infrastructure` / API / schema change. This
  is `kairos-admin` UI only.
- A "Platform Features" descriptive section. This was in the original design
  but was never built — there is no `FeatureCard` and no feature-grid section
  in the shipped code.
- Charts, time-range pickers, auto-refresh.

## Design

**Module / layer:** `kairos-admin` only. Code lives in the feature package
`dev.kairos.admin.feature.dashboard`, mirroring the `task` / `schedule` /
`destination` feature layout.

**Files:**

```
feature/dashboard/
  DashboardRoutes.java        # HOME = "" (landing page), PAGE_TITLE
  DashboardText.java          # every label, caption, badge, dialog string, preview placeholder — no literals in components
  DashboardStats.java         # record: totalTasks, activeTasks, totalSchedules, activeSchedules,
                               #   serviceRanking, scheduleRanking (List<Ranked>); Ranked(label, count); empty()
  CardId.java                 # enum: ACTIVE_TASKS, TOTAL_TASKS, ACTIVE_SCHEDULES, TOTAL_SCHEDULES,
                               #   SERVICE_LEADERBOARD, SCHEDULE_LEADERBOARD, NEXT_RUN, COMPLETED_TODAY,
                               #   SUCCESS_RATE, FAILURE_LEADERBOARD — persisted by name, ordinal = default order
  DashboardCardMeta.java       # CardId -> title / isPreview lookup shared by view + settings dialog
  DashboardService.java        # @Service; computes DashboardStats from TaskService + ScheduleService; empty() on error
  DashboardPrefs.java           # per-user order + hidden set; WebStorage load/save; reorder/setVisible/resetToDefaults
  DashboardView.java            # @Route("") @PageTitle — header + customizable card grid, drag-to-reorder, settings entry
  component/
    DashboardCard.java         # abstract shared card surface + optional preview badge + clickable affordance
    StatCard.java              # icon + value + caption (+ optional onClick)
    LeaderboardCard.java       # title + ranked rows (+ empty state)
    DashboardSettingsDialog.java  # checkbox per card to toggle visibility; reset to defaults
```

- **`DashboardView`** — `@Route(value = DashboardRoutes.HOME /* "" */, layout =
  MainLayout.class)`, `@PageTitle(DashboardRoutes.PAGE_TITLE)`. Extends
  `VerticalLayout`; padding/gap via `StyleConfig` + `Tokens` (same pattern as
  `ScheduleView`). Constructor loads `DashboardStats` synchronously via
  `DashboardService.load()`, then asynchronously loads `DashboardPrefs` from
  browser storage and triggers the first `render()` once prefs arrive (prefs
  reads are async because `WebStorage` requires a client round-trip). `render()`
  clears and rebuilds the card grid from `prefs.order()` filtered to
  `prefs.isVisible(...)`, wiring drag source/drop target on each card.
  `buildCard(CardId)` is a `switch` that dispatches each `CardId` to the right
  `StatCard`/`LeaderboardCard` construction — real cards read from `stats`,
  preview cards read placeholder values from `DashboardText`.
- **`DashboardService`** — orchestrates `TaskService.list()` and
  `ScheduleService.listForTasks(...)`, derives counts and rankings
  (`Collectors.groupingBy` + sort-by-count-descending, capped at
  `MAX_LEADERBOARD_SIZE`). Catches `RuntimeException`, logs at `ERROR`, returns
  `DashboardStats.empty()` — the view never breaks on a backend hiccup.
- **`DashboardPrefs`** — immutable-shaped mutation API (`setVisible`,
  `reorder`, `resetToDefaults`) plus explicit `save()`/`load()` to browser
  storage; callers decide when to persist. Logs at `DEBUG` on load/save.
- **`DashboardCardMeta`** — static lookup only (`title`, `isPreview`); no
  behavior, just shared metadata so the view and `DashboardSettingsDialog`
  can't disagree on a card's title or preview status.
- **`StatCard` / `LeaderboardCard` / `DashboardCard`** — kept in the dashboard
  feature package (their only consumer is the dashboard). Styled entirely
  through `StyleConfig` + `Tokens` — glass/translucent surface, radius,
  shadow tokens already present. Accent colour per stat card via existing
  colour tokens (primary / success / warning).
- **`DashboardText`** — all strings and preview placeholder values, in the
  style of `ScheduleText` / `LayoutText`. Preview placeholder values carry a
  `// TODO: replace with a real stats source ... once the engine is wired`
  comment at the point they're defined.

**Navigation & landing page:**
- `NAV_DASHBOARD` in `LayoutText`, rendered as the **first** `SideNavItem` in
  `MainLayout.createSideNav()` (`VaadinIcon.DASHBOARD`), followed by Tasks,
  Schedules, Destinations.
- `DashboardView` owns route `""`. `TaskView`'s route moved to the explicit
  segment `tasks` (`TaskRoutes.TASKS`) so the dashboard is reachable at the
  console root without a route clash.

**Styling / conventions:**
- All CSS via `StyleConfig` + `Tokens` — no inline CSS string literals in
  components. The responsive card grid uses CSS grid via `StyleConfig`
  (`Tokens.DISPLAY_GRID` + `Tokens.GRID_CARDS_STATS`), no literal grid
  strings in the view.
- No Lombok. SLF4J logging at `DEBUG` for prefs load/save and card reorder,
  `ERROR` for stats-loading failure in `DashboardService`.
- Methods ≤ 40 lines; SRP — `DashboardService` computes stats,
  `DashboardPrefs` owns layout persistence, `DashboardCardMeta` owns
  title/preview lookup, `DashboardView` only assembles and renders.

## Acceptance criteria

- [x] Visiting the console root (`""`) shows the **Dashboard** as the landing
      page; a **Dashboard** nav item appears **first** in the side nav
      (`MainLayout.createSideNav()`), and `TaskView` is reachable at the
      explicit route `tasks`.
- [x] `DashboardService.load()` returns real counts (`totalTasks`,
      `activeTasks`, `totalSchedules`, `activeSchedules`) and rankings
      (`serviceRanking`, `scheduleRanking`, capped at
      `DashboardStats.MAX_LEADERBOARD_SIZE`) computed from `TaskService` /
      `ScheduleService`; on either service throwing, it returns
      `DashboardStats.empty()` without propagating the exception (covered by
      `DashboardServiceTest`).
- [x] The dashboard renders a card per visible `CardId` in `DashboardPrefs`
      order; `ACTIVE_TASKS`, `TOTAL_TASKS`, `ACTIVE_SCHEDULES`,
      `TOTAL_SCHEDULES` render as clickable `StatCard`s reading live
      `DashboardStats`; `SERVICE_LEADERBOARD` and `SCHEDULE_LEADERBOARD`
      render as `LeaderboardCard`s reading live rankings.
- [x] `NEXT_RUN`, `COMPLETED_TODAY`, `SUCCESS_RATE`, `FAILURE_LEADERBOARD`
      render with the "Preview" badge (`DashboardText.PREVIEW_BADGE`) and
      placeholder values from `DashboardText`, since no execution data exists.
- [x] Clicking `ACTIVE_TASKS` / `TOTAL_TASKS` navigates to `TaskView` (the
      former pre-filtered via `TaskRoutes.QUERY_STATUS`); clicking
      `ACTIVE_SCHEDULES` / `TOTAL_SCHEDULES` navigates to `ScheduleView` — all
      via `UI.getCurrent().navigate(ViewClass.class, ...)`, never a literal
      URL.
- [x] `DashboardPrefs.defaults()` returns every `CardId` in enum declaration
      order, all visible; `setVisible`/`isVisible` toggle a single card
      without affecting others; `reorder` moves a card to sit immediately
      before its drop target while preserving the full card set;
      `resetToDefaults` restores default order and clears all hidden cards
      (covered by `DashboardPrefsTest`).
- [x] `DashboardPrefs.save()` / `load()` round-trip the order and hidden set
      through `WebStorage` under `kairos.dashboard.order` /
      `kairos.dashboard.hidden`; unrecognized stored values are skipped with a
      `WARN` log rather than failing to load.
- [x] Dragging a card and dropping it onto another reorders the grid and
      persists the new order immediately (`DashboardView.onDrop` →
      `DashboardPrefs.reorder` + `save()` + `render()`).
- [x] `DashboardSettingsDialog` ("Customize dashboard") lists one checkbox per
      `CardId` reflecting current visibility; toggling a checkbox hides/shows
      that card on the dashboard immediately (no reload) and persists;
      "Reset to defaults" restores the full, unordered-by-user default layout
      and closes the dialog.
- [x] All card titles, captions, badge text, dialog copy and preview
      placeholder values come from `DashboardText`; the view and components
      contain no string/number literals.
- [x] Styling is done via `StyleConfig` + `Tokens` only (no inline CSS
      literals), matching the existing Apple-style look; the card grid is
      responsive.
- [x] No Lombok; methods ≤ 40 lines; changes confined to `kairos-admin`; no
      `domain`/`application`/`infrastructure`/API/schema changes.
- [x] `./gradlew :kairos-admin:build` passes, including
      `DashboardServiceTest`, `DashboardPrefsTest`, `StatCardTest`,
      `LeaderboardCardTest`.

## Notes

- The **setup wizard** remains a deliberately separate, later spec — the
  dashboard is its future launch surface but does not implement it.
- This spec was originally written and partly built as a static mockup (fixed
  four-card row, non-clickable "Platform Features" grid, explicitly no
  `DashboardService`). During implementation the design changed to a live,
  user-customizable dashboard instead; that static design was **never
  shipped** and has been removed from this document rather than described as
  planned/future work. See git history on `kairos-admin/src/main/java/dev/kairos/admin/feature/dashboard/`
  for the evolution.
- Execution-derived preview metrics (`NEXT_RUN`, `COMPLETED_TODAY`,
  `SUCCESS_RATE`, `FAILURE_LEADERBOARD`) stay isolated in `DashboardText` as
  placeholders; the intended follow-up, once executions are wired, is for
  `DashboardService` to compute them too and for `DashboardCardMeta` to drop
  them from the preview set — no structural change to `CardId`,
  `DashboardPrefs`, or the card components should be needed.
- Related: builds alongside the Schedule Admin UI (#20) and CRON builder
  (`doc/specs/cron-builder-ui.md`, #22).

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:admin
- **Priority:** priority:medium
- **Milestone:** Admin UI
