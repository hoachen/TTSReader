package com.ttsreader.database

import com.ttsreader.core.settings.*
import com.ttsreader.database.TTSReaderDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseSettingsManager(
    private val database: TTSReaderDatabase
) : SettingsManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val settingsQueries = database.settingsQueries
    private val appSettingsQueries = database.appSettingsQueries
    
    private val _currentSettings = MutableStateFlow(getDefaultSettings())
    val currentSettings: StateFlow<AppSettings> = _currentSettings
    
    override suspend fun getSettings(): AppSettings = withContext(Dispatchers.Default) {
        appSettingsQueries.getAppSettings()
            .executeAsOneOrNull()
            ?.let { json.decodeFromString<AppSettings>(it.settings_json) }
            ?: getDefaultSettings()
    }
    
    override suspend fun updateSettings(settings: AppSettings) = withContext(Dispatchers.Default) {
        val settingsJson = json.encodeToString(settings)
        appSettingsQueries.saveAppSettings(settingsJson, System.currentTimeMillis())
        _currentSettings.value = settings
    }
    
    override suspend fun resetToDefaults() = withContext(Dispatchers.Default) {
        val defaultSettings = getDefaultSettings()
        updateSettings(defaultSettings)
    }
    
    override fun observeSettings(): Flow<AppSettings> {
        return _currentSettings
    }
    
    override fun observeSetting(key: String): Flow<Any?> {
        return _currentSettings.map { settings ->
            when (key) {
                "ttsProvider.provider" -> settings.ttsProvider.provider
                "ttsProvider.apiKey" -> settings.ttsProvider.apiKey
                "ttsProvider.voiceId" -> settings.ttsProvider.voiceId
                "ttsProvider.speed" -> settings.ttsProvider.speed
                "textProcessor.provider" -> settings.textProcessor.provider
                "textProcessor.apiKey" -> settings.textProcessor.apiKey
                "audio.playbackSpeed" -> settings.audio.playbackSpeed
                "audio.volume" -> settings.audio.volume
                "ui.theme" -> settings.ui.theme
                "ui.language" -> settings.ui.language
                "ui.fontSize" -> settings.ui.fontSize
                "reading.autoScroll" -> settings.reading.autoScroll
                "reading.highlightCurrentWord" -> settings.reading.highlightCurrentWord
                else -> null
            }
        }
    }
    
    override suspend fun getTTSProviderSettings(): TTSProviderSettings = withContext(Dispatchers.Default) {
        getSettings().ttsProvider
    }
    
    override suspend fun getTextProcessorSettings(): TextProcessorSettings = withContext(Dispatchers.Default) {
        getSettings().textProcessor
    }
    
    override suspend fun getAudioSettings(): AudioSettings = withContext(Dispatchers.Default) {
        getSettings().audio
    }
    
    override suspend fun getUISettings(): UISettings = withContext(Dispatchers.Default) {
        getSettings().ui
    }
    
    override suspend fun getReadingSettings(): ReadingSettings = withContext(Dispatchers.Default) {
        getSettings().reading
    }
    
    private fun getDefaultSettings(): AppSettings {
        return AppSettings(
            ttsProvider = TTSProviderSettings(),
            textProcessor = TextProcessorSettings(),
            audio = AudioSettings(),
            ui = UISettings(),
            reading = ReadingSettings()
        )
    }
    
    companion object {
        const val SETTINGS_KEY = "app_settings"
    }
}