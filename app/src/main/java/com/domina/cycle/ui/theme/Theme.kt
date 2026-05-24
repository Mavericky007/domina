package com.domina.cycle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.domina.cycle.data.prefs.ThemePreference

private fun schemeFor(theme: ThemePreference) = when (theme) {
    ThemePreference.SOFT_SWEET -> lightColorScheme(
        primary = SoftPrimary, secondary = SoftSecondary,
        background = SoftBackground, surface = SoftSurface,
    )
    ThemePreference.BRIGHT_JOYFUL -> lightColorScheme(
        primary = BrightPrimary, secondary = BrightSecondary,
        background = BrightBackground, surface = BrightSurface,
    )
    ThemePreference.WARM_COZY -> lightColorScheme(
        primary = CozyPrimary, secondary = CozySecondary,
        background = CozyBackground, surface = CozySurface,
    )
}

@Composable
fun AppTheme(theme: ThemePreference, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeFor(theme), typography = AppTypography, content = content)
}
