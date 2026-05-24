package com.domina.cycle.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PinManagerTest {
    @Test fun correctPinVerifies() {
        val stored = PinManager.hash("1234")
        assertThat(PinManager.verify("1234", stored)).isTrue()
    }
    @Test fun wrongPinFails() {
        val stored = PinManager.hash("1234")
        assertThat(PinManager.verify("0000", stored)).isFalse()
    }
    @Test fun sameInputDifferentSaltDifferentHash() {
        assertThat(PinManager.hash("1234")).isNotEqualTo(PinManager.hash("1234"))
    }
}
