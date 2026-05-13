package com.shkurta.medtrack.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shkurta.medtrack.data.entity.DoseLog
import com.shkurta.medtrack.data.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MedicationRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderManager: ReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        if (medicationId == -1L) return

        when (intent.action) {
            NotificationHelper.ACTION_TAKEN -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repository.addDoseLog(
                        DoseLog(
                            medicationId = medicationId,
                            timestamp = System.currentTimeMillis(),
                            status = "Taken"
                        )
                    )
                    notificationHelper.cancelNotification(notificationId)
                }
            }
            NotificationHelper.ACTION_SNOOZE -> {
                reminderManager.snoozeReminder(medicationId)
                notificationHelper.cancelNotification(notificationId)
            }
        }
    }
}
