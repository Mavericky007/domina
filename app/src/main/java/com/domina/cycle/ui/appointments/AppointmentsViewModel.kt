package com.domina.cycle.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.AppointmentEntity
import com.domina.cycle.data.repository.AppointmentRepository
import com.domina.cycle.reminders.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    private val reminderManager: ReminderManager,
) : ViewModel() {
    val appointments: StateFlow<List<AppointmentEntity>> =
        repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(title: String, atEpochMillis: Long, leadHours: Int) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.add(title.trim(), atEpochMillis, leadHours); reminderManager.reschedule() }
    }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id); reminderManager.reschedule() } }
}
