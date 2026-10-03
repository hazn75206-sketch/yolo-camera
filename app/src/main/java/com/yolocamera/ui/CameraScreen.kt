package com.yolocamera.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yolocamera.camera.YoloAnalyzer
import com.yolocamera.detection.DetectionResult
import com.yolocamera.detection.DetectorDebug
import com.yolocamera.detection.YoloDetector
import com.yolocamera.settings.AppSettings
import com.yolocamera.settings.PerformanceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    var showSettings by remember { mutableStateOf(false) }
    var toolbarVisible by remember { mutableStateOf(true) }
    var detections by remember { mutableStateOf(emptyList<DetectionResult>()) }
    var frameW by remember { mutableIntStateOf(0) }
    var frameH by remember { mutableIntStateOf(0) }
    var inferenceMs by remember { mutableLongStateOf(0L) }
    var dbg by remember { mutableStateOf(DetectorDebug()) }
    var detector by remember { mutableStateOf<YoloDetector?>(null) }
    var modelError by remember { mutableStateOf<String?>(null) }
    var modelLoading by remember { mutableStateOf(settings.detectionEnabled) }
    var lastInteract by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(settings.performanceMode, settings.detectionEnabled) {
        if (!settings.detectionEnabled) {
            withContext(Dispatchers.IO) { detector?.close() }
            detector = null
            modelLoading = false
            modelError = null
            return@LaunchedEffect
        }
        if (detector != null) {
            detector?.updateInputSize(settings.performanceMode.inputSize)
            return@LaunchedEffect
        }
        modelLoading = true
        modelError = null
        withContext(Dispatchers.IO) {
            try {
                detector?.close()
                detector = YoloDetector(context.applicationContext, settings.performanceMode.inputSize, settings.performanceMode.numThreads)
            } catch (e: Exception) {
                modelError = when {
                    e.message?.contains("models/yolov8n", ignoreCase = true) == true ->
                        "Model tidak ditemukan di assets/models/yolov8n.onnx. Lihat MODEL_README."
                    else -> "Model gagal dimuat: ${e.message}"
                }
                try { detector?.close() } catch (_: Exception) { }
                detector = null
            }
        }
        modelLoading = false
    }

    DisposableEffect(detector) {
        onDispose {
            try { detector?.close() } catch (_: Exception) { }
        }
    }

    LaunchedEffect(toolbarVisible, lastInteract) {
        if (!toolbarVisible) return@LaunchedEffect
        delay(4000)
        if (System.currentTimeMillis() - lastInteract >= 4000) toolbarVisible = false
    }

    fun poke() {
        lastInteract = System.currentTimeMillis()
        toolbarVisible = true
    }

    if (!hasPermission) {
        Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Butuh izin kamera", color = Color.White)
                Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("Izinkan kamera")
                }
            }
        }
        return
    }

    if (showSettings) {
        SettingsScreen(settings = settings, onChange = onSettingsChange, onBack = { showSettings = false })
        return
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) {
        onDispose { executor.shutdown() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { poke() }
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
            },
            update = { previewView ->
                val provider = ProcessCameraProvider.getInstance(context).get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val selector = if (settings.useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                analysis.setAnalyzer(
                    executor,
                    YoloAnalyzer(
                        detectorProvider = { detector },
                        settingsProvider = { settings }
                    ) { dets, fw, fh, ms, d ->
                        detections = dets
                        frameW = fw
                        frameH = fh
                        inferenceMs = ms
                        dbg = d
                    }
                )
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycle, selector, preview, analysis)
                } catch (_: Exception) {
                    modelError = "Kamera tidak tersedia di perangkat ini."
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (settings.detectionEnabled) {
            DetectionOverlay(detections, frameW, frameH, settings, Modifier.fillMaxSize())
        }

        ToolbarOverlay(
            detectionEnabled = settings.detectionEnabled,
            performanceMode = settings.performanceMode,
            toolbarVisible = toolbarVisible,
            onToggleDetection = {
                poke()
                onSettingsChange(settings.copy(detectionEnabled = !settings.detectionEnabled))
                if (settings.detectionEnabled) detections = emptyList()
            },
            onOpenSettings = { showSettings = true },
            onSelectPerformance = { mode: PerformanceMode ->
                poke()
                onSettingsChange(settings.copy(performanceMode = mode))
            },
            onFlipCamera = {
                poke()
                onSettingsChange(settings.copy(useFrontCamera = !settings.useFrontCamera))
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (modelLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        val infoLine = buildString {
            if (settings.detectionEnabled && settings.showFps && inferenceMs > 0) {
                append("${(1000L / inferenceMs.coerceAtLeast(1L)).coerceAtMost(99)} FPS")
            }
            if (settings.detectionEnabled && settings.showCount) {
                if (isNotEmpty()) append("  ")
                append("${detections.size} objek")
            }
        }
        if (infoLine.isNotEmpty()) {
            Text(
                infoLine,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        if (settings.detectionEnabled && settings.showDebugInfo) {
            val debugLine = buildString {
                append("out ${dbg.outputShape}")
                append(" max ${String.format("%.2f", dbg.maxScore)}")
                append(" cand ${dbg.candidates}")
                dbg.error?.let { append(" ERR $it") }
            }
            Text(
                debugLine,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        modelError?.let { err ->
            Text(
                err,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp, start = 16.dp, end = 16.dp)
                    .background(Color(0xFFB71C1C).copy(alpha = 0.9f))
                    .padding(12.dp)
            )
        }
    }
}
