package com.shkurta.medtrack.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "medications")
@Serializable
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String, // e.g., "Daily", "Weekly", "Custom"
    val notes: String = "",
    val reminderEnabled: Boolean = true,
    val startTime: Long, // Timestamp for the first dose or preferred time
    val createdAt: Long = System.currentTimeMillis()
)
