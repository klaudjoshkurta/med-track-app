package com.shkurta.medtrack.reminder

import android.content.Context
import androidx.work.*
import com.shkurta.medtrack.data.entity.Medication
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleReminder(medication: Medication) {
        if (!medication.reminderEnabled) {
            cancelReminder(medication.id)
            return
        }

        val delay = calculateInitialDelay(medication.startTime)
        
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("medication_id" to medication.id))
            .addTag("reminder_${medication.id}")
            .build()

        workManager.enqueueUniqueWork(
            "medication_${medication.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun snoozeReminder(medicationId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setInputData(workDataOf("medication_id" to medicationId))
            .addTag("reminder_$medicationId")
            .build()

        workManager.enqueueUniqueWork(
            "medication_$medicationId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelReminder(medicationId: Long) {
        workManager.cancelUniqueWork("medication_$medicationId")
    }

    private fun calculateInitialDelay(startTime: Long): Long {
        val now = System.currentTimeMillis()
        return if (startTime > now) {
            startTime - now
        } else {
            // If the time is in the past, schedule for tomorrow (simplified for now)
            startTime + TimeUnit.DAYS.toMillis(1) - now
        }
    }
}
