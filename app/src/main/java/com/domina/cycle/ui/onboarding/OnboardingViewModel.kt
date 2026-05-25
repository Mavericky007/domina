package com.domina.cycle.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.repository.DayLogRepository
import com.domina.cycle.data.repository.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Existing profile values, used to pre-fill the form when editing. */
data class OnboardingPrefill(
    val name: String = "",
    val birthDate: LocalDate? = null,
    val heightCm: Int? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val weightRepository: WeightRepository,
    private val dayLogRepository: DayLogRepository,
) : ViewModel() {

    val prefill: StateFlow<OnboardingPrefill> =
        combine(settings.userName, settings.birthDate, settings.heightCm) { name, dob, h ->
            OnboardingPrefill(name ?: "", dob, h)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OnboardingPrefill())

    /**
     * Persists the profile. On first-run ([isEdit] == false) it also seeds real starting data —
     * a current-weight entry and the last-period day — and marks onboarding complete.
     */
    fun save(
        name: String,
        birthDate: LocalDate?,
        heightCm: Int?,
        weightKg: Double?,
        lastPeriodStart: LocalDate?,
        isEdit: Boolean,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            settings.saveProfile(name, birthDate, heightCm)
            if (!isEdit) {
                weightKg?.let { weightRepository.add(LocalDate.now().toEpochDay(), it) }
                lastPeriodStart?.let { d ->
                    val existing = dayLogRepository.getByDate(d)
                    dayLogRepository.save((existing ?: DayLog(d)).copy(flow = FlowIntensity.MEDIUM))
                }
                settings.setOnboardingComplete(true)
            }
            onDone()
        }
    }
}
