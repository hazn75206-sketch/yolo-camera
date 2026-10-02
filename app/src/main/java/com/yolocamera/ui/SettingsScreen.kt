package com.yolocamera.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yolocamera.detection.CocoLabels
import com.yolocamera.settings.AppSettings
import com.yolocamera.settings.PerformanceMode

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Pengaturan", fontSize = 20.sp)
            Button(onClick = onBack) { Text("Kembali") }
        }
        Text("Deteksi", fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.detectionEnabled, onCheckedChange = { onChange(settings.copy(detectionEnabled = it)) })
            Text("Aktifkan deteksi")
        }
        Text("Confidence: ${String.format("%.2f", settings.confThreshold)}")
        Slider(settings.confThreshold, onValueChange = { onChange(settings.copy(confThreshold = it)) }, valueRange = 0.1f..0.9f)
        Text("IoU: ${String.format("%.2f", settings.iouThreshold)}")
        Slider(settings.iouThreshold, onValueChange = { onChange(settings.copy(iouThreshold = it)) }, valueRange = 0.2f..0.8f)
        Text("Mode performa", fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PerformanceMode.entries.forEach { mode ->
                Button(onClick = { onChange(settings.copy(performanceMode = mode)) }) {
                    Text(if (mode == settings.performanceMode) "[${mode.label}]" else mode.label)
                }
            }
        }
        Text("Target FPS: ${settings.targetFps}")
        Slider(settings.targetFps.toFloat(), onValueChange = { onChange(settings.copy(targetFps = it.toInt().coerceIn(1, 30))) }, valueRange = 1f..30f, steps = 28)
        Text("Kamera", fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.useFrontCamera, onCheckedChange = { onChange(settings.copy(useFrontCamera = it)) })
            Text("Kamera depan")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.mirrorFront, onCheckedChange = { onChange(settings.copy(mirrorFront = it)) })
            Text("Mirror kamera depan")
        }
        Text("Overlay", fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.showLabel, onCheckedChange = { onChange(settings.copy(showLabel = it)) })
            Text("Nama class")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.showConfidence, onCheckedChange = { onChange(settings.copy(showConfidence = it)) })
            Text("Confidence")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.showFps, onCheckedChange = { onChange(settings.copy(showFps = it)) })
            Text("FPS")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(settings.showCount, onCheckedChange = { onChange(settings.copy(showCount = it)) })
            Text("Jumlah objek")
        }
        Text("Filter class (8 utama)", fontSize = 16.sp)
        val quickIds = listOf(0, 2, 5, 7, 15, 16, 1, 3)
        quickIds.forEach { id ->
            val checked = id in settings.enabledClasses
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked, onCheckedChange = { on ->
                    val next = settings.enabledClasses.toMutableSet()
                    if (on) next.add(id) else next.remove(id)
                    onChange(settings.copy(enabledClasses = next))
                })
                Text(CocoLabels.nameOf(id))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onChange(settings.copy(enabledClasses = (0 until 80).toSet())) }) { Text("Semua") }
            Button(onClick = { onChange(settings.copy(enabledClasses = setOf(0))) }) { Text("Hanya person") }
        }
    }
    }
}
