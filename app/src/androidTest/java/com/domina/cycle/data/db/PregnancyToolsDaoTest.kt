package com.domina.cycle.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.entity.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PregnancyToolsDaoTest {
    private lateinit var db: AppDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
    }
    @After fun tearDown() = db.close()

    @Test fun kickAndContractionAndWeightAndChecklistCrud(): Unit = runBlocking {
        db.kickSessionDao().insert(KickSessionEntity(startMillis = 1, endMillis = 2, count = 10))
        assertThat(db.kickSessionDao().observeAll().first()).hasSize(1)

        db.contractionDao().insert(ContractionEntity(startMillis = 100, endMillis = 145))
        assertThat(db.contractionDao().observeAll().first().single().endMillis).isEqualTo(145)

        db.weightEntryDao().insert(WeightEntryEntity(dateEpochDay = 20000, weightKg = 65.5))
        assertThat(db.weightEntryDao().observeAll().first().single().weightKg).isEqualTo(65.5)

        val id = db.checklistItemDao().insert(
            ChecklistItemEntity(category = "BAG", text = "Phone charger", checked = false, sortOrder = 0))
        db.checklistItemDao().setChecked(id, true)
        assertThat(db.checklistItemDao().observeByCategory("BAG").first().single().checked).isTrue()
    }
}
