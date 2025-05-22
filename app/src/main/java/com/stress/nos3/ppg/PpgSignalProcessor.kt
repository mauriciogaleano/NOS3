package com.stress.nos3.ppg

import kotlin.math.abs
import kotlin.math.sqrt

class PpgSignalProcessor {
    private val windowSize = 250 // 5 seconds at 50Hz sampling rate
    private val movingAverageSize = 5
    private val peakDetectionThreshold = 0.6
    private val minPeakDistance = 20 // Minimum distance between peaks (in samples)

    private val signalBuffer = ArrayDeque<Double>(windowSize)
    private var lastPeakIndex = -minPeakDistance
    private var samplingRateHz = 50.0 // Default sampling rate

    fun setSamplingRate(rate: Double) {
        samplingRateHz = rate
    }

    fun processImageMean(redChannelMean: Double): Double {
        // Add new value to buffer
        signalBuffer.add(redChannelMean)
        if (signalBuffer.size > windowSize) {
            signalBuffer.removeFirst()
        }

        // Apply moving average filter
        return if (signalBuffer.size >= movingAverageSize) {
            signalBuffer.takeLast(movingAverageSize).average()
        } else {
            redChannelMean
        }
    }

    fun detectPeaks(): List<Int> {
        if (signalBuffer.size < windowSize) return emptyList()

        val signal = signalBuffer.toList()
        val peaks = mutableListOf<Int>()
        
        // Normalize signal
        val normalizedSignal = normalizeSignal(signal)

        // Find peaks
        for (i in 1 until normalizedSignal.size - 1) {
            if (isPeak(normalizedSignal, i) && i - lastPeakIndex >= minPeakDistance) {
                peaks.add(i)
                lastPeakIndex = i
            }
        }

        return peaks
    }

    fun calculateHeartRate(): Double {
        val peaks = detectPeaks()
        if (peaks.size < 2) return 0.0

        // Calculate average time between peaks
        val averageInterval = peaks.zipWithNext { a, b -> b - a }.average()
        
        // Convert to heart rate in BPM
        return 60.0 * samplingRateHz / averageInterval
    }

    fun calculateHRV(): Double {
        val peaks = detectPeaks()
        if (peaks.size < 3) return 0.0

        // Calculate RR intervals in milliseconds
        val rrIntervals = peaks.zipWithNext { a, b -> 
            ((b - a) / samplingRateHz) * 1000 
        }

        // Calculate RMSSD (Root Mean Square of Successive Differences)
        val successiveDiffs = rrIntervals.zipWithNext { a, b -> abs(b - a) }
        val squaredDiffs = successiveDiffs.map { it * it }
        return sqrt(squaredDiffs.average())
    }

    private fun normalizeSignal(signal: List<Double>): List<Double> {
        val min = signal.minOrNull() ?: 0.0
        val max = signal.maxOrNull() ?: 1.0
        val range = max - min
        return if (range > 0) {
            signal.map { (it - min) / range }
        } else {
            signal.map { 0.0 }
        }
    }

    private fun isPeak(signal: List<Double>, index: Int): Boolean {
        return signal[index] > signal[index - 1] &&
               signal[index] > signal[index + 1] &&
               signal[index] > peakDetectionThreshold
    }

    fun reset() {
        signalBuffer.clear()
        lastPeakIndex = -minPeakDistance
    }
} 