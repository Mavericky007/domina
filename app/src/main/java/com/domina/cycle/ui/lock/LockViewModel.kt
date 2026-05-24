package com.domina.cycle.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    fun onPinEntered(pin: String) {
        viewModelScope.launch {
            val stored = settings.pinHash.first()
            if (stored == null) { settings.setPinHash(PinManager.hash(pin)); _unlocked.value = true }
            else if (PinManager.verify(pin, stored)) _unlocked.value = true
        }
    }
    fun onBiometricSuccess() { _unlocked.value = true }
}
