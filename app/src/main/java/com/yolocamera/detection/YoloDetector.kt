package com.yolocamera.detection

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import com.yolocamera.utils.ImageUtils
import java.nio.FloatBuffer

data class DetectorDebug(
    val outputShape: String = "-",
    val maxScore: Float = 0f,
    val candidates: Int = 0,
    val error: String? = null
)

class YoloDetector(context: Context, inputSize: Int, numThreads: Int) : AutoCloseable {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    var inputSize: Int = inputSize
        private set

    @Volatile
    var lastDebug = DetectorDebug()
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
                return try {
                    parseAdaptive(output, boxed, frame.width, frame.height, confThreshold, iouThreshold, enabledClasses)
                } catch (e: Exception) {
                    lastDebug = DetectorDebug(error = (e.message ?: "parse gagal").take(60))
                    emptyList()
                }
            }
        }
    }

    private fun parseAdaptive(
        output: OrtSession.Result,
        boxed: ImageUtils.Letterbox,
        frameW: Int,
        frameH: Int,
        confThreshold: Float,
        iouThreshold: Float,
        enabledClasses: Set<Int>
    ): List<DetectionResult> {
        val outTensor = output[0] as OnnxTensor
        val dims = outTensor.info.shape
        val tag = dims.joinToString("x")
        return when {
            dims.size == 3 && dims[1] <= 100 -> {
                @Suppress("UNCHECKED_CAST")
                val data = (outTensor.value as Array<Array<FloatArray>>)[0]
                parseRows(data, transpose = false, tag, boxed, frameW, frameH, confThreshold, iouThreshold, enabledClasses)
            }
            dims.size == 3 && dims[2] <= 100 -> {
                @Suppress("UNCHECKED_CAST")
                val data = (outTensor.value as Array<Array<FloatArray>>)[0]
                parseRows(data, transpose = true, tag, boxed, frameW, frameH, confThreshold, iouThreshold, enabledClasses)
            }
            dims.size == 2 && dims[1] == 6L -> {
                @Suppress("UNCHECKED_CAST")
                val rows = outTensor.value as Array<FloatArray>
                parseNmsRows(rows, tag, boxed, frameW, frameH, confThreshold, enabledClasses)
            }
            else -> {
                lastDebug = DetectorDebug(outputShape = tag, error = "bentuk output tak dikenal")
                emptyList()
            }
        }
    }

    private fun parseRows(
        data: Array<FloatArray>,
        transpose: Boolean,
        tag: String,
        boxed: ImageUtils.Letterbox,
        frameW: Int,
        frameH: Int,
        confThreshold: Float,
        iouThreshold: Float,
        enabledClasses: Set<Int>
    ): List<DetectionResult> {
        val channels = if (transpose) data[0].size else data.size
        val anchors = if (transpose) data.size else data[0].size
        fun at(c: Int, i: Int): Float = if (transpose) data[i][c] else data[c][i]
        val numClasses = (channels - 4).coerceAtLeast(0)
        val candidates = ArrayList<DetectionResult>(64)
        var top = 0f
        for (i in 0 until anchors) {
            var bestClass = -1
            var bestScore = 0f
            for (c in 0 until numClasses) {
                if (enabledClasses.isNotEmpty() && c !in enabledClasses) continue
                val s = at(4 + c, i)
                if (s > top) top = s
                if (s > bestScore) {
                    bestScore = s
                    bestClass = c
                }
            }
            if (bestClass < 0 || bestScore < confThreshold) continue
            boxOf(at(0, i), at(1, i), at(2, i), at(3, i), boxed, frameW, frameH)?.let { b ->
                candidates.add(
                    DetectionResult(b[0], b[1], b[2], b[3], bestClass, CocoLabels.nameOf(bestClass), bestScore)
                )
            }
            if (candidates.size >= 500) break
        }
        lastDebug = DetectorDebug(outputShape = tag, maxScore = top, candidates = candidates.size)
        return NmsUtils.nms(candidates, iouThreshold)
    }

    private fun parseNmsRows(
        rows: Array<FloatArray>,
        tag: String,
        boxed: ImageUtils.Letterbox,
        frameW: Int,
        frameH: Int,
        confThreshold: Float,
        enabledClasses: Set<Int>
    ): List<DetectionResult> {
        val out = ArrayList<DetectionResult>(rows.size.coerceAtMost(100))
        var top = 0f
        for (r in rows) {
            if (r.size < 6) continue
            val score = r[4]
            if (score > top) top = score
            if (score < confThreshold) continue
            val classId = r[5].toInt()
            if (enabledClasses.isNotEmpty() && classId !in enabledClasses) continue
            val cx = (r[0] + r[2]) / 2
            val cy = (r[1] + r[3]) / 2
            val w = r[2] - r[0]
            val h = r[3] - r[1]
            boxOf(cx, cy, w, h, boxed, frameW, frameH)?.let { b ->
                out.add(DetectionResult(b[0], b[1], b[2], b[3], classId, CocoLabels.nameOf(classId), score))
            }
            if (out.size >= 100) break
        }
        lastDebug = DetectorDebug(outputShape = tag, maxScore = top, candidates = out.size)
        return out
    }

    private fun boxOf(
        cx: Float, cy: Float, w: Float, h: Float,
        boxed: ImageUtils.Letterbox, frameW: Int, frameH: Int
    ): FloatArray? {
        val x1 = ((cx - w / 2 - boxed.padX) / boxed.scale / frameW).coerceIn(0f, 1f)
        val y1 = ((cy - h / 2 - boxed.padY) / boxed.scale / frameH).coerceIn(0f, 1f)
        val x2 = ((cx + w / 2 - boxed.padX) / boxed.scale / frameW).coerceIn(0f, 1f)
        val y2 = ((cy + h / 2 - boxed.padY) / boxed.scale / frameH).coerceIn(0f, 1f)
        if (x2 <= x1 || y2 <= y1) return null
        return floatArrayOf(x1, y1, x2, y2)
    }

    override fun close() {
        try { session.close() } catch (_: Exception) { }
    }
}
