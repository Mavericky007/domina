package com.domina.cycle.di

import com.domina.cycle.domain.reminders.ReminderSettings
import com.domina.cycle.reminders.ReminderSettingsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides a default ReminderSettingsProvider (all-defaults) for Task 4.
 * Task 5 will replace this with the DataStore-backed SettingsReminderProvider.
 */
@Module
@InstallIn(SingletonComponent::class)
object ReminderModule {
    @Provides
    @Singleton
    fun provideReminderSettingsProvider(): ReminderSettingsProvider =
        object : ReminderSettingsProvider {
            override suspend fun current(): ReminderSettings = ReminderSettings()
        }
}
