package com.domina.cycle.backup

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupCryptoTest {
    @Test fun sealThenOpenRoundTrips() {
        val plaintext = "DOMINA-BACKUP-1\n#day_logs\nrow".toByteArray()
        val sealed = BackupCrypto.seal(plaintext, "correct horse".toCharArray())
        val opened = BackupCrypto.open(sealed, "correct horse".toCharArray())
        assertThat(String(opened)).isEqualTo(String(plaintext))
    }

    @Test fun wrongPassphraseFails() {
        val sealed = BackupCrypto.seal("secret".toByteArray(), "right".toCharArray())
        try { BackupCrypto.open(sealed, "wrong".toCharArray()); assertThat(false).isTrue() }
        catch (e: Exception) { assertThat(true).isTrue() } // AEADBadTagException or similar
    }

    @Test fun differentSaltsProduceDifferentCiphertext() {
        val a = BackupCrypto.seal("x".toByteArray(), "p".toCharArray())
        val b = BackupCrypto.seal("x".toByteArray(), "p".toCharArray())
        assertThat(a.toList()).isNotEqualTo(b.toList())
    }
}
