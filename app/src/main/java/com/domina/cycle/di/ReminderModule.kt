package com.domina.cycle.di

import com.domina.cycle.data.prefs.SettingsReminderProvider
import com.domina.cycle.reminders.ReminderSettingsProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds @Singleton
    abstract fun bindReminderSettingsProvider(impl: SettingsReminderProvider): ReminderSettingsProvider
}
