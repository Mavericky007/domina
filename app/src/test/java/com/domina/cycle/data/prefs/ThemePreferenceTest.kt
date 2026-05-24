package com.domina.cycle.data.prefs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemePreferenceTest {
    @Test fun mapsEachThemeToStableKeyAndBack() {
        ThemePreference.entries.forEach { theme ->
            assertThat(ThemePreference.fromKey(theme.key)).isEqualTo(theme)
        }
    }
    @Test fun unknownKeyFallsBackToSoftSweet() {
        assertThat(ThemePreference.fromKey("garbage")).isEqualTo(ThemePreference.SOFT_SWEET)
    }
}
