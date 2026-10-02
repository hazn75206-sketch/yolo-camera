package com.yolocamera.detection

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import com.yolocamera.utils.ImageUtils
import java.nio.FloatBuffer

class YoloDetector(context: Context, inputSize: Int, numThreads: Int) : AutoCloseable {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    var inputSize: Int = inputSize
        private set

    init {
        val modelBytes = context.assets.open("models/yolov8n.onnx").use { it.readBytes() }
        val opts = OrtSession.SessionOptions()
        opts.setIntraOpNumThreads(numThreads.coerceIn(1, 4))
        session = env.createSession(modelBytes, opts)
        inputName = session.inputNames.iterator().next()
    }

    fun updateInputSize(size: Int) {
        inputSize = size
    }

    fun detect(
        frame: Bitmap,
        confThreshold: Float,
        iouThreshold: Float,
        enabledClasses: Set<Int>
    ): List<DetectionResult> {
        val boxed = ImageUtils.letterbox(frame, inputSize)
        val chw = ImageUtils.bitmapToChwFloat(boxed.bitmap)
        boxed.bitmap.recycle()
        val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(chw), shape)
        tensor.use {
            val results = session.run(mapOf(inputName to it))
            results.use { output ->
                @Suppress("UNCHECKED_CAST")
                val data = (output[0].value as Array<Array<FloatArray>>)[0]
                return parseYolov8(data, boxed, frame.width, frame.height, confThreshold, iouThreshold, enabledClasses)
            }
        }
    }

    private fun parseYolov8(
        data: Array<FloatArray>,
        boxed: ImageUtils.Letterbox,
        frameW: Int,
        frameH: Int,
        confThreshold: Float,
        iouThreshold: Float,
        enabledClasses: Set<Int>
    ): List<DetectionResult> {
        val numAnchors = data[0].size
        val numRows = data.size
        val numClasses = (numRows - 4).coerceAtLeast(0)
        val candidates = ArrayList<DetectionResult>(64)
        for (i in 0 until numAnchors) {
            var bestClass = -1
            var bestScore = 0f
            for (c in 0 until numClasses) {
                val classId = c
                if (enabledClasses.isNotEmpty() && classId !in enabledClasses) continue
                val s = data[4 + c][i]
                if (s > bestScore) {
                    bestScore = s
                    bestClass = classId
                }
            }
            if (bestClass < 0 || bestScore < confThreshold) continue
            val cx = data[0][i]
            val cy = data[1][i]
            val w = data[2][i]
            val h = data[3][i]
            val x1 = ((cx - w / 2 - boxed.padX) / boxed.scale / frameW).coerceIn(0f, 1f)
            val y1 = ((cy - h / 2 - boxed.padY) / boxed.scale / frameH).coerceIn(0f, 1f)
            val x2 = ((cx + w / 2 - boxed.padX) / boxed.scale / frameW).coerceIn(0f, 1f)
            val y2 = ((cy + h / 2 - boxed.padY) / boxed.scale / frameH).coerceIn(0f, 1f)
            if (x2 <= x1 || y2 <= y1) continue
            candidates.add(
                DetectionResult(x1, y1, x2, y2, bestClass, CocoLabels.nameOf(bestClass), bestScore)
            )
            if (candidates.size >= 500) break
        }
        return NmsUtils.nms(candidates, iouThreshold)
    }

    override fun close() {
        try { session.close() } catch (_: Exception) { }
    }
}
