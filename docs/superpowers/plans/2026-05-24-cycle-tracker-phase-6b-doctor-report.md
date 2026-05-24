# Phase 6b: Doctor-Report PDF Export — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user export a clean, shareable **PDF summary** of her cycle history, recent logs, and BBT for an OB/GYN visit — generated on-device and saved wherever she chooses (stays offline).

**Architecture:** A pure-Kotlin `ReportContent.build(...)` assembles the report into titled sections of text lines (JVM-tested). An Android `PdfReportRenderer` draws those sections onto pages with `android.graphics.pdf.PdfDocument`, writing to a plain `OutputStream` (so the SAF `Uri` plumbing lives in the UI and rendering is instrumented-testable with a byte stream). A Settings button creates an `application/pdf` document via SAF and renders into it.

**Tech Stack:** Kotlin, android.graphics.pdf.PdfDocument (built-in), Compose + Storage Access Framework, Hilt. No new dependencies. Still **no `INTERNET` permission**.

> Task 1 is pure-JVM unit-tested. Task 2 is Android; its renderer is verified via `am instrument` (renders to a byte stream, asserts a valid `%PDF` header — no device wipe). Avoid `connectedDebugAndroidTest`.

## Builds on (existing, on `main`)
- `domain/prediction/{PeriodDeriver,LoggedPeriod}.kt`, `domain/insights/CycleHistoryStats.kt`, `data/model/DayLog.kt`, `data/repository/DayLogRepository.kt`.
- `ui/settings/{SettingsScreen,SettingsViewModel}.kt`.

## New file structure
```
app/src/main/java/com/domina/cycle/
  report/ReportContent.kt        # pure: build titled sections of lines
  report/PdfReportRenderer.kt    # Android: render sections to a PDF OutputStream
  ui/settings/SettingsScreen.kt / SettingsViewModel.kt   # MODIFY: "Export PDF" button + SAF
app/src/test/java/com/domina/cycle/report/ReportContentTest.kt
app/src/androidTest/java/com/domina/cycle/report/PdfReportRendererTest.kt
```

---

## Task 1: ReportContent (pure)

**Files:**
- Create: `app/src/main/java/com/domina/cycle/report/ReportContent.kt`
- Test: `app/src/test/java/com/domina/cycle/report/ReportContentTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.domina.cycle.report

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class ReportContentTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun buildsSummaryWithCycleStatsAndRecentPeriods() {
        val logs = listOf(
            DayLog(date = d("2026-02-01"), flow = FlowIntensity.MEDIUM),
            DayLog(date = d("2026-03-01"), flow = FlowIntensity.MEDIUM),
            DayLog(date = d("2026-03-29"), flow = FlowIntensity.MEDIUM, symptoms = listOf("cramps")),
        )
        val report = ReportContent.build(logs, generatedOn = d("2026-04-01"))
        assertThat(report.title).contains("Cycle Report")
        val allText = report.sections.joinToString("\n") { it.title + "\n" + it.lines.joinToString("\n") }
        assertThat(allText).contains("Average cycle")   // cycle-stats section
        assertThat(allText).contains("2026-03-29")      // a recent period start appears
    }

    @Test fun emptyDataStillProducesAReport() {
        val report = ReportContent.build(emptyList(), generatedOn = d("2026-04-01"))
        assertThat(report.sections).isNotEmpty()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReportContentTest*"`
Expected: FAIL — unresolved `ReportContent`.

- [ ] **Step 3: Implement `ReportContent`**

```kotlin
package com.domina.cycle.report

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.domain.insights.CycleHistoryStats
import com.domina.cycle.domain.prediction.PeriodDeriver
import java.time.LocalDate

data class ReportSection(val title: String, val lines: List<String>)
data class Report(val title: String, val generatedOn: LocalDate, val sections: List<ReportSection>)

object ReportContent {
    fun build(logs: List<DayLog>, generatedOn: LocalDate = LocalDate.now()): Report {
        val periods = PeriodDeriver.derive(logs.filter { it.flow != FlowIntensity.NONE }.map { it.date })
        val history = CycleHistoryStats.compute(periods)
        val sections = mutableListOf<ReportSection>()

        sections += ReportSection(
            "Cycle summary",
            if (history.cycleLengths.isEmpty()) listOf("Not enough cycle data logged yet.")
            else listOf(
                "Cycles recorded: ${history.cycleLengths.size + 1}",
                "Average cycle length: ${history.average} days",
                "Range: ${history.shortest}–${history.longest} days",
            ),
        )

        val recentStarts = periods.takeLast(6).map { "Period started ${it.start} (lasted ${it.lengthDays} days)" }
        sections += ReportSection("Recent periods",
            recentStarts.ifEmpty { listOf("No periods logged yet.") })

        val recentSymptoms = logs.filter { it.symptoms.isNotEmpty() }.sortedByDescending { it.date }.take(15)
            .map { "${it.date}: ${it.symptoms.joinToString(", ")}" }
        sections += ReportSection("Recent symptoms",
            recentSymptoms.ifEmpty { listOf("No symptoms logged yet.") })

        val bbt = logs.filter { it.bbt != null }.sortedBy { it.date }
        sections += ReportSection("Basal body temperature",
            if (bbt.isEmpty()) listOf("No BBT readings logged yet.")
            else listOf("${bbt.size} readings, latest ${bbt.last().bbt} °C on ${bbt.last().date}"))

        return Report("Domina — Cycle Report", generatedOn, sections)
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ReportContentTest*"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/domina/cycle/report/ReportContent.kt \
        app/src/test/java/com/domina/cycle/report/ReportContentTest.kt
git commit -m "feat: doctor-report content builder (pure)"
```

