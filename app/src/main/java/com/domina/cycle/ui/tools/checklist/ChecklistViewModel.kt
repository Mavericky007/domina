package com.domina.cycle.ui.tools.checklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.db.entity.ChecklistItemEntity
import com.domina.cycle.data.repository.ChecklistRepository
import com.domina.cycle.domain.pregnancy.ChecklistDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    private val repository: ChecklistRepository,
) : ViewModel() {
    init {
        viewModelScope.launch {
            if (repository.countByCategory(ChecklistDefaults.BAG) == 0) {
                ChecklistDefaults.bag.forEachIndexed { i, t -> repository.add(ChecklistDefaults.BAG, t, i) }
            }
            if (repository.countByCategory(ChecklistDefaults.BIRTH_PLAN) == 0) {
                ChecklistDefaults.birthPlan.forEachIndexed { i, t -> repository.add(ChecklistDefaults.BIRTH_PLAN, t, i) }
            }
        }
    }
    fun items(category: String): StateFlow<List<ChecklistItemEntity>> =
        repository.observeByCategory(category).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(id: Long, checked: Boolean) { viewModelScope.launch { repository.setChecked(id, checked) } }
    fun add(category: String, text: String) { if (text.isNotBlank()) viewModelScope.launch { repository.add(category, text.trim(), 999) } }
    fun delete(id: Long) { viewModelScope.launch { repository.delete(id) } }
}
