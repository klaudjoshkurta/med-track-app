package com.shkurta.medtrack.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.medtrack.data.entity.DoseLog
import com.shkurta.medtrack.data.entity.Medication
import com.shkurta.medtrack.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val scheduleItems: Map<String, List<ScheduleItem>> = emptyMap(),
    val isLoading: Boolean = true,
    val instantLogName: String = "",
    val currentDateLabel: String = ""
)

data class ScheduleItem(
    val id: Long, // medicationId or logId
    val medication: Medication,
    val timestamp: Long,
    val isTaken: Boolean,
    val status: String = "Pending"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val _instantLogName = MutableStateFlow("")
    
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllMedications(),
        repository.getAllLogs(),
        _instantLogName
    ) { medications, allLogs, instantName ->
        val items = mutableListOf<ScheduleItem>()
        
        // 1. Add all completed logs
        allLogs.forEach { log ->
            val med = medications.find { it.id == log.medicationId }
            if (med != null) {
                items.add(
                    ScheduleItem(
                        id = log.id,
                        medication = med,
                        timestamp = log.timestamp,
                        isTaken = true,
                        status = log.status
                    )
                )
            }
        }
        
        // 2. Add upcoming scheduled doses (simplified logic for the next 7 days)
        val now = System.currentTimeMillis()
        medications.forEach { med ->
            // Only add if not already taken today (very basic logic)
            val takenToday = allLogs.any { 
                it.medicationId == med.id && isSameDay(it.timestamp, now) 
            }
            if (!takenToday && med.reminderEnabled) {
                items.add(
                    ScheduleItem(
                        id = med.id,
                        medication = med,
                        timestamp = med.startTime, // In real app, calculate next occurrence
                        isTaken = false
                    )
                )
            }
        }

        // Group by Date
        val grouped = items.sortedBy { it.timestamp }.groupBy { 
            formatDateHeader(it.timestamp) 
        }
        
        DashboardUiState(
            scheduleItems = grouped,
            isLoading = false,
            instantLogName = instantName,
            currentDateLabel = formatCurrentDateLabel()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onInstantNameChange(name: String) {
        _instantLogName.value = name
    }

    fun instantLog() {
        val name = _instantLogName.value
        if (name.isBlank()) return

        viewModelScope.launch {
            val existingMed = repository.getMedicationByName(name)
            val medicationId = if (existingMed != null) {
                existingMed.id
            } else {
                repository.saveMedication(
                    Medication(
                        name = name,
                        dosage = "As needed",
                        frequency = "Instant",
                        startTime = System.currentTimeMillis(),
                        reminderEnabled = false
                    )
                )
            }

            repository.addDoseLog(
                DoseLog(
                    medicationId = medicationId,
                    timestamp = System.currentTimeMillis(),
                    status = "Taken"
                )
            )
            _instantLogName.value = ""
        }
    }

    fun markAsTaken(medicationId: Long) {
        viewModelScope.launch {
            repository.addDoseLog(
                DoseLog(
                    medicationId = medicationId,
                    timestamp = System.currentTimeMillis(),
                    status = "Taken"
                )
            )
        }
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDateHeader(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()
        
        val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
        val monthDay = SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
        
        return if (isSameDay(timestamp, now.timeInMillis)) {
            "Today $dayOfWeek, $monthDay"
        } else {
            "$dayOfWeek, $monthDay"
        }
    }

    private fun formatCurrentDateLabel(): String {
        return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
    }
}
