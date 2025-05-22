package com.stress.nos3.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.stress.nos3.ppg.PpgAnalyzer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraManager(
    private val context: Context,
    private val cameraExecutor: ExecutorService
) {
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var ppgAnalyzer: PpgAnalyzer? = null

    suspend fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        onHeartRateAvailable: (Double) -> Unit,
        onHrvAvailable: (Double) -> Unit,
        onSignalUpdate: (Double) -> Unit
    ): Result<Camera> = kotlin.runCatching {
        val cameraProvider = getCameraProvider()
        
        // Create and configure the preview use case
        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        // Create and configure the image analysis use case
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Create PPG analyzer
        ppgAnalyzer = PpgAnalyzer(
            onHeartRateAvailable = onHeartRateAvailable,
            onHrvAvailable = onHrvAvailable,
            onSignalUpdate = onSignalUpdate
        )

        imageAnalysis.setAnalyzer(cameraExecutor, ppgAnalyzer!!)

        try {
            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            this.camera = camera
            this.preview = preview
            this.imageAnalysis = imageAnalysis
            this.cameraProvider = cameraProvider

            camera
        } catch (e: Exception) {
            cleanupResources()
            throw e
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider {
        return suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    continuation.resume(cameraProviderFuture.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }, context.mainExecutor)
        }
    }

    fun stopCamera() {
        cleanupResources()
    }

    private fun cleanupResources() {
        try {
            cameraProvider?.unbindAll()
            ppgAnalyzer?.reset()
            imageAnalysis?.clearAnalyzer()
        } catch (e: Exception) {
            // Log or handle cleanup errors
        } finally {
            camera = null
            preview = null
            imageAnalysis = null
            cameraProvider = null
            ppgAnalyzer = null
        }
    }

    fun toggleTorch(enabled: Boolean) {
        try {
            camera?.cameraControl?.enableTorch(enabled)
            if (!enabled) {
                ppgAnalyzer?.reset()
            }
        } catch (e: Exception) {
            throw CameraException("Failed to toggle torch: ${e.message}", e)
        }
    }

    fun hasFlashUnit(): Boolean = camera?.cameraInfo?.hasFlashUnit() ?: false
}

class CameraException(message: String, cause: Throwable? = null) : Exception(message, cause) 