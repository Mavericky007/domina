package com.domina.cycle.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.MedicationEntity
import com.domina.cycle.data.repository.MedicationRepository
import com.domina.cycle.reminders.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedsViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    val meds: StateFlow<List<MedicationEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, timeMinutes: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.add(name.trim(), timeMinutes); reminderManager.reschedule() }
    }
    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id); reminderManager.reschedule() }
    }
}
