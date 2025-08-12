package com.ttsreader.core.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SettingsManager {
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(settings: AppSettings)
    suspend fun resetToDefaults()
    
    fun observeSettings(): Flow<AppSettings>
    fun observeSetting(key: String): Flow<Any?>
    
    suspend fun getTTSProviderSettings(): TTSProviderSettings
    suspend fun getTextProcessorSettings(): TextProcessorSettings
    suspend fun getAudioSettings(): AudioSettings
    suspend fun getUISettings(): UISettings
    suspend fun getReadingSettings(): ReadingSettings
}

data class AppSettings(
    val ttsProvider: TTSProviderSettings,
    val textProcessor: TextProcessorSettings,
    val audio: AudioSettings,
    val ui: UISettings,
    val reading: ReadingSettings,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class TTSProviderSettings(
    val provider: String = "minimax",
    val apiKey: String = "",
    val voiceId: String = "female-1",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val customProviders: List<CustomProvider> = emptyList()
)

data class TextProcessorSettings(
    val provider: String = "deepseek",
    val apiKey: String = "",
    val model: String = "deepseek-chat",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val customPrompts: Map<String, String> = emptyMap()
)

data class AudioSettings(
    val playbackSpeed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f,
    val autoPlayNext: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val cacheAudio: Boolean = true,
    val cacheSizeMB: Int = 100
)

data class UISettings(
    val theme: Theme = Theme.SYSTEM,
    val language: String = "en",
    val fontSize: Float = 16f,
    val fontFamily: String = "system",
    val showLineNumbers: Boolean = false,
    val showWordCount: Boolean = true,
    val enableDarkMode: Boolean = false,
    val colorScheme: ColorScheme = ColorScheme.DEFAULT
)

data class ReadingSettings(
    val autoScroll: Boolean = true,
    val scrollSpeed: Float = 1.0f,
    val highlightCurrentWord: Boolean = true,
    val highlightColor: String = "#FFEB3B",
    val showProgressBar: Boolean = true,
    val pauseOnCall: Boolean = true,
    val resumeAfterCall: Boolean = true,
    val sleepTimer: SleepTimer = SleepTimer.NONE
)

data class CustomProvider(
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val voiceOptions: List<String>
)

enum class Theme {
    LIGHT, DARK, SYSTEM
}

enum class ColorScheme {
    DEFAULT, BLUE, GREEN, PURPLE, ORANGE
}

enum class SleepTimer {
    NONE, FIFTEEN_MIN, THIRTY_MIN, ONE_HOUR, CUSTOM
}

sealed class SettingsEvent {
    data class SettingsUpdated(val settings: AppSettings) : SettingsEvent()
    data class SettingChanged(val key: String, val value: Any?) : SettingsEvent()
    object SettingsReset : SettingsEvent()
}