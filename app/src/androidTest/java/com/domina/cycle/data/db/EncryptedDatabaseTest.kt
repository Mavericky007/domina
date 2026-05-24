package com.domina.cycle.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domina.cycle.data.db.entity.DayLogEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val dbName = "enc-test.db"

    private fun open(pass: ByteArray): AppDatabase {
        System.loadLibrary("sqlcipher")
        return Room.databaseBuilder(ctx, AppDatabase::class.java, dbName)
            .openHelperFactory(SupportOpenHelperFactory(pass))
            .build()
    }

    @Test fun writesAndReadsBackWithCorrectKey() = runBlocking {
        ctx.deleteDatabase(dbName)
        val key = "correct-horse".toByteArray()
        val db = open(key)
        db.dayLogDao().upsert(DayLogEntity(date = "2026-05-24", mood = "HAPPY"))
        db.close()

        val reopened = open(key)
        assertThat(reopened.dayLogDao().getByDate("2026-05-24")?.mood).isEqualTo("HAPPY")
        reopened.close()
    }

    @Test(expected = Exception::class)
    fun wrongKeyCannotOpen(): Unit = runBlocking {
        ctx.deleteDatabase(dbName)
        open("correct-horse".toByteArray()).apply {
            dayLogDao().upsert(DayLogEntity(date = "2026-05-24"))
            close()
        }
        val bad = open("wrong-key".toByteArray())
        bad.dayLogDao().getByDate("2026-05-24") // must throw: file is encrypted
    }
}
