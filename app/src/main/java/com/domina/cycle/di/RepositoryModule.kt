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

    @Binds @Singleton
    abstract fun bindMedicationRepository(impl: RoomMedicationRepository): MedicationRepository

    @Binds @Singleton
    abstract fun bindAppointmentRepository(impl: RoomAppointmentRepository): AppointmentRepository

    @Binds @Singleton
    abstract fun bindKickRepository(impl: RoomKickRepository): KickRepository

    @Binds @Singleton
    abstract fun bindContractionRepository(impl: RoomContractionRepository): ContractionRepository

    @Binds @Singleton
    abstract fun bindWeightRepository(impl: RoomWeightRepository): WeightRepository

    @Binds @Singleton
    abstract fun bindChecklistRepository(impl: RoomChecklistRepository): ChecklistRepository

    @Binds @Singleton
    abstract fun bindUpdateRepository(
        impl: com.domina.cycle.data.update.GithubUpdateRepository,
    ): com.domina.cycle.data.update.UpdateRepository

    @Binds @Singleton
    abstract fun bindCheckInRepository(impl: RoomCheckInRepository): CheckInRepository
}
