package com.stress.nos3.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "measurements")
data class MeasurementRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Instant,
    val heartRate: Double,
    val hrv: Double,
    val signalQuality: Double,
    val duration: Long // measurement duration in milliseconds
) 