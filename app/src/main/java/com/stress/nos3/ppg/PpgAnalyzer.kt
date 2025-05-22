package com.stress.nos3.ppg

import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.roundToInt

class PpgAnalyzer(
    private val onHeartRateAvailable: (Double) -> Unit,
    private val onHrvAvailable: (Double) -> Unit,
    private val onSignalUpdate: (Double) -> Unit
) : ImageAnalysis.Analyzer {

    private val signalProcessor = PpgSignalProcessor()
    private var frameCount = 0
    private val framesPerCalculation = 30 // Calculate HR every 30 frames

    override fun analyze(image: ImageProxy) {
        try {
            val redMean = extractRedChannelMean(image)
            val processedValue = signalProcessor.processImageMean(redMean)
            
            onSignalUpdate(processedValue)
            
            frameCount++
            if (frameCount >= framesPerCalculation) {
                val heartRate = signalProcessor.calculateHeartRate()
                val hrv = signalProcessor.calculateHRV()
                
                if (heartRate > 0) {
                    onHeartRateAvailable(heartRate.roundToInt().toDouble())
                }
                if (hrv > 0) {
                    onHrvAvailable(hrv)
                }
                
                frameCount = 0
            }
        } catch (e: Exception) {
            // Handle any errors during analysis
        } finally {
            image.close()
        }
    }

    private fun extractRedChannelMean(image: ImageProxy): Double {
        val planes = image.planes
        if (planes.isEmpty()) return 0.0

        val buffer = planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        // For YUV format, the first plane contains the Y (luminance) data
        var total = 0.0
        var count = 0

        // Calculate the center region (middle 50% of the image)
        val centerRect = getCenterRegion(image.width, image.height)

        for (y in centerRect.top until centerRect.bottom) {
            for (x in centerRect.left until centerRect.right) {
                val index = y * image.width + x
                if (index < data.size) {
                    // Convert byte to unsigned (0-255)
                    total += (data[index].toInt() and 0xFF)
                    count++
                }
            }
        }

        return if (count > 0) total / count else 0.0
    }

    private fun getCenterRegion(width: Int, height: Int): Rect {
        val centerX = width / 2
        val centerY = height / 2
        val regionSize = minOf(width, height) / 4

        return Rect(
            centerX - regionSize,
            centerY - regionSize,
            centerX + regionSize,
            centerY + regionSize
        )
    }

    fun updateSamplingRate(frameRate: Double) {
        signalProcessor.setSamplingRate(frameRate)
    }

    fun reset() {
        signalProcessor.reset()
        frameCount = 0
    }
} 