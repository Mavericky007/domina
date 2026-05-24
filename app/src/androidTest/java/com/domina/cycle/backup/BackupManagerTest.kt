package com.domina.cycle.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    private lateinit var db: AppDatabase
    private lateinit var manager: BackupManager
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        manager = BackupManager(db)
    }
    @After fun tearDown() = db.close()

    @Test fun exportThenWipeThenImportRestoresData(): Unit = runBlocking {
        db.dayLogDao().upsert(DayLogEntity(date = "2026-05-01", mood = "HAPPY", symptoms = listOf("cramps")))
        db.medicationDao().insert(MedicationEntity(name = "Prenatal vitamin", timeMinutes = 540, enabled = true))

        val out = ByteArrayOutputStream()
        manager.export(out, "pass1234".toCharArray())

        // wipe
        db.dayLogDao().clearAll(); db.medicationDao().clearAll()
        assertThat(db.dayLogDao().getAll()).isEmpty()

        manager.import(ByteArrayInputStream(out.toByteArray()), "pass1234".toCharArray())
        assertThat(db.dayLogDao().getAll().single().mood).isEqualTo("HAPPY")
        assertThat(db.dayLogDao().getAll().single().symptoms).containsExactly("cramps")
        assertThat(db.medicationDao().getAll().single().name).isEqualTo("Prenatal vitamin")
    }

    @Test(expected = Exception::class)
    fun importWithWrongPassphraseThrows(): Unit = runBlocking {
        val out = ByteArrayOutputStream()
        manager.export(out, "right".toCharArray())
        manager.import(ByteArrayInputStream(out.toByteArray()), "wrong".toCharArray())
    }
}
