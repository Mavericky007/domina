package com.domina.cycle.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.model.Mood
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.CycleForecast
import com.domina.cycle.domain.prediction.PeriodDeriver
import com.domina.cycle.domain.prediction.CycleRisk
import com.domina.cycle.domain.pregnancy.AppMode
import com.domina.cycle.domain.pregnancy.PregnancyProjection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class CalEventKind { PERIOD, PREDICTED_PERIOD, FERTILE, OVULATION, NOTE, TODAY, HEADER, ALERT }

data class CalendarEvent(
    val date: LocalDate?,   // tappable target; null for info rows / headers
    val icon: String,       // emoji shown in the leading badge
    val title: String,
    val detail: String,
    val kind: CalEventKind,
    val sort: LocalDate,    // ordering key (range start / event day)
)

/** Per-month derived view, computed purely from the observed logs. */
data class CalendarUiState(
    val month: YearMonth,
    val cells: List<Int?>,
    val periodDays: Set<Int>,
    val predictedPeriodDays: Set<Int>,
    val fertileDays: Set<Int>,
    val ovulationDays: Set<Int>,
    val intimacyDays: Set<Int> = emptySet(),
    val trimesterDays: Map<Int, Int> = emptyMap(),
    val isPregnancy: Boolean = false,
    val today: Int?,
    val events: List<CalendarEvent>,
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: DayLogRepository,
    settings: SettingsRepository,
) : ViewModel() {

    /** Raw history the UI pages over; one observation feeds every visible month. */
    data class CalendarData(
        val logs: List<DayLog>,
        val today: LocalDate,
        val mode: AppMode = AppMode.CYCLE,
        val dueDate: LocalDate? = null,
    )

    private val today = LocalDate.now()

    val data: StateFlow<CalendarData> =
        combine(
            repository.observeRange(today.minusDays(400), today.plusDays(400)),
            settings.appMode,
            settings.dueDate,
        ) { logs, mode, due -> CalendarData(logs, today, mode, due) }
            .stateIn(
                viewModelScope, SharingStarted.WhileSubscribed(5000),
                CalendarData(emptyList(), today),
            )

    companion object {
        private val dayFmt = DateTimeFormatter.ofPattern("MMM d")
        private val loggedFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
        private val moodEmoji = mapOf(
            Mood.HAPPY to "😊", Mood.CALM to "😌", Mood.SENSITIVE to "🥺",
            Mood.SAD to "😢", Mood.IRRITABLE to "😤", Mood.ANXIOUS to "😰",
            Mood.ENERGETIC to "⚡", Mood.TIRED to "😴",
        )

        fun buildState(m: YearMonth, logs: List<DayLog>, today: LocalDate, mode: AppMode, dueDate: LocalDate?): CalendarUiState {
            val flowDates = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date }
            val periods = PeriodDeriver.derive(flowDates)
            val pred = CyclePredictor.predict(periods, today)
            val outlook = CycleForecast.outlookFor(mode, pred, periods, logs, today)
            val marks = CycleForecast.marksFor(
                mode, periods, m, pred.averageCycleLength, pred.averagePeriodLength, today, outlook.delayDays,
            )
            val loggedDays = flowDates.filter { YearMonth.from(it) == m }.map { it.dayOfMonth }.toSet()
            val intimacyDays = logs
                .filter { YearMonth.from(it.date) == m && it.intimacy != com.domina.cycle.data.model.Intimacy.NONE }
                .map { it.date.dayOfMonth }.toSet()

            val isCurrentMonth = YearMonth.from(today) == m

            val isPregnancy = mode == AppMode.PREGNANCY && dueDate != null
            val trimesterDays = if (isPregnancy) PregnancyProjection.marksFor(dueDate!!, m) else emptyMap()

            // ── Prediction / window summaries (always future-leaning markers) ──
            val markerEvents = if (isPregnancy) buildList {
                if (java.time.YearMonth.from(dueDate) == m) {
                    add(CalendarEvent(dueDate, "👶", "Due date", dueDate!!.format(dayFmt), CalEventKind.NOTE, dueDate!!))
                }
                trimesterDays.entries.groupBy { it.value }.toSortedMap().forEach { (tri, days) ->
                    val firstDay = days.minOf { it.key }
                    val date = m.atDay(firstDay)
                    add(CalendarEvent(null, triEmoji(tri), "Trimester $tri", "Week ${weekOf(dueDate!!, date)}+", CalEventKind.NOTE, date))
                }
            } else buildList {
                if (marks.predictedPeriod.isNotEmpty()) {
                    add(CalendarEvent(null, "🩸", "Predicted period",
                        rangeLabel(m, marks.predictedPeriod), CalEventKind.PREDICTED_PERIOD,
                        m.atDay(marks.predictedPeriod.min())))
                }
                val fertileAll = marks.fertile + marks.ovulation
                if (fertileAll.isNotEmpty()) {
                    add(CalendarEvent(null, "🌱", "Fertile window",
                        rangeLabel(m, fertileAll), CalEventKind.FERTILE, m.atDay(fertileAll.min())))
                }
                marks.ovulation.minOrNull()?.let { d ->
                    val date = m.atDay(d)
                    add(CalendarEvent(date, "🥚", "Ovulation", date.format(dayFmt), CalEventKind.OVULATION, date))
                }
            }

            // ── Notable logged days only (period / notes / symptoms / mood) ──
            val loggedEvents = logs
                .filter { YearMonth.from(it.date) == m && it.notable() && !(isCurrentMonth && it.date == today) }
                .map { log ->
                    val period = log.flow != FlowIntensity.NONE
                    CalendarEvent(
                        date = log.date,
                        icon = if (period) "🩸" else moodEmoji[log.mood] ?: "📝",
                        title = log.date.format(loggedFmt),
                        detail = summarize(log),
                        kind = if (period) CalEventKind.PERIOD else CalEventKind.NOTE,
                        sort = log.date,
                    )
                }

            val all = markerEvents + loggedEvents
            val events = buildList {
                if (isCurrentMonth) {
                    when {
                        outlook.pregnancyChance == CycleRisk.PregnancyChance.LIKELY -> add(CalendarEvent(
                            null, "💗", "Pregnancy chance",
                            "Period ${outlook.daysLate}d late after unprotected intimacy — consider a test",
                            CalEventKind.ALERT, today))
                        outlook.pregnancyChance == CycleRisk.PregnancyChance.POSSIBLE -> add(CalendarEvent(
                            null, "💗", "Keep an eye out",
                            "Period ${outlook.daysLate}d late after unprotected intimacy this cycle",
                            CalEventKind.ALERT, today))
                        outlook.emergencyPillThisCycle -> add(CalendarEvent(
                            null, "💊", "Morning-after pill logged",
                            "Next period may arrive a few days later than usual",
                            CalEventKind.ALERT, today))
                    }
                    val todayLog = logs.firstOrNull { it.date == today }
                    add(CalendarEvent(today, "☀️", "Today · ${today.format(dayFmt)}",
                        todayLog?.takeIf { it.notable() }?.let { summarize(it) } ?: "No entry yet — tap to add",
                        CalEventKind.TODAY, today))
                    val upcoming = all.filter { it.sort.isAfter(today) }.sortedBy { it.sort }
                    val earlier = all.filter { !it.sort.isAfter(today) }.sortedByDescending { it.sort }
                    if (upcoming.isNotEmpty()) {
                        add(header("Coming up")); addAll(upcoming)
                    }
                    if (earlier.isNotEmpty()) {
                        add(header("Earlier")); addAll(earlier)
                    }
                } else if (m.isAfter(YearMonth.from(today))) {
                    addAll(all.sortedBy { it.sort })          // future month: soonest first
                } else {
                    addAll(all.sortedByDescending { it.sort }) // past month: most recent first
                }
            }

            return CalendarUiState(
                month = m,
                cells = CalendarMonth.cells(m),
                periodDays = loggedDays,
                predictedPeriodDays = marks.predictedPeriod,
                fertileDays = marks.fertile,
                ovulationDays = marks.ovulation,
                intimacyDays = intimacyDays,
                trimesterDays = trimesterDays,
                isPregnancy = isPregnancy,
                today = today.takeIf { YearMonth.from(it) == m }?.dayOfMonth,
                events = events,
            )
        }

        private fun header(text: String) =
            CalendarEvent(null, "", text, "", CalEventKind.HEADER, LocalDate.MIN)

        private fun DayLog.notable() =
            flow != FlowIntensity.NONE || note.isNotBlank() || symptoms.isNotEmpty() || mood != null ||
                intimacy != com.domina.cycle.data.model.Intimacy.NONE || emergencyContraception ||
                pregnancyTest != com.domina.cycle.data.model.PregnancyTest.NOT_TESTED

        private fun triEmoji(t: Int) = when (t) { 1 -> "🌱"; 2 -> "🌷"; else -> "🌳" }
        private fun weekOf(due: LocalDate, date: LocalDate): Int =
            (java.time.temporal.ChronoUnit.DAYS.between(due.minusDays(280), date) / 7).toInt()

        private fun rangeLabel(m: YearMonth, days: Set<Int>): String {
            val lo = days.min(); val hi = days.max()
            return if (lo == hi) m.atDay(lo).format(dayFmt) else "${m.atDay(lo).format(dayFmt)} – ${hi}"
        }

        private fun summarize(log: DayLog): String {
            val parts = buildList {
                if (log.flow != FlowIntensity.NONE) add("${log.flow.name.lowercase()} flow")
                log.mood?.let { add(it.name.lowercase()) }
                if (log.symptoms.isNotEmpty()) add(log.symptoms.joinToString(", "))
                if (log.intimacy != com.domina.cycle.data.model.Intimacy.NONE) add("${log.intimacy.name.lowercase()} sex")
                if (log.emergencyContraception) add("morning-after pill")
                if (log.pregnancyTest != com.domina.cycle.data.model.PregnancyTest.NOT_TESTED)
                    add("${log.pregnancyTest.name.lowercase()} pregnancy test")
                if (log.note.isNotBlank()) add("note")
                log.bbt?.let { add("BBT $it°") }
            }
            return parts.joinToString(" · ").ifEmpty { "logged" }
        }
    }
}
