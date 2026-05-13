package com.shkurta.medtrack.reminder

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shkurta.medtrack.data.repository.MedicationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: MedicationRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val medicationId = inputData.getLong("medication_id", -1L)
        if (medicationId == -1L) return Result.failure()

        val medication = repository.getMedicationById(medicationId)
        if (medication != null && medication.reminderEnabled) {
            notificationHelper.showReminderNotification(
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = medication.dosage
            )
        }

        return Result.success()
    }
}
