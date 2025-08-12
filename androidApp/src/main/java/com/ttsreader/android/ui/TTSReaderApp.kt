package com.ttsreader.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ttsreader.android.ui.screens.*

@Composable
fun TTSReaderApp() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(
                    "home" to "主页",
                    "library" to "书库",
                    "camera" to "扫描",
                    "settings" to "设置"
                )
                
                items.forEach { (route, label) ->
                    NavigationBarItem(
                        selected = navController.currentBackStackEntry?.destination?.route == route,
                        onClick = { navController.navigate(route) },
                        label = { Text(label) },
                        icon = { /* Icon placeholder */ }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") { HomeScreen() }
            composable("library") { LibraryScreen() }
            composable("camera") { CameraScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}