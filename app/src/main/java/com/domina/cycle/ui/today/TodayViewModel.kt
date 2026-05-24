package com.domina.cycle.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.domain.guidance.PhaseGuidance
import com.domina.cycle.domain.guidance.PhaseGuide
import com.domina.cycle.domain.prediction.CyclePrediction
import com.domina.cycle.domain.prediction.CyclePredictor
import com.domina.cycle.domain.prediction.PeriodDeriver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class TodayUiState(
    val today: LocalDate,
    val prediction: CyclePrediction,
    val guidance: PhaseGuidance?,
    val todayLog: DayLog?,
) {
    companion object {
        fun empty(today: LocalDate) = TodayUiState(
            today = today,
            prediction = CyclePredictor.predict(emptyList(), today),
            guidance = null,
            todayLog = null,
        )
    }
}

@HiltViewModel
class TodayViewModel @Inject constructor(
    repository: DayLogRepository,
) : ViewModel() {
    val today: LocalDate = LocalDate.now()

    val state: StateFlow<TodayUiState> =
        repository.observeRange(today.minusDays(LOOKBACK_DAYS), today)
            .map { logs -> buildState(logs, today) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.empty(today))

    companion object {
        private const val LOOKBACK_DAYS = 400L

        /** Pure transform from logs to dashboard state — unit-tested directly. */
        fun buildState(logs: List<DayLog>, today: LocalDate): TodayUiState {
            val flowDates = logs.filter { it.flow != FlowIntensity.NONE }.map { it.date }
            val periods = PeriodDeriver.derive(flowDates)
            val prediction = CyclePredictor.predict(periods, today)
            val guidance = prediction.phase?.let { PhaseGuide.forPhase(it) }
            val todayLog = logs.firstOrNull { it.date == today }
            return TodayUiState(today, prediction, guidance, todayLog)
        }
    }
}
