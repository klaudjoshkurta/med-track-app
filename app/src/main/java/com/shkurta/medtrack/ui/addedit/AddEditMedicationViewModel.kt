package com.shkurta.medtrack.ui.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.medtrack.data.entity.Medication
import com.shkurta.medtrack.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val name: String = "",
    val dosage: String = "",
    val notes: String = "",
    val frequency: String = "Daily",
    val reminderEnabled: Boolean = true,
    val startTime: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditMedicationViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    fun loadMedication(id: Long?) {
        if (id == null) return
        viewModelScope.launch {
            repository.getMedicationById(id)?.let { med ->
                _uiState.update {
                    it.copy(
                        name = med.name,
                        dosage = med.dosage,
                        notes = med.notes,
                        frequency = med.frequency,
                        reminderEnabled = med.reminderEnabled,
                        startTime = med.startTime
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onDosageChange(dosage: String) = _uiState.update { it.copy(dosage = dosage) }
    fun onNotesChange(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun onFrequencyChange(frequency: String) = _uiState.update { it.copy(frequency = frequency) }
    fun onReminderToggle(enabled: Boolean) = _uiState.update { it.copy(reminderEnabled = enabled) }
    fun onTimeChange(timestamp: Long) = _uiState.update { it.copy(startTime = timestamp) }

    fun saveMedication(id: Long?) {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) {
            _uiState.update { it.copy(error = "Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val medication = Medication(
                id = id ?: 0L,
                name = currentState.name,
                dosage = currentState.dosage,
                frequency = currentState.frequency,
                notes = currentState.notes,
                reminderEnabled = currentState.reminderEnabled,
                startTime = currentState.startTime
            )
            repository.saveMedication(medication)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
