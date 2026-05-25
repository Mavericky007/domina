package com.domina.cycle.domain.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VersionCompareTest {
    @Test fun newerMinor() { assertThat(VersionCompare.isNewer("1.1", "1.2")).isTrue() }
    @Test fun sameVersionIsNotNewer() { assertThat(VersionCompare.isNewer("1.2", "1.2")).isFalse() }
    @Test fun olderIsNotNewer() { assertThat(VersionCompare.isNewer("1.2", "1.1")).isFalse() }
    @Test fun tolerantOfVPrefix() { assertThat(VersionCompare.isNewer("1.2", "v1.3")).isTrue() }
    @Test fun numericSegmentsNotLexical() { assertThat(VersionCompare.isNewer("1.9", "1.10")).isTrue() }
    @Test fun ignoresSuffix() { assertThat(VersionCompare.isNewer("1.2", "1.2-beta")).isFalse() }
    @Test fun majorBump() { assertThat(VersionCompare.isNewer("1.9", "2.0")).isTrue() }
    @Test fun differentLengths() { assertThat(VersionCompare.isNewer("1.2", "1.2.1")).isTrue() }
}
