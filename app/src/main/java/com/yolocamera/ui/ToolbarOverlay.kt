package com.yolocamera.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yolocamera.settings.PerformanceMode

@Composable
fun ToolbarOverlay(
    detectionEnabled: Boolean,
    performanceMode: PerformanceMode,
    toolbarVisible: Boolean,
    onToggleDetection: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectPerformance: (PerformanceMode) -> Unit,
    onFlipCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!toolbarVisible) return
    var perfMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("YOLO Camera", color = Color.White, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleDetection, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (detectionEnabled) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (detectionEnabled) "Matikan deteksi" else "Nyalakan deteksi",
                    tint = if (detectionEnabled) Color(0xFF81C784) else Color.White
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = "Pengaturan", tint = Color.White)
            }
            IconButton(onClick = { perfMenu = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Speed, contentDescription = "Mode performa", tint = Color.White)
            }
            DropdownMenu(expanded = perfMenu, onDismissRequest = { perfMenu = false }) {
                PerformanceMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label + if (mode == performanceMode) " (aktif)" else "") },
                        onClick = { onSelectPerformance(mode); perfMenu = false }
                    )
                }
            }
            IconButton(onClick = { moreMenu = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Lainnya", tint = Color.White)
            }
            DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                DropdownMenuItem(text = { Text("Balik kamera") }, onClick = { onFlipCamera(); moreMenu = false })
                DropdownMenuItem(text = { Text("Pengaturan") }, onClick = { onOpenSettings(); moreMenu = false })
            }
        }
    }
}
