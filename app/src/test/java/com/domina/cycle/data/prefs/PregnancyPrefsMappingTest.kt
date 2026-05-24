package com.domina.cycle.data.prefs

import com.domina.cycle.domain.pregnancy.AppMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PregnancyPrefsMappingTest {
    @Test fun appModeRoundTripsByName() {
        AppMode.entries.forEach { assertThat(AppMode.fromName(it.name)).isEqualTo(it) }
    }
    @Test fun unknownModeFallsBackToCycle() {
        assertThat(AppMode.fromName("nonsense")).isEqualTo(AppMode.CYCLE)
        assertThat(AppMode.fromName(null)).isEqualTo(AppMode.CYCLE)
    }
}
