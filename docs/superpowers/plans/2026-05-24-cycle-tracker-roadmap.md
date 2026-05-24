# Cycle & Pregnancy Tracker — Implementation Roadmap

Spec: `docs/superpowers/specs/2026-05-24-private-cycle-pregnancy-tracker-design.md`

The app is built in **6 phases**, each shippable and testable on its own. Each phase gets its
own detailed plan file, written just-in-time before execution so it stays accurate to the code
that already exists.

| Phase | Plan file | Delivers (working app at end of phase) |
|---|---|---|
| **1. Foundation & Core MVP** | `2026-05-24-cycle-tracker-phase-1-foundation.md` | Buildable Compose app, encrypted DB (SQLCipher + Keystore), data model, repositories, 3 switchable themes, biometric+PIN lock, daily logging, calendar, Today dashboard. **She can log days and browse a calendar, fully encrypted & locked.** |
| **2. Prediction engine & phases** | _(written before Phase 2)_ | Pure-Kotlin adaptive prediction engine (next period, phase, fertile window, ovulation, confidence) + bundled phase-guidance content + the original **cycle-ring hero** dashboard (see `docs/superpowers/specs/2026-05-24-design-inspiration.md`). |
| **3. Notifications & background** | _(written before Phase 3)_ | AlarmManager exact alarms, WorkManager nightly recompute, BootReceiver, the 4 reminder types, runtime permissions. |
| **4. Pregnancy mode** | _(written before Phase 4)_ | Mode switch, pregnancy profile, bundled 40-week size-comparison dataset, kick counter, contraction timer, weight, checklists, milestone notifications. |
| **5. Insights & analytics** | _(written before Phase 5)_ | Vico charts, BBT chart with coverline, on-device pattern insights. |
| **6. Convenience, report, backup polish** | _(written before Phase 6)_ | Glance home-screen widget, log-from-notification, PDF/CSV doctor report, manual encrypted export/import, discreet-icon option. |

**Cross-cutting principles (every phase):** no `INTERNET` permission; TDD on all domain/logic
code; frequent commits; DRY/YAGNI; warm, playful tone.
