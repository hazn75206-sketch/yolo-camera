package com.yolocamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.yolocamera.settings.AppSettings
import com.yolocamera.settings.SettingsStore
import com.yolocamera.ui.CameraScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(applicationContext)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val scope = rememberCoroutineScope()
                val settings by store.flow.collectAsState(initial = remember { AppSettings() })
                CameraScreen(
                    settings = settings,
                    onSettingsChange = { next ->
                        scope.launch { store.update({ next }, settings) }
                    }
                )
            }
        }
    }
}
