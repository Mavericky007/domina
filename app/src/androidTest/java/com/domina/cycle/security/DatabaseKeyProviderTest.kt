package com.domina.cycle.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseKeyProviderTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun returnsStableNonEmptyPassphraseAcrossInstances() {
        context.getSharedPreferences("secure_db", 0).edit().clear().commit()
        val first = DatabaseKeyProvider(context).getOrCreatePassphrase()
        val second = DatabaseKeyProvider(context).getOrCreatePassphrase()

        assertThat(first).isNotEmpty()
        assertThat(first).isEqualTo(second) // persisted & decrypted identically
    }
}
