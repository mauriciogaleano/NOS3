package com.stress.nos3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementDao {
    @Insert
    suspend fun insertMeasurement(measurement: MeasurementRecord)

    @Query("SELECT * FROM measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<MeasurementRecord>>

    @Query("SELECT * FROM measurements WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getMeasurementsInTimeRange(startTime: Long, endTime: Long): Flow<List<MeasurementRecord>>

    @Query("SELECT AVG(heartRate) FROM measurements WHERE timestamp >= :startTime")
    fun getAverageHeartRate(startTime: Long): Flow<Double>

    @Query("SELECT AVG(hrv) FROM measurements WHERE timestamp >= :startTime")
    fun getAverageHrv(startTime: Long): Flow<Double>
} 