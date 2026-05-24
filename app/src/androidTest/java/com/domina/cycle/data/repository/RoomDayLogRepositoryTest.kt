package com.domina.cycle.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.model.DayLog
import com.domina.cycle.data.model.Mood
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RoomDayLogRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: DayLogRepository

    @Before fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).build()
        repo = RoomDayLogRepository(db.dayLogDao())
    }
    @After fun tearDown() = db.close()

    @Test fun savesAndLoadsDayLogWithEnumMapping(): Unit = runBlocking {
        val date = LocalDate.of(2026, 5, 24)
        repo.save(DayLog(date = date, mood = Mood.HAPPY, symptoms = listOf("cramps")))
        val loaded = repo.getByDate(date)
        assertThat(loaded?.mood).isEqualTo(Mood.HAPPY)
        assertThat(loaded?.symptoms).containsExactly("cramps")
    }
}
