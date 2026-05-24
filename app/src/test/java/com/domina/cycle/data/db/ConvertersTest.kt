package com.domina.cycle.data.db

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {
    private val c = Converters()

    @Test fun symptomsRoundTrip() {
        val list = listOf("cramps", "headache")
        assertThat(c.toSymptomList(c.fromSymptomList(list))).isEqualTo(list)
    }

    @Test fun emptySymptomsRoundTrip() {
        assertThat(c.toSymptomList(c.fromSymptomList(emptyList()))).isEmpty()
    }
}
