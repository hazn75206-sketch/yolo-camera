package com.yolocamera.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("yolo_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val detection = booleanPreferencesKey("detection_enabled")
        val conf = floatPreferencesKey("conf")
        val iou = floatPreferencesKey("iou")
        val classes = stringPreferencesKey("classes_csv")
        val perf = intPreferencesKey("perf")
        val targetFps = intPreferencesKey("target_fps")
        val front = booleanPreferencesKey("front")
        val mirror = booleanPreferencesKey("mirror")
        val showConf = booleanPreferencesKey("show_conf")
        val showFps = booleanPreferencesKey("show_fps")
        val showCount = booleanPreferencesKey("show_count")
        val showLabel = booleanPreferencesKey("show_label")
        val thickness = floatPreferencesKey("thickness")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val csv = p[Keys.classes]
        val classes = if (csv.isNullOrBlank()) (0 until 80).toSet()
        else csv.split(",").mapNotNull { it.toIntOrNull() }.toSet().ifEmpty { (0 until 80).toSet() }
        AppSettings(
            detectionEnabled = p[Keys.detection] ?: false,
            confThreshold = p[Keys.conf] ?: 0.35f,
            iouThreshold = p[Keys.iou] ?: 0.45f,
            enabledClasses = classes,
            performanceMode = PerformanceMode.entries.getOrElse(p[Keys.perf] ?: 1) { PerformanceMode.MEDIUM },
            targetFps = p[Keys.targetFps] ?: 10,
            useFrontCamera = p[Keys.front] ?: false,
            mirrorFront = p[Keys.mirror] ?: true,
            showConfidence = p[Keys.showConf] ?: true,
            showFps = p[Keys.showFps] ?: true,
            showCount = p[Keys.showCount] ?: true,
            showLabel = p[Keys.showLabel] ?: true,
            boxThicknessDp = p[Keys.thickness] ?: 2f
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings, current: AppSettings) {
        val next = transform(current)
        context.dataStore.edit { p ->
            p[Keys.detection] = next.detectionEnabled
            p[Keys.conf] = next.confThreshold
            p[Keys.iou] = next.iouThreshold
            p[Keys.classes] = next.enabledClasses.sorted().joinToString(",")
            p[Keys.perf] = next.performanceMode.ordinal
            p[Keys.targetFps] = next.targetFps
            p[Keys.front] = next.useFrontCamera
            p[Keys.mirror] = next.mirrorFront
            p[Keys.showConf] = next.showConfidence
            p[Keys.showFps] = next.showFps
            p[Keys.showCount] = next.showCount
            p[Keys.showLabel] = next.showLabel
            p[Keys.thickness] = next.boxThicknessDp
        }
    }
}
