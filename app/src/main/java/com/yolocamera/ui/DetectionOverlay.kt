package com.yolocamera.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.yolocamera.detection.DetectionResult
import com.yolocamera.settings.AppSettings

@Composable
fun DetectionOverlay(
    detections: List<DetectionResult>,
    frameW: Int,
    frameH: Int,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val strokePx = with(density) { settings.boxThicknessDp.dp.toPx() }
    Canvas(modifier = modifier.fillMaxSize()) {
        if (frameW <= 0 || frameH <= 0) return@Canvas
        val canvasW = size.width
        val canvasH = size.height
        val imgAspect = frameW / frameH.toFloat()
        val canvasAspect = canvasW / canvasH
        var visW = 1f
        var visH = 1f
        var offX = 0f
        var offY = 0f
        if (imgAspect > canvasAspect) {
            visW = canvasAspect / imgAspect
            offX = (1f - visW) / 2f
        } else {
            visH = imgAspect / canvasAspect
            offY = (1f - visH) / 2f
        }
        val mirror = settings.useFrontCamera && settings.mirrorFront
        detections.forEach { d ->
            var nx1 = (d.left - offX) / visW
            var nx2 = (d.right - offX) / visW
            val ny1 = (d.top - offY) / visH
            val ny2 = (d.bottom - offY) / visH
            if (mirror) {
                val m1 = 1f - nx2
                val m2 = 1f - nx1
                nx1 = m1
                nx2 = m2
            }
            if (nx2 < 0f || nx1 > 1f || ny2 < 0f || ny1 > 1f) return@forEach
            val x = nx1.coerceIn(0f, 1f) * canvasW
            val y = ny1.coerceIn(0f, 1f) * canvasH
            val w = (nx2.coerceIn(0f, 1f) - nx1.coerceIn(0f, 1f)) * canvasW
            val h = (ny2.coerceIn(0f, 1f) - ny1.coerceIn(0f, 1f)) * canvasH
            if (w <= 2f || h <= 2f) return@forEach
            drawRect(Color(0xFF4CAF50), Offset(x, y), Size(w, h), style = Stroke(strokePx))
            if (settings.showLabel || settings.showConfidence) {
                val label = buildString {
                    if (settings.showLabel) append(d.className)
                    if (settings.showLabel && settings.showConfidence) append(" ")
                    if (settings.showConfidence) append(String.format("%.2f", d.confidence))
                }
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(200, 0, 0, 0)
                        textSize = 13f * density.density
                    }
                    val bgPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(180, 0, 0, 0)
                    }
                    val tw = paint.measureText(label)
                    drawRect(x, (y - 22f * density.density).coerceAtLeast(0f), x + tw + 12f, y, bgPaint)
                    drawText(label, x + 6f, y - 6f * density.density, paint)
                }
            }
        }
    }
}
