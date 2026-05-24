package com.domina.cycle.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.domina.cycle.data.prefs.ThemePreference

private val SoftSweet = lightColorScheme(
    primary = Color(0xFF8E5BBF), onPrimary = Color.White,
    primaryContainer = Color(0xFFEFE2FF), onPrimaryContainer = Color(0xFF2E0F4F),
    secondary = Color(0xFFC76B9A), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0EE), onSecondaryContainer = Color(0xFF3E0023),
    tertiary = Color(0xFFE08A6B), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0D2), onTertiaryContainer = Color(0xFF3A1200),
    background = Color(0xFFFFF6FB), onBackground = Color(0xFF221721),
    surface = Color(0xFFFFF6FB), onSurface = Color(0xFF221721),
    surfaceVariant = Color(0xFFF3E3EF), onSurfaceVariant = Color(0xFF5B4B57),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFCEFF7),
    surfaceContainer = Color(0xFFF8E9F2), surfaceContainerHigh = Color(0xFFF3E3EF),
    surfaceContainerHighest = Color(0xFFEDDCE9),
    outline = Color(0xFF8C7A88), outlineVariant = Color(0xFFDEC9D8),
)

private val BrightJoyful = lightColorScheme(
    primary = Color(0xFF12A594), onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F2E8), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFFE63E5C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9DF), onSecondaryContainer = Color(0xFF40000D),
    tertiary = Color(0xFFC79200), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8A3), onTertiaryContainer = Color(0xFF241A00),
    background = Color(0xFFFFF8F0), onBackground = Color(0xFF211B16),
    surface = Color(0xFFFFF8F0), onSurface = Color(0xFF211B16),
    surfaceVariant = Color(0xFFFCE7D6), onSurfaceVariant = Color(0xFF5C4633),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFFF2E6),
    surfaceContainer = Color(0xFFFCEDDD), surfaceContainerHigh = Color(0xFFF8E6D2),
    surfaceContainerHighest = Color(0xFFF3DFC8),
    outline = Color(0xFF8C7259), outlineVariant = Color(0xFFE6CDB4),
)

private val WarmCozy = lightColorScheme(
    primary = Color(0xFF5E7A4F), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EBC8), onPrimaryContainer = Color(0xFF14210B),
    secondary = Color(0xFFA8704F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC4), onSecondaryContainer = Color(0xFF351300),
    tertiary = Color(0xFF9A7B2E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E6B5), onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFBF6EC), onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFBF6EC), onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFE9E4D2), onSurfaceVariant = Color(0xFF4B4739),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF6F1E6),
    surfaceContainer = Color(0xFFF1ECE0), surfaceContainerHigh = Color(0xFFEBE6D9),
    surfaceContainerHighest = Color(0xFFE5E0D3),
    outline = Color(0xFF7C7867), outlineVariant = Color(0xFFCEC8B5),
)

private fun schemeFor(theme: ThemePreference) = when (theme) {
    ThemePreference.SOFT_SWEET -> SoftSweet
    ThemePreference.BRIGHT_JOYFUL -> BrightJoyful
    ThemePreference.WARM_COZY -> WarmCozy
}

@Composable
fun AppTheme(theme: ThemePreference, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = schemeFor(theme), typography = AppTypography, content = content)
}
