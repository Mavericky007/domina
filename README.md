# 🌙 Domina

> A warm, private menstrual-cycle & pregnancy companion — predictions, phases, moods, reminders, pregnancy week-by-week, and insights — that lives **entirely on your phone**.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20·%20Material%203-4285F4)
![Privacy](https://img.shields.io/badge/privacy-no%20INTERNET%20permission-success)
![Storage](https://img.shields.io/badge/storage-SQLCipher%20encrypted-blue)
![Price](https://img.shields.io/badge/price-free-brightgreen)

Built as a personal app — a gift for my wife — because the popular trackers (Flo, Lively, and friends)
send deeply personal health data to their clouds. Domina does the opposite: **your data never leaves
your device.** 💛

---

## 🔒 100% private & offline

Domina is designed so your data *can't* leak, by construction:

- 🚫 **No `INTERNET` permission at all.** The app is physically incapable of sending anything off the
  device — it's not "we promise not to," it's "it literally can't."
- 🔐 **Encrypted at rest** with SQLCipher; the database key is sealed in the **Android Keystore**.
- 👆 **Biometric + PIN lock** with auto-lock when you leave the app.
- 💾 **Encrypted backups you control** — export a passphrase-protected file anywhere *you* choose
  (Drive, email-to-self, SD); restore it on any phone. No cloud account, ever.

> Everything below works fully offline, on a plane, forever. There is no server, no sign-up, no telemetry.

---

## ✨ Features

- **🩸 Cycle & period tracking** — quick daily logging (mood, energy, flow, symptoms, BBT, cervical
  mucus, LH tests, libido, sleep, weight, notes) with a color-coded calendar.
- **🔮 Adaptive predictions** — next period, current phase (menstrual / follicular / ovulation / luteal),
  fertile window & ovulation, each with a confidence level that sharpens as you log. Learns from *your*
  history, not a fixed 28-day model.
- **⭕ Cycle-ring dashboard** — an at-a-glance circular day/phase dial.
- **🌱 Phase guidance** — gentle, encouraging "what to expect + Move / Eat / Plan" tips for each phase.
- **🔔 Reliable reminders** — period, fertility, daily-log nudge, plus medications & appointments;
  exact alarms that survive reboots and battery optimization.
- **🤰 Pregnancy mode** — switch modes any time: week-by-week size comparisons ("size of an avocado 🥑"),
  development blurbs, fun facts, and a due-date countdown.
- **👶 Pregnancy tools** — kick counter, contraction timer (with averages + the 5-1-1 tip), weight
  tracking, and hospital-bag / birth-plan checklists.
- **📊 Insights** — cycle-length history, a BBT chart with auto-detected coverline, weight trend, and
  on-device pattern insights ("cramps tend to show up in your menstrual phase").
- **🎨 Themes** — Soft & Sweet, Bright & Joyful, Warm & Cozy.
- **📄 Doctor report** — export a clean PDF summary for an OB/GYN visit.

---

## 🛠️ Build from source

Requirements: **JDK 17**, the **Android SDK** (platform 35, build-tools 35), and a device or emulator.

```bash
# build a debug APK
./gradlew :app:assembleDebug

# install on a connected device (USB debugging on)
./gradlew :app:installDebug

# run the tests
./gradlew :app:testDebugUnitTest          # fast JVM unit tests
./gradlew :app:connectedDebugAndroidTest  # on-device (encryption, DB) tests
```

The APK is a standard Android Studio project — open the folder in Android Studio and hit **Run** if you
prefer.

---

## 🧱 Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), single-activity, MVVM + clean layering.
- **Room** + **SQLCipher** for an encrypted database; **Android Keystore** for the key.
- **Hilt** for dependency injection.
- **AlarmManager** + **WorkManager** + a boot receiver for exact, reboot-surviving reminders.
- **AndroidX Biometric** for the app lock; **DataStore** for settings.
- Charts and the cycle ring are hand-drawn with **Compose Canvas** (no charting dependency).
- A pure-Kotlin **domain layer** (predictions, phase guidance, insights, backup codec/crypto) that is
  exhaustively unit-tested.

---

## 🗺️ Status

Built in phases, all on-device and test-covered: cycle MVP → adaptive predictions → reminders →
pregnancy mode → meds & appointments → pregnancy tools → insights → encrypted backup/restore →
doctor-report PDF. A home-screen widget and a custom/discreet launcher icon are the finishing touches.

Design specs and implementation plans live in [`docs/superpowers/`](docs/superpowers/).

---

## 💛 Notes

Domina offers gentle, encouraging guidance — it is **not medical advice**, and predictions are estimates.
For anything that matters, talk to a healthcare provider.

## License

A personal project, shared as-is. Use it, learn from it, build on it for yourself. 💛
