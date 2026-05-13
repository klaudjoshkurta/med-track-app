package com.shkurta.medtrack.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shkurta.medtrack.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportFile: File? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: MedicationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            
            val medications = repository.getAllMedications().first()
            val logs = repository.getLogsForDay(0, Long.MAX_VALUE).first()
            
            val root = JSONObject()
            val medArray = JSONArray()
            medications.forEach { med ->
                medArray.put(JSONObject().apply {
                    put("name", med.name)
                    put("dosage", med.dosage)
                    put("frequency", med.frequency)
                })
            }
            root.put("medications", medArray)
            
            val logArray = JSONArray()
            logs.forEach { log ->
                logArray.put(JSONObject().apply {
                    put("medicationId", log.medicationId)
                    put("timestamp", log.timestamp)
                    put("status", log.status)
                })
            }
            root.put("logs", logArray)
            
            val file = File(context.cacheDir, "medtrack_backup.json")
            file.writeText(root.toString(4))
            
            _uiState.update { it.copy(isExporting = false, exportFile = file) }
            shareFile(file)
        }
    }

    private fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Export Backup").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun resetData() {
        viewModelScope.launch {
            // repository.clearAll() // Need to implement in repository
        }
    }
}
