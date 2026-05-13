package com.shkurta.medtrack.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shkurta.medtrack.data.dao.MedicationDao
import com.shkurta.medtrack.data.entity.DoseLog
import com.shkurta.medtrack.data.entity.Medication

@Database(entities = [Medication::class, DoseLog::class], version = 1, exportSchema = false)
abstract class MedDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        const val DATABASE_NAME = "med_track_db"
    }
}
