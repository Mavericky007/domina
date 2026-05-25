package com.domina.cycle

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.reminders.ReminderManager
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
    @Inject lateinit var reminderManager: ReminderManager
    private val lockVm: LockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val bio = BiometricAuthenticator(this)
        setContent {
            val theme by settings.theme.collectAsStateWithLifecycle(
                initialValue = com.domina.cycle.data.prefs.ThemePreference.SOFT_SWEET
            )
            val unlocked by lockVm.unlocked.collectAsStateWithLifecycle()
            var hasPin by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) { hasPin = settings.pinHash.first() != null }
            var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(unlocked) { if (unlocked) onboardingDone = settings.onboardingComplete.first() }

            // Request POST_NOTIFICATIONS permission on API 33+ once unlocked
            val notifLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            LaunchedEffect(unlocked) {
                if (unlocked && android.os.Build.VERSION.SDK_INT >= 33) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (unlocked) {
                    lifecycleScope.launch { reminderManager.reschedule() }
                }
            }

            AppTheme(theme) {
                if (unlocked) {
                    when (onboardingDone) {
                        false -> com.domina.cycle.ui.onboarding.OnboardingScreen(onDone = { onboardingDone = true })
                        true -> AppNav()
                        null -> {}  // brief: reading the flag
                    }
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
