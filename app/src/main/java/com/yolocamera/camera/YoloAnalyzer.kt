package com.yolocamera.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.yolocamera.detection.DetectionResult
import com.yolocamera.detection.YoloDetector
import com.yolocamera.settings.AppSettings
import com.yolocamera.utils.ImageUtils
import java.util.concurrent.atomic.AtomicBoolean

class YoloAnalyzer(
    private val detectorProvider: () -> YoloDetector?,
    private val settingsProvider: () -> AppSettings,
    private val onResult: (List<DetectionResult>, frameW: Int, frameH: Int, inferenceMs: Long) -> Unit
) : ImageAnalysis.Analyzer {
    private val inferring = AtomicBoolean(false)
    private var lastRun = 0L

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val settings = settingsProvider()
        if (!settings.detectionEnabled) {
            image.close()
            return
        }
        val minInterval = 1000L / settings.targetFps.coerceIn(1, 30)
        val now = System.currentTimeMillis()
        if (now - lastRun < minInterval || !inferring.compareAndSet(false, true)) {
            image.close()
            return
        }
        try {
            lastRun = now
            val detector = detectorProvider()
            if (detector == null) {
                image.close()
                return
            }
            val bitmap = ImageUtils.imageProxyToBitmapUpright(image)
            val fw = image.width
            val fh = image.height
            image.close()
            if (bitmap == null) return
            val t0 = System.nanoTime()
            val dets = try {
                detector.detect(bitmap, settings.confThreshold, settings.iouThreshold, settings.enabledClasses)
            } catch (_: Exception) {
                emptyList()
            } finally {
                bitmap.recycle()
            }
            val ms = (System.nanoTime() - t0) / 1_000_000
            onResult(dets, fw, fh, ms)
        } finally {
            inferring.set(false)
        }
    }
}
