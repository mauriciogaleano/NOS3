package com.stress.nos3.viewmodel

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.stress.nos3.camera.CameraException
import com.stress.nos3.camera.CameraManager
import com.stress.nos3.data.AppDatabase
import com.stress.nos3.data.MeasurementRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.ExecutorService

class MainViewModel(
    application: Application,
    private val cameraExecutor: ExecutorService
) : AndroidViewModel(application) {
    
    private val cameraManager = CameraManager(application, cameraExecutor)
    private val database = AppDatabase.getDatabase(application)
    private val measurementDao = database.measurementDao()

    private val _cameraState = MutableStateFlow<CameraState>(CameraState.Initial)
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _measurementState = MutableStateFlow<MeasurementState>(MeasurementState.Idle)
    val measurementState: StateFlow<MeasurementState> = _measurementState.asStateFlow()

    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _hrv = MutableStateFlow(0.0)
    val hrv: StateFlow<Double> = _hrv.asStateFlow()

    private val _signalValues = MutableStateFlow<List<Double>>(emptyList())
    val signalValues: StateFlow<List<Double>> = _signalValues.asStateFlow()

    private val _measurementHistory = MutableStateFlow<List<MeasurementRecord>>(emptyList())
    val measurementHistory: StateFlow<List<MeasurementRecord>> = _measurementHistory.asStateFlow()

    private val _averageHeartRate = MutableStateFlow(0.0)
    val averageHeartRate: StateFlow<Double> = _averageHeartRate.asStateFlow()

    private val _averageHrv = MutableStateFlow(0.0)
    val averageHrv: StateFlow<Double> = _averageHrv.asStateFlow()

    private var measurementStartTime: Instant? = null
    private val maxSignalPoints = 100

    init {
        viewModelScope.launch {
            // Load measurement history
            measurementDao.getAllMeasurements().collect {
                _measurementHistory.value = it
            }

            // Calculate averages for the last 24 hours
            val oneDayAgo = Instant.now().toEpochMilli() - (24 * 60 * 60 * 1000)
            measurementDao.getAverageHeartRate(oneDayAgo).collect {
                _averageHeartRate.value = it
            }
            measurementDao.getAverageHrv(oneDayAgo).collect {
                _averageHrv.value = it
            }
        }
    }

    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            try {
                _cameraState.value = CameraState.Starting
                cameraManager.startCamera(
                    previewView = previewView,
                    lifecycleOwner = lifecycleOwner,
                    onHeartRateAvailable = { hr -> _heartRate.value = hr },
                    onHrvAvailable = { hrvValue -> _hrv.value = hrvValue },
                    onSignalUpdate = { value ->
                        _signalValues.value = _signalValues.value.toMutableList().apply {
                            add(value)
                            if (size > maxSignalPoints) removeAt(0)
                        }
                    }
                ).onSuccess { camera ->
                    _cameraState.value = CameraState.Started(hasFlash = camera.cameraInfo.hasFlashUnit())
                }.onFailure { error ->
                    handleCameraError(error)
                }
            } catch (e: Exception) {
                handleCameraError(e)
            }
        }
    }

    fun toggleMeasurement() {
        viewModelScope.launch {
            try {
                val newState = _measurementState.value != MeasurementState.Measuring
                if (newState) {
                    _measurementState.value = MeasurementState.Measuring
                    measurementStartTime = Instant.now()
                    resetMeasurements()
                } else {
                    _measurementState.value = MeasurementState.Idle
                    saveMeasurement()
                }
                cameraManager.toggleTorch(newState)
            } catch (e: CameraException) {
                _measurementState.value = MeasurementState.Error(e.message ?: "Failed to toggle measurement")
            }
        }
    }

    private fun saveMeasurement() {
        viewModelScope.launch {
            measurementStartTime?.let { startTime ->
                val endTime = Instant.now()
                val duration = endTime.toEpochMilli() - startTime.toEpochMilli()
                val signalQuality = calculateSignalQuality()

                val measurement = MeasurementRecord(
                    timestamp = endTime,
                    heartRate = _heartRate.value,
                    hrv = _hrv.value,
                    signalQuality = signalQuality,
                    duration = duration
                )

                measurementDao.insertMeasurement(measurement)
            }
            measurementStartTime = null
        }
    }

    private fun calculateSignalQuality(): Double {
        // Simple signal quality metric based on signal variance
        val values = _signalValues.value
        if (values.isEmpty()) return 0.0

        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        
        // Normalize to 0-1 range, where lower variance means better quality
        return 1.0 / (1.0 + variance)
    }

    private fun resetMeasurements() {
        _heartRate.value = 0.0
        _hrv.value = 0.0
        _signalValues.value = emptyList()
    }

    private fun handleCameraError(error: Throwable) {
        _cameraState.value = CameraState.Error(
            message = error.message ?: "Unknown camera error",
            isRecoverable = true
        )
    }

    fun retryCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        startCamera(previewView, lifecycleOwner)
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager.stopCamera()
    }
}

sealed class CameraState {
    object Initial : CameraState()
    object Starting : CameraState()
    data class Started(val hasFlash: Boolean) : CameraState()
    data class Error(val message: String, val isRecoverable: Boolean = true) : CameraState()
}

sealed class MeasurementState {
    object Idle : MeasurementState()
    object Measuring : MeasurementState()
    data class Error(val message: String) : MeasurementState()
} 