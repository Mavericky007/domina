# 🌙 Domina

> A warm, private menstrual-cycle & pregnancy companion — predictions, phases, moods, reminders, pregnancy week-by-week, and insights — that lives **entirely on your phone**.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20·%20Material%203-4285F4)
![Privacy](https://img.shields.io/badge/privacy-data%20stays%20on%20device-success)
![Storage](https://img.shields.io/badge/storage-SQLCipher%20encrypted-blue)
![Price](https://img.shields.io/badge/price-free-brightgreen)
[![Latest release](https://img.shields.io/github/v/release/Mavericky007/domina?label=latest&color=3DDC84&logo=android&logoColor=white)](https://github.com/Mavericky007/domina/releases/latest)

Built as a personal app, because the popular trackers (Flo, Lively, and friends) send deeply personal
health data to their clouds. Domina does the opposite: **your data never leaves your device.** 💛

---

## ⬇️ Download

**[⬇️ Download the latest APK](https://github.com/Mavericky007/domina/releases/latest)** → on the release
page, tap the `.apk` asset under **Assets** to download it to your phone.

Then to install (sideload):

1. Open the downloaded `domina-*.apk`.
2. If prompted, allow installing from this source ("Install unknown apps").
3. On first launch, set a PIN — and you're in. 💛

Once installed, future updates are one tap from inside the app: **Settings → Check for updates**.

> Android only. Requires Android 8.0+. The APK is signed; updates install in place without losing data.

---

## 🔒 100% private & offline

Domina keeps your health data on your device, by design:

- 🚫 **Your data is never uploaded.** Cycle logs live only on the phone — there's no server, no account,
  and no telemetry. The app's *only* network use is an **optional, on-demand update check**
  (Settings → Check for updates) that contacts GitHub and sends **none** of your data.
- 🔐 **Encrypted at rest** with SQLCipher; the database key is sealed in the **Android Keystore**.
- 👆 **Biometric + PIN lock** with auto-lock when you leave the app.
- 💾 **Encrypted backups you control** — export a passphrase-protected file anywhere *you* choose
  (Drive, email-to-self, SD); restore it on any phone. No cloud account, ever.

> Everything works fully offline — no account, no telemetry, no server storing your data. The only time
> Domina touches the network is that optional update check, and only when you tap it.

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
- **🔄 In-app updates** — check for and install new versions from inside the app (Settings → Check for
  updates), the only feature that uses the network — on demand, no data sent.

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
