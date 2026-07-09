# Dashboard / Home Landing Page (Admin UI)

## Problem

The Kairos admin console currently drops the operator straight onto the Tasks
grid — there is no overview screen, no sense of "what's going on" at a glance,
and no guided entry point into the console's main actions. Operators want a
**home dashboard**: a summary of the system's health (active/total tasks,
throughput, success rate) plus fast paths into the main workflows.

Because the engine / execution side isn't wired up yet, the live metrics don't
exist. So this slice ships the dashboard **structure and look** with hardcoded
placeholder numbers, arranged so real data can drop in later without reworking
the layout.

**For whom:** operators / admins landing in the Kairos admin console.

## Scope

**In:**
- A new **Dashboard** screen (`DashboardView`) registered under `MainLayout`,
  mapped to the **empty route** `""` so it is the console's **landing page**,
  and added as the **first** `SideNavItem` in `MainLayout` (above Tasks).
- **Summary stat cards** — a top row of four cards, each with an icon, a large
  number, and a caption:
  - **Active Tasks**, **Completed Today**, **Success Rate**, **Total Tasks**.
  - Each card has a **CTA button** beneath it linking to an existing route:
    Create Task → Tasks, View Schedule → Schedules, Monitor Tasks → Tasks,
    View Reports → (no destination yet → Tasks, or disabled).
  - Values are **hardcoded placeholders** (via `DashboardText`) with a clear
    `// TODO` marking where a real stats source plugs in. No API calls for
    metrics in this slice.
- **Platform Features** section — a titled grid ("Everything you need for task
  management") of **six descriptive feature cards** (icon + title + blurb),
  **mixed**: some reflect real Kairos capabilities, some are general:
  - **Multiple Delivery Adapters** — highlights that Kairos delivers to many
    destinations (SQS, Kafka, Webhook, RabbitMQ — and more coming), where it
    previously supported only SQS. This is the headline capability card.
  - **Flexible Scheduling** — ONCE / CRON / FIXED schedules.
  - **Retry & Reliability** — retry policies, at-least-once delivery, safe
    horizontal scaling.
  - **Live Monitoring** — track execution in real time (general/aspirational).
  - **Advanced Analytics** — insights & reports (general/aspirational).
  - **Secure & Reliable** — enterprise-grade posture (general/aspirational).
  - Feature cards are **not clickable** — purely descriptive.
- Kairos "Apple-style" look: cards built from `StyleConfig` + `Tokens`
  (glass/translucent surface, radius, shadow, gradient accents), matching the
  existing views. Responsive card grid.
- All copy (numbers, captions, titles, blurbs, CTA labels) in a new
  `DashboardText` constants class — **no literals in the component**.

**Out:**
- **The setup wizard.** A guided "connect destination → create task → configure
  schedule" onboarding wizard is a **separate future spec** (`/specification`
  again). This slice only builds the dashboard; it does not build the wizard,
  though the dashboard is the natural launch point for it later.
- **Live metrics / a stats API.** No execution/throughput data exists yet
  (engine not wired). Numbers are placeholders. No new endpoint, no
  `DashboardService`, no `common` DTO — deferred until executions land.
- Any `domain` / `application` / `infrastructure` / API / schema change. This is
  `kairos-admin` UI only.
- Clickable feature cards, per-card deep links beyond the stat CTAs, charts,
  time-range pickers, auto-refresh.

## Design

**Module / layer:** `kairos-admin` only. New code lives in a new feature package
`dev.kairos.admin.feature.dashboard`, mirroring the `task` / `schedule` /
`destination` feature layout.

**New files:**

```
feature/dashboard/
  DashboardRoutes.java     # route + page-title constants (route = "", title = "Dashboard · Kairos")
  DashboardText.java       # every label, number, caption, feature title/blurb, CTA label — no literals in the view
  DashboardView.java       # @Route("") @PageTitle — assembles stat cards + features grid from small build* helpers
  component/
    StatCard.java          # one summary card: icon + big number + caption + CTA button
    FeatureCard.java       # one feature card: icon + title + blurb (non-clickable)
```

- **`DashboardView`** — `@Route(value = DashboardRoutes.HOME /* "" */, layout =
  MainLayout.class)`, `@PageTitle(DashboardRoutes.PAGE_TITLE)`. Extends
  `VerticalLayout` like the other views; padding/gap via `StyleConfig` +
  `Tokens` (same pattern as `ScheduleView`). Assembled from small helpers
  (`buildStatsRow`, `buildFeaturesSection`, each ≤ 40 lines) that instantiate
  `StatCard` / `FeatureCard` from `DashboardText` constants. Stat CTA buttons
  use `Buttons` and navigate via `UI.getCurrent().navigate(...)` to the existing
  view classes (`TaskView`, `ScheduleView`) — no hardcoded URL strings.
