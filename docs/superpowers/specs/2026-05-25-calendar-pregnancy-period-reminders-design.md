# Design — Mode-aware predictions, calendar overlays & period-logging reminders

Date: 2026-05-25
Status: Approved (design)
Targets: next release (alongside the in-app updater robustness work)

## Problem

Reported on-device issues with the Calendar tab — several of which are really **core
prediction-logic** problems, so the fix must apply to Home (Today) and the reminder
scheduler too, not just the Calendar UI:

1. In **pregnancy mode**, the calendar still predicts the next menstrual period.
2. Logged **intimacy** shows in the activity list but has **no indicator on the grid dates**.
3. In **pregnancy mode**, the calendar still predicts **ovulation / fertile** days.
4. In **pregnancy mode**, the calendar should **gently highlight the pregnancy span** and
   **indicate trimesters** (on the grid and in the list).
5. In **cycle mode**, the user shouldn't have to open the app every day to log her period — a
   **twice-daily notification** should let her log the day's flow with one tap (no app open),
   running from the period start until ~7 days (eventually adaptive to her historic cycle).

## Root cause (items 1 & 3)

The cycle prediction is computed independently in **three** places, none of which know the
`AppMode`:
- `ui/calendar/CalendarViewModel.buildState`
- `ui/today/TodayViewModel.buildState`
- `reminders/ReminderManager.reschedule`

Each calls `CyclePredictor.predict` / `CycleProjection.marksFor` / `CycleRisk.analyze` directly,
so period/fertile/ovulation predictions and cycle alerts appear regardless of mode.

## Decisions (locked)

- **Period reminder anchor:** predicted start; re-anchor to the actual start once day 1 is logged.
- **Quick buttons:** Light / Medium / Heavy (write `FlowIntensity`).
- **Pregnancy calendar styling:** color-coded by trimester (3 soft tints + labels), grid **and** list.
- **Defaults:** period-logging toggle defaults **ON** for CYCLE/TTC; ❤️ shows for *any* intimacy.

## Design

### 1. Mode-aware predictions (core) — items 1 & 3

New pure helper `domain/prediction/CycleForecast.kt` wrapping the existing predictors and applying
one gating rule:

- `AppMode.PREGNANCY` → empty period / fertile / ovulation marks **and** a neutral
  `CycleRisk.Outlook` (no pregnancy-chance / EC / period-late signals).
- `AppMode.CYCLE` / `AppMode.TTC` → unchanged (TTC intentionally keeps fertile/ovulation).

Consumers:
- `CalendarViewModel` and `TodayViewModel` begin observing `settings.appMode` (+ `dueDate`) and
  route prediction through `CycleForecast`.
- `ReminderManager.reschedule` skips period & fertility alerts (and the new period-logging
  reminders) in pregnancy mode.

This makes the gating decision single-sourced, so Home and Calendar cannot drift apart again.

### 2. Intimacy indicator on the grid — item 2

- Add `intimacyDays: Set<Int>` to `CalendarUiState` (days where `intimacy != Intimacy.NONE`).
- In `DayCell`, overlay a tiny ❤️ (~9sp) bottom-center inside the existing circle so the cell
  size/layout is unchanged.
- Add a small ❤️ entry to the legend.

### 3. Pregnancy calendar overlay — item 4

- New pure `domain/pregnancy/PregnancyProjection.kt`: `marksFor(dueDate, month) -> Map<Int,Int>`
  (day → trimester 1/2/3) over the span `[due − 280, due]`, reusing the existing week-13 / week-27
  boundaries from `PregnancyCalculator`.
- In pregnancy mode the grid tints each in-span day by trimester (3 soft shades); out-of-span days
  unchanged.
- The activity list gains **trimester section headers** ("Trimester 2 · Weeks 14–27") and a
  **Due date** marker; the cycle legend is swapped for a trimester legend.

### 4. Period-logging reminders — item 5

Mirrors the existing check-in notification system.

- `domain/reminders/PeriodLogPrompt.kt` (pure, unit-tested): `shouldPromptOn(today, logs,
  prediction)` and an exposed window for testing.
  - **Window** = `anchor .. anchor + length`.
  - **anchor** = the actual logged period start if a recent one exists, else the predicted next
    start (predicted-then-re-anchor behaviour).
  - **length** = adaptive: `prediction.averagePeriodLength` when history allows, default 7,
    clamped to 3–10.
  - Prompts only when today is in-window and today's `flow` is not already logged.
- `reminders/PeriodReminderScheduler.kt`: two exact alarms/day (~10:00 & ~19:00), respecting the
  existing 9pm–9am quiet window.
- `reminders/PeriodReminderReceiver.kt`: on fire, checks mode ∈ {CYCLE, TTC}, toggle on, in-window,
  not-already-logged, not quiet → posts the notification, then re-arms.
- `reminders/PeriodLogActionReceiver.kt`: a Light/Medium/Heavy button does a read-modify-write
  (`getByDate(today) ?: DayLog(today)` → `copy(flow = …)` → `save`) with no app open, then a warm
  confirmation. Tapping the body opens the app.
- `domain/reminders/ReminderSettings.periodLogReminders` (new DataStore pref) + a Settings toggle.
- New `cycle_log` notification channel.
- Wired into `ReminderManager.reschedule` (schedule when enabled & non-pregnancy, else cancel) and
  registered in the manifest (`exported=false`).

### 5. Testing

Unit tests:
- `CycleForecast`: pregnancy → empty marks/outlook; cycle/TTC unchanged.
- `PregnancyProjection`: trimester boundaries across a month, span edges.
- `PeriodLogPrompt`: predicted-start window, re-anchor after logging day 1, skip-if-logged,
  adaptive length, out-of-window = no prompt.

## Out of scope / later

- Smarter period-end detection (stop nagging once the period clearly ended before the window end).
- Deep-linking the notification body straight to that day's log screen.
