package com.shkurta.medtrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.medtrack.data.entity.DoseLog
import com.shkurta.medtrack.data.entity.Medication
import com.shkurta.medtrack.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.*
import javax.inject.Inject

data class HistoryUiState(
    val logs: List<LogWithMedication> = emptyList(),
    val adherenceRate: Float = 0f,
    val streak: Int = 0,
    val isLoading: Boolean = true
)

data class LogWithMedication(
    val log: DoseLog,
    val medicationName: String
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.getAllMedications(),
        repository.getLogsForDay(0, Long.MAX_VALUE) // Simplified for all history
    ) { medications, logs ->
        val logWithMeds = logs.map { log ->
            val medName = medications.find { it.id == log.medicationId }?.name ?: "Unknown"
            LogWithMedication(log, medName)
        }

        val takenCount = logs.count { it.status == "Taken" }
        val totalCount = logs.size
        val adherence = if (totalCount > 0) takenCount.toFloat() / totalCount else 0f
        
        // Simplified streak calculation (consecutive days with at least one "Taken" dose)
        val streak = calculateStreak(logs)

        HistoryUiState(
            logs = logWithMeds,
            adherenceRate = adherence,
            streak = streak,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState()
    )

    private fun calculateStreak(logs: List<DoseLog>): Int {
        if (logs.isEmpty()) return 0
        val sortedDates = logs.filter { it.status == "Taken" }
            .map { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }.distinct().sortedDescending()
        
        if (sortedDates.isEmpty()) return 0
        
        var currentStreak = 0
        val oneDay = 24 * 60 * 60 * 1000L
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        var expectedDate = today
        for (date in sortedDates) {
            if (date == expectedDate || date == expectedDate - oneDay) {
                currentStreak++
                expectedDate = date
            } else {
                break
            }
        }
        return currentStreak
    }
}
