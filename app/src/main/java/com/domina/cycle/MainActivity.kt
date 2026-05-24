package com.domina.cycle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.security.BiometricAuthenticator
import com.domina.cycle.ui.lock.LockScreen
import com.domina.cycle.ui.lock.LockViewModel
import com.domina.cycle.ui.nav.AppNav
import com.domina.cycle.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settings: SettingsRepository
    private val lockVm: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bio = BiometricAuthenticator(this)
        setContent {
            val theme by settings.theme.collectAsStateWithLifecycle(
                initialValue = com.domina.cycle.data.prefs.ThemePreference.SOFT_SWEET
            )
            val unlocked by lockVm.unlocked.collectAsStateWithLifecycle()
            var hasPin by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) { hasPin = settings.pinHash.first() != null }

            AppTheme(theme) {
                if (unlocked) {
                    AppNav()
                } else {
                    LockScreen(
                        hasPin = hasPin == true,
                        onPinEntered = lockVm::onPinEntered,
                        onUseBiometric = {
                            if (bio.isAvailable()) lifecycleScope.launch {
                                if (bio.authenticate()) lockVm.onBiometricSuccess()
                            }
                        },
                    )
                }
            }
        }
    }
}
