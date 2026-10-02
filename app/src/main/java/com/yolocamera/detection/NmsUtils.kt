package com.yolocamera.detection

import kotlin.math.max

object NmsUtils {
    fun iou(a: DetectionResult, b: DetectionResult): Float {
        val interLeft = max(a.left, b.left)
        val interTop = max(a.top, b.top)
        val interRight = a.right.coerceAtMost(b.right)
        val interBottom = a.bottom.coerceAtMost(b.bottom)
        val interW = (interRight - interLeft).coerceAtLeast(0f)
        val interH = (interBottom - interTop).coerceAtLeast(0f)
        val inter = interW * interH
        if (inter <= 0f) return 0f
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        return inter / (areaA + areaB - inter)
    }

    fun nms(candidates: List<DetectionResult>, iouThreshold: Float, maxDetections: Int = 100): List<DetectionResult> {
        if (candidates.isEmpty()) return emptyList()
        val sorted = candidates.sortedByDescending { it.confidence }.toMutableList()
        val kept = ArrayList<DetectionResult>(maxDetections.coerceAtMost(sorted.size))
        while (sorted.isNotEmpty() && kept.size < maxDetections) {
            val best = sorted.removeAt(0)
            kept.add(best)
            val it = sorted.iterator()
            while (it.hasNext()) {
                val other = it.next()
                if (other.classId == best.classId && iou(best, other) > iouThreshold) {
                    it.remove()
                }
            }
        }
        return kept
    }
}
