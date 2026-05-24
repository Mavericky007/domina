package com.domina.cycle.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupCodecTest {
    @Test fun roundTripsTablesWithSpecialCharacters() {
        val data = linkedMapOf(
            "day_logs" to listOf(
                listOf("2026-05-01", "HAPPY", "crampsheadache"),
                listOf("2026-05-02", "", "note with | pipe and \n newline"),
            ),
            "medications" to listOf(listOf("1", "Prenatal vitamin", "540")),
            "empty_table" to emptyList(),
        )
        val encoded = BackupCodec.encode(data)
        val decoded = BackupCodec.decode(encoded)
        assertThat(decoded).isEqualTo(data)
    }

    @Test fun rejectsUnknownHeader() {
        try { BackupCodec.decode("NOT-A-BACKUP\n"); assertThat(false).isTrue() }
        catch (e: IllegalArgumentException) { assertThat(e).hasMessageThat().contains("header") }
    }
}
