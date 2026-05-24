package com.domina.cycle.di

import android.content.Context
import androidx.room.Room
import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.db.CycleEventDao
import com.domina.cycle.data.db.DayLogDao
import com.domina.cycle.security.DatabaseKeyProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = DatabaseKeyProvider(context).getOrCreatePassphrase()
        return Room.databaseBuilder(context, AppDatabase::class.java, "domina.db")
            .openHelperFactory(SupportOpenHelperFactory(passphrase))
            .build()
    }

    @Provides fun provideDayLogDao(db: AppDatabase): DayLogDao = db.dayLogDao()
    @Provides fun provideCycleEventDao(db: AppDatabase): CycleEventDao = db.cycleEventDao()
}
