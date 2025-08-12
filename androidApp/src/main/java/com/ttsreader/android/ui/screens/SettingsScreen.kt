package com.ttsreader.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingItem(
                    title = "TTS 提供商",
                    subtitle = "MiniMax",
                    icon = Icons.Default.VolumeUp,
                    onClick = { /* TODO */ }
                )
                
                Divider()
                
                SettingItem(
                    title = "AI 文本处理",
                    subtitle = "DeepSeek",
                    icon = Icons.Default.TextFields,
                    onClick = { /* TODO */ }
                )
                
                Divider()
                
                SettingItem(
                    title = "语音设置",
                    subtitle = "语速、音量、音调",
                    icon = Icons.Default.SettingsVoice,
                    onClick = { /* TODO */ }
                )
                
                Divider()
                
                SettingItem(
                    title = "主题",
                    subtitle = "浅色/深色模式",
                    icon = Icons.Default.Brightness6,
                    onClick = { /* TODO */ }
                )
                
                Divider()
                
                SettingItem(
                    title = "缓存管理",
                    subtitle = "清理缓存文件",
                    icon = Icons.Default.Storage,
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "更多"
            )
        }
    }
}