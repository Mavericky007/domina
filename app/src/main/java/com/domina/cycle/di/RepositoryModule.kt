package com.domina.cycle.di

import com.domina.cycle.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindDayLogRepository(impl: RoomDayLogRepository): DayLogRepository

    @Binds @Singleton
    abstract fun bindCycleRepository(impl: RoomCycleRepository): CycleRepository
}
