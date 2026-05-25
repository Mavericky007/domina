package com.domina.cycle.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChangePinViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    /** Overwrites the stored PIN with [pin]; no old-PIN check, so it also recovers a lost PIN. */
    fun save(pin: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            val hash = withContext(Dispatchers.Default) { PinManager.hash(pin) }
            settings.setPinHash(hash)
            onSaved()
        }
    }
}
