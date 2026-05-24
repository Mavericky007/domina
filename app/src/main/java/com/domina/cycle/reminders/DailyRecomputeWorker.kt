package com.domina.cycle.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyRecomputeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderManager: ReminderManager,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        reminderManager.reschedule()
        return Result.success()
    }
}
