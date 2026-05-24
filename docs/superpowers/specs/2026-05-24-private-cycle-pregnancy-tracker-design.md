# Private Cycle & Pregnancy Tracker — Design Spec

- **Date:** 2026-05-24
- **Status:** Approved (pending spec review)
- **Platform:** Native Android (Kotlin + Jetpack Compose)
- **Audience:** A personal app, primarily for one user (the author's wife), sideloaded.

---

## 1. Overview

A warm, playful, privacy-first Android app for tracking the menstrual cycle, cycle
phases, mood, fertility, and pregnancy. Unlike Flo / Lively and similar apps, this app
stores **all data exclusively on the device** and performs **zero network communication**.
It is encrypted, locked behind biometrics, accurate via an adaptive prediction engine, and
delivers reliable reminders.

### Goals
- Track cycle, phases, mood, and full clinical fertility data — all on-device.
- Accurate, *adaptive* predictions (learns from her real history, not a fixed 28-day model).
- Reliable, exact notifications that survive reboots and battery optimization.
- Strong privacy: encrypted at rest, biometric lock, and **no internet capability at all**.
- A happy, encouraging, delightful experience — not a clinical tool.

### Non-Goals
- No cloud, no account, no sync, no analytics, no ads, no telemetry.
- No multi-user / profiles (single user).
- No iOS (Android only).
- Not a medical device; guidance is framed as gentle, non-medical encouragement.

---

## 2. Core Privacy Principle

**The app declares no `INTERNET` permission in its manifest.** It is therefore *physically
incapable* of transmitting data off the device. This is the strongest possible guarantee of
the "phone storage only" requirement — not a promise, an architectural fact.

---

## 3. Modes (switchable anytime)

A top-level **Mode** the user can flip in Settings:

1. **Cycle** — everyday period & phase tracking (default).
2. **Trying to Conceive (TTC)** — same data, surfaced around the fertile window/ovulation.
3. **Pregnancy** — pregnancy tracker becomes the centerpiece; cycle prediction pauses.

Switching modes changes the dashboard and which insights/notifications are emphasized. No
data is lost when switching; history is preserved.

---

## 4. Feature Set

### 4.1 Core (always included)
- **Clinical daily logging:** mood, energy, flow intensity, symptoms (multi-select), BBT,
  cervical mucus, LH/ovulation test result, libido, sleep, weight, free-text note. Log only
  what she wants each day.
- **Adaptive predictions:** next period, current phase (menstrual / follicular / ovulation /
  luteal), fertile window, ovulation day — each with a confidence level.
- **Today dashboard + color-coded calendar.**
- **Phase guidance:** per-phase mood forecast + Move / Eat / Plan tips (see §8).
- **Notifications** (all toggleable individually): period & cycle alerts, fertility alerts,
  daily logging nudge, meds & appointment reminders. Pregnancy weekly milestones auto-on in
  Pregnancy mode.
- **Biometric + PIN lock**, auto-lock on background.
- **Encrypted database**, auto local backups + manual encrypted export/import.
- **Discreet icon** option (generic name/icon on the home screen).
- **Themes:** three switchable in-app themes (see §9).

### 4.2 Insights & analytics
- Cycle-history charts; average cycle & period length over time.
- BBT chart with coverline (ovulation confirmation).
- Pattern insights, e.g. "your mood tends to dip ~2 days before your period",
  "headaches cluster in your luteal phase". Derived purely on-device from logged data.

### 4.3 Full pregnancy mode
- Week-by-week baby development + **size comparison to a fruit/veg/nut** with a fun fact
  (see §7). Bundled offline dataset (~40 weeks).
- Due-date countdown, kick counter, contraction timer, pregnancy weight tracking,
  hospital-bag & birth-plan checklists.

### 4.4 Convenience extras
- Home-screen **widget** (cycle day / days until period / current phase).
- **Log from notification** + quick-add shortcuts.

### 4.5 Doctor report export
- Generate a clean **PDF/CSV** summary (cycles, symptoms, BBT) to share at appointments.
  Fully offline; the user chooses if/when to share the generated file.

---

## 5. Architecture

Single-activity Kotlin app, Jetpack Compose, MVVM + clean layering. Each unit has one clear
purpose, communicates through well-defined interfaces, and is independently testable.

- **UI layer** — Compose screens + ViewModels, Navigation Compose. Renders state, emits events.
- **Domain layer** — *pure Kotlin, no Android deps* (exhaustively unit-testable): prediction
  engine, mode manager, insights/stats, reminder scheduler.
- **Data layer** — repositories over Room (SQLite) + **SQLCipher**; encrypted DataStore for
  prefs; backup module; bundled read-only asset datasets.
- **Platform/background** — AlarmManager (exact reminders), WorkManager (nightly recompute +
  backups), BootReceiver (re-arm after reboot), AndroidX Biometric (lock).

### Libraries / config
- Hilt (DI), Coroutines/Flow (async), Room + SQLCipher (storage), AndroidX Security/Keystore,
  AndroidX Biometric, WorkManager, Vico (Compose charts), Android `PdfDocument` (reports),
  Glance (home-screen widget).
- **minSdk 26 (Android 8.0)**, **targetSdk 35 (Android 15)**.
- **No `INTERNET` permission.**

---

## 6. Data Model

Encrypted Room entities:
- `DayLog` — date (PK), mood, energy, flow, symptoms[], bbt, cervicalMucus, lhTest, libido,
  sleepHours, weight, note.
- `CycleEvent` — period start/end markers used to anchor predictions.
- `Prediction` — cached per-day phase + fertility + confidence (recomputed nightly).
- `Reminder` — type, time, recurrence, enabled.
- `Appointment` — datetime, title, note, reminder offset.
- `Medication` — name, schedule, reminder config (e.g. prenatal vitamins, pill).
- `PregnancyProfile` — mode, LMP / due date, current week, kick/contraction logs, checklists.

Bundled read-only assets (shipped in the APK, no network):
- `pregnancy_weeks` — 40-week dataset: size comparison + emoji + measurements + development
  blurb + fun fact.
- `phase_guidance` — the four-phase content table (§8).

---

## 7. Pregnancy Size-Comparison Dataset

For each pregnancy week, a playful card: "your baby is the size of a **<fruit/veg/nut>** <emoji>",
with approximate length/weight, a short development blurb, and a fun fact. Bundled as an app
asset so it works fully offline (e.g. on a plane). ~40 weeks of entries.

---

## 8. Phase Guidance Content

Default day ranges below; the engine shifts them to her **actual** cycle length. Tone is warm
and gentle ("you might feel…", "many women find…"), with a quiet "guidance, not medical advice"
footer. Sourced from Cleveland Clinic and Healthline (cycle-syncing); nutrition framing kept
gentle since some cycle-syncing diet advice is popular wisdom more than hard science.

| Phase | Mood/energy forecast | Move | Eat | Plan |
|---|---|---|---|---|
| **Menstrual (1–5)** | Lowest energy; serotonin/dopamine dip → tired, foggy, tender | Rest & restore: yoga, stretching, gentle walks | Iron (spinach, lentils, red meat) + vitamin C to absorb it; omega-3s; warm food | Be kind to yourself; light load, journaling, early nights |
| **Follicular (6–14)** | Estrogen rising → upbeat, motivated, sharp | Ramp up: cardio, hikes, new classes | Lean protein, complex carbs, avocado, seeds, fermented foods | Start new things; big projects, tough talks, creativity |
| **Ovulation (14–17)** | Peak energy, mood & libido; most social | Go hard: HIIT, spin, kickboxing | Berries, cruciferous veg (broccoli, brussels), light fresh food | Schedule presentations, dates, social events, negotiations |
| **Luteal (18–28)** | Winding down; PMS, cravings, irritability, bloating later | Taper: strength early, then walks, pilates, tai chi | Fiber + magnesium (pumpkin seeds, dark chocolate), hydrate; ease sugar/salt/caffeine | Wrap up & tidy; self-care, cozy nights, smaller gatherings |

---

## 9. Themes

Three switchable themes (live preview in Settings), all "happy" but distinct:
1. **Soft & Sweet** — pastel peach & lavender, rounded, tender.
2. **Bright & Joyful** — vibrant coral / teal / sunshine, celebratory (confetti moments).
3. **Warm & Cozy** — earthy terracotta / sage / cream, calm and grounded.

Implemented as Compose theme definitions (color schemes + accents) selectable at runtime;
the choice persists in encrypted DataStore.

---

## 10. Prediction Engine (accuracy core)

Pure Kotlin, the heart of the "accurate" requirement.
- Cold start: sensible defaults (configurable average cycle/period length).
- Adaptive: rolling average of recent cycle lengths + variability → predicts next period,
  current phase, fertile window, ovulation day.
- **Confidence level** that rises as more cycles are logged.
- In TTC/clinical mode, BBT + LH + cervical mucus refine ovulation detection.
- Recomputed nightly via WorkManager and on relevant new logs.
- Fully unit-tested against known cycle patterns (regular, irregular, sparse data).

---

## 11. Notifications & Background

- `AlarmManager` exact alarms per reminder (period, fertility, daily nudge, meds, appointments,
  pregnancy milestones).
- Nightly `WorkManager` job: recompute predictions, refresh widget, re-arm alarms.
- `BootReceiver`: re-schedule all alarms after device reboot.
- Runtime permission handling for `POST_NOTIFICATIONS` (Android 13+) and exact-alarm permission
  (Android 12+), with friendly in-app prompts.
- Optional "log from notification" actions.

---

## 12. Security & Backup

- SQLCipher database; key stored in the Android Keystore.
- Biometric (fingerprint/face) unlock with PIN fallback; auto-lock when app backgrounds.
- **Auto local backups:** periodic encrypted backups kept on-device.
- **Manual export/import:** passphrase-protected encrypted backup file the user saves wherever
  she chooses (Drive, email-to-self, SD, USB) via the Storage Access Framework. Import validates
  and restores. The app itself never uploads anything.

---

## 13. Distribution

Sideloaded, signed APK installed directly on her phone (and the author's). No Play Store, no
review, manual updates by the author. This enables the discreet-icon feature and full freedom.

---

## 14. Testing & Error Handling

- **TDD on the domain layer:** prediction, phase math, reminder scheduling, stats/insights.
- Graceful empty states ("log a few days to unlock predictions").
- Safe, validated backup import (reject corrupt/incompatible files without crashing).
- No crashes on sparse or irregular data; predictions degrade to lower confidence, never errors.
- Instrumented tests for DB encryption round-trip, alarm scheduling, and backup export/import.

---

## 15. Tone & Content Guidelines

- Warm, playful, encouraging copy throughout ("You're doing amazing 💛").
- Gentle, non-prescriptive framing for guidance; visible "guidance, not medical advice" note.
- Delightful but tasteful: friendly emoji/illustrations, gentle milestone celebrations.
- Accessible: readable contrast in every theme, large tap targets, optional larger text.

**UI design principles** (full detail + competitor analysis in
`2026-05-24-design-inspiration.md`): companion-not-clinical voice; an original **cycle-ring
hero** (circular day/phase dial) on Today (Phase 2); **progressive disclosure** (simple log by
default, expand for clinical fields); **icon-first logging** in quick tabs; education woven in;
**privacy shown as a visible feature**; dashboard hierarchy (today + next events on top,
insights below). Inspiration only — every screen is original work, no copying.

---

## 16. Future / Out of Scope (for now)
- iOS version (would justify revisiting the cross-platform decision).
- Postpartum / breastfeeding mode.
- Optional partner read-only sharing (would require careful privacy design).