- **`StatCard`** — a reusable card component (icon, value, caption, CTA). Kept in
  the dashboard feature package (its only consumer is the dashboard). Styled
  entirely through `StyleConfig` + `Tokens`; uses the glass/translucent surface,
  radius, shadow tokens already present. Accent colour per card via existing
  colour tokens (primary / success / warning) — add a token only if a genuinely
  new colour is needed.
- **`FeatureCard`** — icon + title + blurb, non-interactive, same styling
  vocabulary. Icons via `VaadinIcon` (as `MainLayout` already does).
- **`DashboardText`** — all strings and placeholder numbers, in the style of
  `ScheduleText` / `LayoutText`. Placeholder metric values are constants here
  with a `// TODO: replace with real stats source once executions are wired`
  comment at the single point they're defined.

**Navigation & landing page:**
- Add `NAV_DASHBOARD` to `LayoutText` and a `SideNavItem` **first** in
  `MainLayout.createSideNav()` (icon e.g. `VaadinIcon.DASHBOARD`).
- `DashboardView` takes route `""`. **Check:** confirm no other view currently
  owns `""` (today Tasks is reachable at its own route via `TaskRoutes`); if
  Tasks currently holds the empty route it must move to an explicit route (e.g.
  `tasks`) so the dashboard becomes the landing page without a route clash.

**Styling / conventions:**
- All CSS via `StyleConfig` + `Tokens` — **no inline CSS string literals** in
  components. Any genuinely new token (e.g. a card min-width, a stats-grid gap,
  a card accent bar) is added to `Tokens`, not hand-written in the component.
- The responsive multi-column card layout uses CSS grid via `StyleConfig`
  (`display: grid`, `grid-template-columns`), with the column template as a
  `Tokens` constant — no literal grid strings in the view.
- No Lombok. SLF4J only if a real log statement is warranted (this is static UI;
  likely none needed).
- Methods ≤ 40 lines; SRP — `StatCard` and `FeatureCard` each do one thing.

**Data placeholder shape:** metrics are plain constants for now. When executions
land, the intended follow-up is a `DashboardService` (admin-side) or a
`common` stats DTO fetched via `KairosApiClient`; `DashboardView` would swap the
constant read for a service call with the layout unchanged. Called out here so
the placeholder is deliberately isolated to `DashboardText`.

## Acceptance criteria

- [ ] Visiting the console root (`""`) shows the **Dashboard** as the landing
      page; a **Dashboard** nav item appears **first** in the side nav and is
      selected there.
- [ ] The top row shows four stat cards — **Active Tasks**, **Completed Today**,
      **Success Rate**, **Total Tasks** — each with an icon, a large placeholder
      number, a caption, and a CTA button.
- [ ] Each stat CTA navigates to its existing target view (Create Task / Monitor
      Tasks → Tasks; View Schedule → Schedules) via the view class, not a
      literal URL.
- [ ] A **Platform Features** section shows six descriptive, non-clickable cards,
      including a **Multiple Delivery Adapters** card that names SQS, Kafka,
      Webhook, RabbitMQ (and "more coming").
- [ ] All placeholder numbers, captions, titles, blurbs and CTA labels come from
      `DashboardText`; the view contains **no string/number literals**.
- [ ] Styling is done via `StyleConfig` + `Tokens` only (no inline CSS literals),
      matching the existing Apple-style look; the card grid is responsive.
- [ ] No Lombok; methods ≤ 40 lines; changes confined to `kairos-admin`; no
      `domain`/`application`/`infrastructure`/API/schema changes.
- [ ] `./gradlew :kairos-admin:build` passes.

## Notes

- The **setup wizard** is a deliberately separate, later spec — this slice is
  its future launch surface but does not implement it.
- Placeholder metrics are isolated in `DashboardText` so the eventual real
  stats source (a `DashboardService` / stats DTO once executions are wired) is a
  contained change.
- Adapter story context: Kairos previously delivered only to SQS; it now targets
  multiple delivery adapters (SQS, Kafka, Webhook, RabbitMQ, more planned) —
  see `kairos-adapters/` and the M7 hexagonal-payoff checkpoint in
  `.claude/CLAUDE.md`. The **Multiple Delivery Adapters** feature card surfaces
  this.
- Reference screenshot provided by the user (a similar dashboard in another
  product) informs layout only; visuals must be re-styled to Kairos conventions.
- Related: builds alongside the Schedule Admin UI (#20) and CRON builder
  (`doc/specs/cron-builder-ui.md`, #22).

## Issue metadata (suggested)

- **Type:** feature
- **Module(s):** module:admin
- **Priority:** priority:medium
- **Milestone:** Admin UI
