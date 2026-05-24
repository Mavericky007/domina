package com.domina.cycle.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.entity.AppointmentEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedsAppointmentsDaoTest {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
    }
    @After fun tearDown() = db.close()

    @Test fun medicationCrud(): Unit = runBlocking {
        val id = db.medicationDao().insert(MedicationEntity(name = "Prenatal vitamin", timeMinutes = 540, enabled = true))
        assertThat(db.medicationDao().observeAll().first().single().name).isEqualTo("Prenatal vitamin")
        db.medicationDao().deleteById(id)
        assertThat(db.medicationDao().observeAll().first()).isEmpty()
    }

    @Test fun appointmentCrud(): Unit = runBlocking {
        val id = db.appointmentDao().insert(
            AppointmentEntity(title = "OB checkup", atEpochMillis = 1_900_000_000_000L, note = "", leadHours = 24, enabled = true))
        assertThat(db.appointmentDao().observeAll().first().single().title).isEqualTo("OB checkup")
        db.appointmentDao().deleteById(id)
        assertThat(db.appointmentDao().observeAll().first()).isEmpty()
    }
}
