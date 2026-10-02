package com.yolocamera.detection

data class DetectionResult(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val classId: Int,
    val className: String,
    val confidence: Float
)
