package com.yolocamera.settings

enum class PerformanceMode(val inputSize: Int, val numThreads: Int, val label: String) {
    LOW(320, 2, "Low"),
    MEDIUM(480, 2, "Medium"),
    HIGH(640, 4, "High")
}

data class AppSettings(
    val detectionEnabled: Boolean = false,
    val confThreshold: Float = 0.35f,
    val iouThreshold: Float = 0.45f,
    val enabledClasses: Set<Int> = (0 until 80).toSet(),
    val performanceMode: PerformanceMode = PerformanceMode.MEDIUM,
    val targetFps: Int = 10,
    val useFrontCamera: Boolean = false,
    val mirrorFront: Boolean = true,
    val showConfidence: Boolean = true,
    val showFps: Boolean = true,
    val showCount: Boolean = true,
    val showLabel: Boolean = true,
    val boxThicknessDp: Float = 2f
)
