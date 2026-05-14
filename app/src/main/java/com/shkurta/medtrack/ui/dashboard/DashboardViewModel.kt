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
    val id: Long,
    val medication: Medication,
    val timestamp: Long,
    val isTaken: Boolean,
    val status: String = "Pending"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _instantLogName = MutableStateFlow("")

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllMedications(),
        repository.getAllLogs(),
        _instantLogName
    ) { medications, allLogs, instantName ->
        val items = mutableListOf<ScheduleItem>()

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

        val now = System.currentTimeMillis()
        medications.forEach { med ->
            val takenToday = allLogs.any {
                it.medicationId == med.id && isSameDay(it.timestamp, now)
            }
            if (!takenToday && med.reminderEnabled) {
                items.add(
                    ScheduleItem(
                        id = med.id,
                        medication = med,
                        timestamp = med.startTime,
                        isTaken = false
                    )
                )
            }
        }

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
        val raw = _instantLogName.value
        if (raw.isBlank()) return

        viewModelScope.launch {
            val (name, timestamp) = parseInput(raw)
            if (name.isBlank()) return@launch

            val existingMed = repository.getMedicationByName(name)
            val medicationId = existingMed?.id ?: repository.saveMedication(
                Medication(
                    name = name,
                    dosage = "As needed",
                    frequency = "Instant",
                    startTime = System.currentTimeMillis(),
                    reminderEnabled = false
                )
            )

            repository.addDoseLog(
                DoseLog(
                    medicationId = medicationId,
                    timestamp = timestamp,
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

    // Parses natural-language input into (medicationName, timestamp).
    // Supported patterns: "Aspirin", "Aspirin 9am", "Aspirin tomorrow", "Aspirin May 20 at 3pm".
    private fun parseInput(input: String): Pair<String, Long> {
        var text = input.trim()
        val cal = Calendar.getInstance()

        // Time: optional "at " prefix, then "9am" / "9:30pm" / "14:00"
        val timeRegex = Regex(
            """(?:at\s+)?\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b|(?:at\s+)?\b(\d{1,2}):(\d{2})\b""",
            RegexOption.IGNORE_CASE
        )
        val timeMatch = timeRegex.find(text)
        if (timeMatch != null) {
            val g = timeMatch.groupValues
            if (g[1].isNotEmpty()) {
                // am/pm format
                val rawHour = g[1].toInt()
                val minute = if (g[2].isNotEmpty()) g[2].toInt() else 0
                val meridiem = g[3].lowercase()
                val hour = when {
                    meridiem == "pm" && rawHour != 12 -> rawHour + 12
                    meridiem == "am" && rawHour == 12 -> 0
                    else -> rawHour
                }
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
            } else {
                // 24-hour format
                cal.set(Calendar.HOUR_OF_DAY, g[4].toInt())
                cal.set(Calendar.MINUTE, g[5].toInt())
            }
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            text = text.removeRange(timeMatch.range).collapseSpaces()
        }

        // Date patterns — checked in priority order
        val tomorrowRegex = Regex("""(?:on\s+)?\btomorrow\b""", RegexOption.IGNORE_CASE)
        val todayRegex = Regex("""(?:on\s+)?\btoday\b""", RegexOption.IGNORE_CASE)
        val dowRegex = Regex(
            """(?:on\s+)?\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\b""",
            RegexOption.IGNORE_CASE
        )
        val monthDayRegex = Regex(
            """(?:on\s+)?\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})\b""",
            RegexOption.IGNORE_CASE
        )

        when {
            tomorrowRegex.containsMatchIn(text) -> {
                val m = tomorrowRegex.find(text)!!
                cal.add(Calendar.DAY_OF_YEAR, 1)
                text = text.removeRange(m.range).collapseSpaces()
            }
            todayRegex.containsMatchIn(text) -> {
                val m = todayRegex.find(text)!!
                text = text.removeRange(m.range).collapseSpaces()
            }
            dowRegex.containsMatchIn(text) -> {
                val m = dowRegex.find(text)!!
                val targetDow = parseDayOfWeek(m.groupValues[1])
                val currentDow = cal.get(Calendar.DAY_OF_WEEK)
                var daysToAdd = (targetDow - currentDow + 7) % 7
                if (daysToAdd == 0) daysToAdd = 7
                cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
                text = text.removeRange(m.range).collapseSpaces()
            }
            monthDayRegex.containsMatchIn(text) -> {
                val m = monthDayRegex.find(text)!!
                val month = parseMonth(m.groupValues[1])
                val day = m.groupValues[2].toInt()
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                // If the date is already in the past, assume next year
                if (cal.timeInMillis < System.currentTimeMillis() - 24 * 60 * 60 * 1000L) {
                    cal.add(Calendar.YEAR, 1)
                }
                text = text.removeRange(m.range).collapseSpaces()
            }
        }

        val medicationName = text.trim().ifBlank { input.trim() }
        return Pair(medicationName, cal.timeInMillis)
    }

    private fun parseDayOfWeek(token: String): Int = when (token.lowercase().take(3)) {
        "sun" -> Calendar.SUNDAY
        "mon" -> Calendar.MONDAY
        "tue" -> Calendar.TUESDAY
        "wed" -> Calendar.WEDNESDAY
        "thu" -> Calendar.THURSDAY
        "fri" -> Calendar.FRIDAY
        "sat" -> Calendar.SATURDAY
        else -> Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    private fun parseMonth(token: String): Int = when (token.lowercase().take(3)) {
        "jan" -> Calendar.JANUARY
        "feb" -> Calendar.FEBRUARY
        "mar" -> Calendar.MARCH
        "apr" -> Calendar.APRIL
        "may" -> Calendar.MAY
        "jun" -> Calendar.JUNE
        "jul" -> Calendar.JULY
        "aug" -> Calendar.AUGUST
        "sep" -> Calendar.SEPTEMBER
        "oct" -> Calendar.OCTOBER
        "nov" -> Calendar.NOVEMBER
        "dec" -> Calendar.DECEMBER
        else -> Calendar.getInstance().get(Calendar.MONTH)
    }

    private fun String.collapseSpaces() = replace(Regex("\\s+"), " ").trim()

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
