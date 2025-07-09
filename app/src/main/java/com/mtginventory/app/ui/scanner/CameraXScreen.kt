package com.mtginventory.app.ui.scanner

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.mtginventory.app.data.processing.CardProcessingPipeline
import com.mtginventory.app.data.processing.ProcessingResult
import com.mtginventory.app.model.ScannedCard
import com.mtginventory.app.ui.theme.BlueAccent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraXScreen(
    cardProcessingPipeline: CardProcessingPipeline,
    onCardScanned: (List<ScannedCard>) -> Unit,
    onDismiss: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    when {
        cameraPermissionState.status.isGranted -> {
            CameraXScreenContent(
                cardProcessingPipeline = cardProcessingPipeline,
                onCardScanned = onCardScanned,
                onDismiss = onDismiss
            )
        }
        cameraPermissionState.status.shouldShowRationale -> {
            // Show rationale dialog
            PermissionRationaleDialog(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                onDismiss = onDismiss
            )
        }
        else -> {
            // Request permission
            LaunchedEffect(Unit) {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}

@Composable
private fun CameraXScreenContent(
    cardProcessingPipeline: CardProcessingPipeline,
    onCardScanned: (List<ScannedCard>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var gridMode by remember { mutableStateOf(GridMode.GRID_3X3) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableStateOf(0f) }
    var detectedCards by remember { mutableStateOf<List<CardDetection>>(emptyList()) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera Preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            ) { view ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    bindCamera(
                        cameraProvider = cameraProvider,
                        previewView = view,
                        lifecycleOwner = lifecycleOwner,
                        flashEnabled = flashEnabled
                    )
                }, ContextCompat.getMainExecutor(context))
            }

            // Grid Overlay
            GridOverlay(
                gridMode = gridMode,
                detectedCards = detectedCards,
                modifier = Modifier.fillMaxSize()
            )

            // Top Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Close Button
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }

                // Grid Mode Toggle
                IconButton(
                    onClick = {
                        gridMode = if (gridMode == GridMode.GRID_3X3) {
                            GridMode.GRID_4X3
                        } else {
                            GridMode.GRID_3X3
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.GridView, contentDescription = "Toggle Grid")
                }

                // Flash Toggle
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash"
                    )
                }
            }

            // Processing Overlay
            if (isProcessing) {
                ProcessingOverlay(
                    progress = processingProgress,
                    gridMode = gridMode,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Instructions and Controls
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Position binder page within the grid",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "${gridMode.description} • Make sure all cards are visible",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            if (!isProcessing) {
                                isProcessing = true
                                processingProgress = 0f
                                // Trigger image capture and processing
                                captureAndProcess(
                                    previewView = previewView, 
                                    gridMode = gridMode,
                                    cardProcessingPipeline = cardProcessingPipeline,
                                    onProgress = { progress -> processingProgress = progress },
                                    onComplete = { scannedCards ->
                                        isProcessing = false
                                        onCardScanned(scannedCards)
                                        onDismiss()
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Scan ${gridMode.cardCount} Cards")
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

@Composable
private fun GridOverlay(
    gridMode: GridMode,
    detectedCards: List<CardDetection>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawGrid(gridMode, detectedCards)
    }
}

@Composable
private fun ProcessingOverlay(
    progress: Float,
    gridMode: GridMode,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(48.dp),
                    color = BlueAccent,
                    strokeWidth = 4.dp
                )

                Text(
                    text = "Processing ${gridMode.cardCount} cards...",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Text(
                    text = "${(progress * 100).toInt()}% complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Permission Required") },
        text = { Text("This app needs camera access to scan Magic: The Gathering cards. Please grant camera permission to continue.") },
        confirmButton = {
            TextButton(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Data classes and enums
enum class GridMode(val rows: Int, val cols: Int, val cardCount: Int, val description: String) {
    GRID_3X3(3, 3, 9, "3×3 Grid (9 cards)"),
    GRID_4X3(4, 3, 12, "4×3 Grid (12 cards)")
}

data class CardDetection(
    val position: Int,
    val bounds: Rect,
    val confidence: Float,
    val isDetected: Boolean = false
)

// Helper functions
private fun DrawScope.drawGrid(gridMode: GridMode, detectedCards: List<CardDetection>) {
    val padding = 40.dp.toPx()
    val gridWidth = size.width - (padding * 2)
    val gridHeight = size.height - (padding * 2)
    
    val cellWidth = gridWidth / gridMode.cols
    val cellHeight = gridHeight / gridMode.rows
    
    val paint = Paint().apply {
        color = BlueAccent
        style = PaintingStyle.Stroke
        strokeWidth = 3.dp.toPx()
    }

    // Draw grid lines
    for (i in 0..gridMode.rows) {
        val y = padding + (i * cellHeight)
        drawLine(
            color = BlueAccent,
            start = Offset(padding, y),
            end = Offset(size.width - padding, y),
            strokeWidth = 2.dp.toPx()
        )
    }

    for (i in 0..gridMode.cols) {
        val x = padding + (i * cellWidth)
        drawLine(
            color = BlueAccent,
            start = Offset(x, padding),
            end = Offset(x, size.height - padding),
            strokeWidth = 2.dp.toPx()
        )
    }

    // Draw card detection indicators
    detectedCards.forEach { detection ->
        val row = detection.position / gridMode.cols
        val col = detection.position % gridMode.cols
        
        val x = padding + (col * cellWidth) + (cellWidth * 0.1f)
        val y = padding + (row * cellHeight) + (cellHeight * 0.1f)
        val width = cellWidth * 0.8f
        val height = cellHeight * 0.8f

        val color = if (detection.isDetected) Color.Green else Color.Red
        
        drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(width, height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
    }
}

private fun bindCamera(
    cameraProvider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    flashEnabled: Boolean
) {
    val preview = Preview.Builder().build()
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    try {
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)

        // Set flash mode
        camera.cameraControl.enableTorch(flashEnabled)

    } catch (exc: Exception) {
        // Handle camera binding exception
    }
}

private fun captureAndProcess(
    previewView: PreviewView,
    gridMode: GridMode,
    cardProcessingPipeline: CardProcessingPipeline,
    onProgress: (Float) -> Unit,
    onComplete: (List<ScannedCard>) -> Unit
) {
    val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
    
    coroutineScope.launch {
        try {
            // Capture image from camera
            val bitmap = captureImageFromPreview(previewView)
            
            if (bitmap != null) {
                // Process the captured image using the real pipeline
                cardProcessingPipeline.processGridImage(
                    bitmap = bitmap,
                    gridMode = gridMode,
                    onProgress = onProgress
                ).collect { result ->
                    when (result) {
                        is ProcessingResult.Completed -> {
                            onComplete(result.scannedCards)
                        }
                        is ProcessingResult.Error -> {
                            // Handle error case by providing empty list or showing error
                            // TODO: Show error message to user
                            onComplete(emptyList())
                        }
                        else -> {
                            // Continue processing, update UI if needed
                        }
                    }
                }
            } else {
                // Handle bitmap capture failure
                onComplete(emptyList())
            }
        } catch (e: Exception) {
            onComplete(emptyList())
        }
    }
}

private suspend fun captureImageFromPreview(previewView: PreviewView): Bitmap? {
    return suspendCoroutine { continuation ->
        try {
            // Validate preview view dimensions
            if (previewView.width <= 0 || previewView.height <= 0) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            // Convert preview view to bitmap
            val bitmap = Bitmap.createBitmap(
                previewView.width,
                previewView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            previewView.draw(canvas)
            
            // Validate bitmap was created successfully
            if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
                continuation.resume(null)
                return@suspendCoroutine
            }
            
            continuation.resume(bitmap)
        } catch (e: OutOfMemoryError) {
            // Handle OOM gracefully
            continuation.resume(null)
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}

