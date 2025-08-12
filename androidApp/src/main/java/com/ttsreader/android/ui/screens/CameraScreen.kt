package com.ttsreader.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CameraScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "相机扫描",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "使用相机扫描文本",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Button(
                    onClick = { /* TODO: Open camera */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("打开相机")
                }
                
                OutlinedButton(
                    onClick = { /* TODO: Select from gallery */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("从相册选择")
                }
            }
        }
    }
}