---

## Task 2: PdfReportRenderer + Settings export

**Files:**
- Create: `app/src/main/java/com/domina/cycle/report/PdfReportRenderer.kt`
- Modify: `ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`
- Test: `app/src/androidTest/java/com/domina/cycle/report/PdfReportRendererTest.kt`

- [ ] **Step 1: Write the failing instrumented test (renders to bytes, checks %PDF)**

```kotlin
package com.domina.cycle.report

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PdfReportRendererTest {
    @Test fun rendersAValidPdf() {
        val report = Report(
            "Domina — Cycle Report", LocalDate.parse("2026-04-01"),
            listOf(ReportSection("Cycle summary", listOf("Average cycle length: 28 days", "Range: 27–30 days"))),
        )
        val out = ByteArrayOutputStream()
        PdfReportRenderer.render(report, out)
        val bytes = out.toByteArray()
        assertThat(bytes.size).isGreaterThan(100)
        assertThat(String(bytes.copyOfRange(0, 5), Charsets.US_ASCII)).isEqualTo("%PDF-")
    }
}
```

- [ ] **Step 2: Run to verify it fails (via am instrument — no wipe)**

Run: `./gradlew :app:installDebug :app:installDebugAndroidTest` then
`adb shell am instrument -w -e class com.domina.cycle.report.PdfReportRendererTest com.domina.cycle.test/androidx.test.runner.AndroidJUnitRunner`
Expected: FAIL — unresolved `PdfReportRenderer`.

- [ ] **Step 3: Implement `PdfReportRenderer`**

```kotlin
package com.domina.cycle.report

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object PdfReportRenderer {
    private const val PAGE_W = 595   // A4 @ 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 48f

    fun render(report: Report, out: OutputStream) {
        val doc = PdfDocument()
        val title = Paint().apply { textSize = 22f; isFakeBoldText = true }
        val heading = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 12f }
        val muted = Paint().apply { textSize = 10f; color = android.graphics.Color.GRAY }

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
        var c = page.canvas
        var y = MARGIN

        fun newPage() {
            doc.finishPage(page); pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas; y = MARGIN
        }
        fun line(text: String, paint: Paint, gap: Float) {
            if (y + gap > PAGE_H - MARGIN) newPage()
            c.drawText(text, MARGIN, y, paint); y += gap
        }

        line(report.title, title, 30f)
        line("Generated ${report.generatedOn} · stays on your device", muted, 24f)
        report.sections.forEach { section ->
            line(section.title, heading, 22f)
            section.lines.forEach { line("•  $it", body, 18f) }
            y += 8f
        }
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
```

- [ ] **Step 4: Run to verify it passes (am instrument)**

Run: `./gradlew :app:installDebugAndroidTest` then
`adb shell am instrument -w -e class com.domina.cycle.report.PdfReportRendererTest com.domina.cycle.test/androidx.test.runner.AndroidJUnitRunner`
Expected: `OK (1 test)`.

- [ ] **Step 5: Add the export action to Settings**

In `SettingsViewModel` add (it already has `@ApplicationContext context`, `message`, and a `DayLogRepository`? — if not, inject `DayLogRepository`):
```kotlin
    fun exportReport(uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching {
                val logs = dayLogRepository.observeRange(java.time.LocalDate.now().minusDays(400), java.time.LocalDate.now()).first()
                val report = com.domina.cycle.report.ReportContent.build(logs)
                context.contentResolver.openOutputStream(uri)!!.use { com.domina.cycle.report.PdfReportRenderer.render(report, it) }
            }.onSuccess { _message.value = "Report saved 💛" }.onFailure { _message.value = "Export failed: ${it.message}" }
        }
    }
```
(Inject `dayLogRepository: DayLogRepository` into `SettingsViewModel` if not already present; add `import kotlinx.coroutines.flow.first`.)
In `SettingsScreen`, add to the "Backup & restore" area (or a new "Reports" section) a `CreateDocument("application/pdf")` launcher and a button:
```kotlin
    val createPdf = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) vm.exportReport(uri) }
    // ...
    Text("Doctor report", style = MaterialTheme.typography.titleMedium)
    Button(onClick = { createPdf.launch("domina-cycle-report.pdf") }) { Text("Export PDF for my doctor") }
```

- [ ] **Step 6: Build, install, verify launch; commit**

Run: `./gradlew :app:installDebug` → BUILD SUCCESSFUL; launch; `adb logcat -d` shows no crash. (Real export tap is the user's to try: Settings → "Export PDF for my doctor" → choose location → a PDF is written.)
```bash
git add app/src/main/java/com/domina/cycle/report/PdfReportRenderer.kt \
        app/src/androidTest/java/com/domina/cycle/report/PdfReportRendererTest.kt \
        app/src/main/java/com/domina/cycle/ui/settings/
git commit -m "feat: doctor-report PDF renderer + Settings export"
```

---

## Phase 6b Definition of Done

- `ReportContent` is pure Kotlin with passing unit tests; `PdfReportRenderer` produces a valid `%PDF` document (instrumented-verified via am instrument).
- From Settings, the user can export a PDF cycle report to a location of her choice.
- Full unit suite green; app installed at the end. Still **no `INTERNET` permission**; no new dependencies.

## Deferred
- Embedding charts/images into the PDF; date-range selection for the report; richer formatting.
```
