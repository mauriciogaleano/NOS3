package com.stress.nos3

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.lifecycle.LifecycleOwner
import com.stress.nos3.ui.components.MeasurementHistory
import com.stress.nos3.ui.theme.Nos3Theme
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val healthPermissions = setOf(
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
)

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectClient: HealthConnectClient
    private lateinit var cameraExecutor: ExecutorService
    private var showPermissionRationale by mutableStateOf(false)

    private val healthConnectRequestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                showToast("Health Connect permissions granted")
                initializeHealthConnect()
            } else {
                showToast("Health Connect permissions denied")
            }
        }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showToast("Camera permission granted")
            initializeCamera()
        } else {
            showToast("Camera permission denied")
            showPermissionRationale = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isHealthConnectAvailable()) {
            showToast("Health Connect is not available on this device")
        } else {
            healthConnectClient = HealthConnectClient.getOrCreate(this)
            requestHealthConnectPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermission()

        enableEdgeToEdge()
        setContent {
            Nos3Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(
                        showPermissionRationale = showPermissionRationale,
                        onPermissionRationaleConfirm = {
                            showPermissionRationale = false
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onPermissionRationaleDismiss = {
                            showPermissionRationale = false
                        },
                        cameraExecutor = cameraExecutor
                    )
                }
            }
        }
    }

    private fun requestHealthConnectPermissions() {
        val permissionStrings = healthPermissions.toTypedArray()
        healthConnectRequestPermissions.launch(permissionStrings)
    }

    private fun isHealthConnectAvailable(): Boolean {
        return try {
            HealthConnectClient.getSdkStatus(applicationContext) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            false
        }
    }

    private fun initializeHealthConnect() {
        // Initialize Health Connect features here
    }

    private fun initializeCamera() {
        // Initialize camera features here
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationale = true
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(
    showPermissionRationale: Boolean,
    onPermissionRationaleConfirm: () -> Unit,
    onPermissionRationaleDismiss: () -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel = remember { MainViewModel(context.applicationContext as Application, cameraExecutor) }
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Camera", "History")

    Scaffold(
        topBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.PhotoCamera
                                    else -> Icons.Default.History
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> CameraScreen(
                    viewModel = viewModel,
                    showPermissionRationale = showPermissionRationale,
                    onPermissionRationaleConfirm = onPermissionRationaleConfirm,
                    onPermissionRationaleDismiss = onPermissionRationaleDismiss
                )
                1 -> MeasurementHistory(
                    measurements = viewModel.measurementHistory.collectAsState().value,
                    averageHeartRate = viewModel.averageHeartRate.collectAsState().value,
                    averageHrv = viewModel.averageHrv.collectAsState().value
                )
            }
        }
    }
}

@Composable
private fun CameraScreen(
    viewModel: MainViewModel,
    showPermissionRationale: Boolean,
    onPermissionRationaleConfirm: () -> Unit,
    onPermissionRationaleDismiss: () -> Unit
) {
    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onConfirm = onPermissionRationaleConfirm,
            onDismiss = onPermissionRationaleDismiss
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        CameraPreviewContent(
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

@Composable
private fun MeasurementsCard(
    heartRate: Double,
    hrv: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Heart Rate: ${heartRate.roundToInt()} BPM",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HRV: ${String.format("%.2f", hrv)} ms",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SignalVisualizationCard(
    signalValues: List<Double>,
    maxSignalPoints: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            if (signalValues.isNotEmpty()) {
                val path = Path()
                val xStep = size.width / maxSignalPoints
                val yScale = size.height / 2

                path.moveTo(0f, (size.height / 2))
                signalValues.forEachIndexed { index, value ->
                    path.lineTo(
                        index * xStep,
                        (size.height / 2) - (value * yScale).toFloat()
                    )
                }

                drawPath(
                    path = path,
                    color = MaterialTheme.colorScheme.primary,
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

@Composable
private fun CameraPreviewContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraState by viewModel.cameraState.collectAsState()
    val measurementState by viewModel.measurementState.collectAsState()
    val heartRate by viewModel.heartRate.collectAsState()
    val hrv by viewModel.hrv.collectAsState()
    val signalValues by viewModel.signalValues.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                viewModel.startCamera(previewView, lifecycleOwner)
            }
        )

        // Loading indicator
        if (cameraState is CameraState.Starting) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )
        }

        // Error state
        when (val state = cameraState) {
            is CameraState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    if (state.isRecoverable) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                viewModel.retryCamera(
                                    previewView = PreviewView(context),
                                    lifecycleOwner = lifecycleOwner
                                )
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is CameraState.Started -> {
                if (state.hasFlash) {
                    // Measurement controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleMeasurement() },
                            enabled = measurementState !is MeasurementState.Error
                        ) {
                            Text(
                                when (measurementState) {
                                    is MeasurementState.Measuring -> "Stop Measurement"
                                    is MeasurementState.Error -> "Error"
                                    else -> "Start Measurement"
                                }
                            )
                        }

                        if (measurementState is MeasurementState.Error) {
                            Text(
                                text = (measurementState as MeasurementState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            else -> { /* Initial state, no UI needed */ }
        }
    }

    // Measurements display
    if (measurementState is MeasurementState.Measuring) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MeasurementsCard(
                heartRate = heartRate,
                hrv = hrv
            )
            Spacer(modifier = Modifier.height(16.dp))
            SignalVisualizationCard(
                signalValues = signalValues,
                maxSignalPoints = 100
            )
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Permission Required") },
        text = { Text("The camera permission is required to measure your heart rate using the camera flash. Please grant the permission to continue.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Nos3Theme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Preview not available")
            }
        }
    }
}