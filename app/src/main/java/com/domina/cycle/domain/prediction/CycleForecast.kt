package com.domina.cycle.domain.prediction

import com.domina.cycle.data.model.DayLog
import com.domina.cycle.domain.pregnancy.AppMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * Mode-aware gate over the cycle predictors. In pregnancy mode there is no menstrual cycle to
 * forecast, so period/fertile/ovulation marks and the cycle-risk outlook are suppressed. Cycle and
 * TTC modes pass straight through (TTC intentionally keeps fertile/ovulation).
 */
object CycleForecast {
    fun marksFor(
        mode: AppMode,
        periods: List<LoggedPeriod>,
        month: YearMonth,
        avgCycle: Int,
        avgPeriod: Int,
        today: LocalDate,
        delayDays: Int = 0,
    ): CycleProjection.MonthMarks =
        if (mode == AppMode.PREGNANCY) CycleProjection.MonthMarks()
        else CycleProjection.marksFor(periods, month, avgCycle, avgPeriod, today, delayDays)

    fun outlookFor(
        mode: AppMode,
        prediction: CyclePrediction,
        periods: List<LoggedPeriod>,
        logs: List<DayLog>,
        today: LocalDate,
    ): CycleRisk.Outlook =
        if (mode == AppMode.PREGNANCY) CycleRisk.Outlook.EMPTY
        else CycleRisk.analyze(prediction, periods, logs, today)
}
