package com.shkurta.medtrack.data.repository

import com.shkurta.medtrack.data.dao.MedicationDao
import com.shkurta.medtrack.data.entity.DoseLog
import com.shkurta.medtrack.data.entity.Medication
import com.shkurta.medtrack.reminder.ReminderManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface MedicationRepository {
    fun getAllMedications(): Flow<List<Medication>>
    suspend fun getMedicationById(id: Long): Medication?
    suspend fun getMedicationByName(name: String): Medication?
    suspend fun saveMedication(medication: Medication): Long
    suspend fun deleteMedication(medication: Medication)
    fun getLogsForMedication(medicationId: Long): Flow<List<DoseLog>>
    suspend fun addDoseLog(doseLog: DoseLog)
    fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DoseLog>>
    fun getAllLogs(): Flow<List<DoseLog>>
}

@Singleton
class MedicationRepositoryImpl @Inject constructor(
    private val medicationDao: MedicationDao,
    private val reminderManager: ReminderManager
) : MedicationRepository {
    override fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAllMedications()

    override suspend fun getMedicationById(id: Long): Medication? = medicationDao.getMedicationById(id)

    override suspend fun getMedicationByName(name: String): Medication? = medicationDao.getMedicationByName(name)

    override suspend fun saveMedication(medication: Medication): Long {
        val id = if (medication.id == 0L) {
            medicationDao.insertMedication(medication)
        } else {
            medicationDao.updateMedication(medication)
            medication.id
        }
        
        // Schedule reminder
        val updatedMedication = medication.copy(id = id)
        reminderManager.scheduleReminder(updatedMedication)
        
        return id
    }

    override suspend fun deleteMedication(medication: Medication) {
        medicationDao.deleteMedication(medication)
        reminderManager.cancelReminder(medication.id)
    }

    override fun getLogsForMedication(medicationId: Long): Flow<List<DoseLog>> = medicationDao.getLogsForMedication(medicationId)

    override suspend fun addDoseLog(doseLog: DoseLog) = medicationDao.insertDoseLog(doseLog)

    override fun getLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<DoseLog>> = medicationDao.getLogsForDay(startOfDay, endOfDay)

    override fun getAllLogs(): Flow<List<DoseLog>> = medicationDao.getLogsForDay(0, Long.MAX_VALUE)
}
