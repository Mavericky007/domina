# Design Inspiration & UI Principles

Inspiration only — **no copying**. We learn from what works in leading apps and design every
screen originally. Companion doc to the main spec.

## What leading apps do well (borrow the spirit)

- **Stardust** — a beautiful **circular cycle dial** mapped to phases with an interactive
  scrubber; immersive and narrative rather than clinical.
- **Clue** — clean, **data-driven**, gender-neutral, 30+ trackable data points.
- **Flo** — **education woven into** the experience; friendly, encouraging voice; huge reach.
- **"Empathetic period tracker" UX case study** (best source) — frame the app as a
  **companion, not a data collector** (mascot + warm tone); **colorful icon-first logging**;
  **cycle literacy up front**; **dashboard hierarchy** (current day + calendar on top,
  analytics below the fold); actionable visualizations (trends, fertility, symptoms).

## Pitfalls to avoid

- **Clue** can feel **cluttered**, features hard to find → use **progressive disclosure**.
- **Ovia** feels like a **medical portal** → stay warm and playful.
- **Flo** settled with the **FTC (2021)** and a **$56M class action (2025)** for sharing
  period/pregnancy data with Facebook/Google → this validates our entire premise. We ship
  with **no `INTERNET` permission**; **privacy is our headline feature.**
- Generic/AI-looking icons → ship a **consistent custom icon set**.

## Our 7 original UI principles

1. **Companion, not clinical** — warm voice, friendly illustrations/mascot, encouraging copy.
2. **Cycle-ring hero** — an original circular day/phase dial on the Today screen (Phase 2,
   once the prediction engine can color the phase arcs). Tap/drag to scrub days.
3. **Progressive disclosure** — simple daily log by default; expand for clinical fields
   (BBT, cervical mucus, LH) so depth never creates clutter.
4. **Icon-first logging** — quick tabs (Flow / Body / Mood / Notes) with colorful icons over
   text inputs.
5. **Education woven in** — phase guidance normalizes the cycle inline, not buried in help.
6. **Privacy as a visible feature** — a "stays on your phone" badge/affordance.
7. **Dashboard hierarchy** — today + next events on top; insights and analytics below the fold.

## Sources

- [Best Period Tracker App 2026: Clue vs Flo vs Ovia](https://www.go-go-gaia.com/blog/how-to-choose-period-tracker-app.html)
- [Stardust Period Tracker UI Breakdown](https://screensdesign.com/showcase/stardust-period-pregnancy)
- [Designing an Empathetic Period Tracker (UX case study)](https://medium.com/design-bootcamp/designing-an-empathetic-period-tracker-for-burpys-health-app-814501479a5f)
- [The 5 Best Period Tracker Apps in 2025](https://bearable.app/the-best-period-tracker-apps-of-2025/)
- [Flo Periods Tracking App — UI/UX Redesign (Behance)](https://www.behance.net/gallery/105602007/Flo-Periods-Tracking-App-UIUX-Redesign)
