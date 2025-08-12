package com.ttsreader.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "TTS Reader",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "欢迎使用文本转语音阅读器",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "支持文本朗读、相机扫描、AI处理等功能",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Button(
            onClick = { /* TODO: Start reading */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始朗读")
        }
        
        Button(
            onClick = { /* TODO: Open file picker */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("选择文本文件")
        }
    }
}