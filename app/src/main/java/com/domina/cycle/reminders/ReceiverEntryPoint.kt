package com.domina.cycle.reminders

import com.domina.cycle.data.prefs.SettingsRepository
import com.domina.cycle.data.repository.CheckInRepository
import com.domina.cycle.data.repository.DayLogRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets BroadcastReceivers pull Hilt singletons without field injection (which is finicky for receivers). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun settings(): SettingsRepository
    fun checkInRepository(): CheckInRepository
    fun dayLogRepository(): DayLogRepository
}
