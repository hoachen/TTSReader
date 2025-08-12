package com.ttsreader.core.config

import com.ttsreader.core.tts.TTSProvider
import com.ttsreader.core.text.TextProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ConfigurationManager {
    suspend fun getCurrentTTSProvider(): String
    suspend fun getCurrentAIProvider(): String
    suspend fun updateTTSProvider(provider: String, config: Map<String, String>): Result<Unit>
    suspend fun updateAIProvider(provider: String, config: Map<String, String>): Result<Unit>
    suspend fun getProviderConfig(provider: String): Map<String, String>
    suspend fun getAvailableProviders(): List<String>
    suspend fun validateConfiguration(provider: String, config: Map<String, String>): Boolean
    
    val currentTTSProvider: StateFlow<String>
    val currentAIProvider: StateFlow<String>
    val isConfigured: StateFlow<Boolean>
}

class ConfigurationManagerImpl(
    private val storage: ConfigurationStorage
) : ConfigurationManager {
    
    private val _currentTTSProvider = MutableStateFlow("minimax")
    private val _currentAIProvider = MutableStateFlow("deepseek")
    private val _isConfigured = MutableStateFlow(false)
    
    override val currentTTSProvider: StateFlow<String> = _currentTTSProvider.asStateFlow()
    override val currentAIProvider: StateFlow<String> = _currentAIProvider.asStateFlow()
    override val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()
    
    init {
        loadInitialConfig()
    }
    
    private fun loadInitialConfig() {
        // Load from storage on initialization
        // This would typically be async in a real implementation
    }
    
    override suspend fun getCurrentTTSProvider(): String = _currentTTSProvider.value
    
    override suspend fun getCurrentAIProvider(): String = _currentAIProvider.value
    
    override suspend fun updateTTSProvider(provider: String, config: Map<String, String>): Result<Unit> {
        return try {
            if (!validateConfiguration(provider, config)) {
                return Result.failure(IllegalArgumentException("Invalid configuration for provider: $provider"))
            }
            
            storage.saveTTSConfig(provider, config)
            _currentTTSProvider.value = provider
            _isConfigured.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateAIProvider(provider: String, config: Map<String, String>): Result<Unit> {
        return try {
            if (!validateConfiguration(provider, config)) {
                return Result.failure(IllegalArgumentException("Invalid configuration for provider: $provider"))
            }
            
            storage.saveAIConfig(provider, config)
            _currentAIProvider.value = provider
            _isConfigured.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProviderConfig(provider: String): Map<String, String> {
        return when (provider) {
            "minimax" -> storage.getTTSConfig()
            "deepseek", "openai" -> storage.getAIConfig()
            else -> emptyMap()
        }
    }
    
    override suspend fun getAvailableProviders(): List<String> {
        return listOf("minimax", "deepseek", "openai", "local")
    }
    
    override suspend fun validateConfiguration(provider: String, config: Map<String, String>): Boolean {
        return when (provider) {
            "minimax" -> {
                config["apiKey"]?.isNotBlank() == true && 
                config["groupId"]?.isNotBlank() == true
            }
            "deepseek", "openai" -> {
                config["apiKey"]?.isNotBlank() == true
            }
            "local" -> true // Local provider doesn't need configuration
            else -> false
        }
    }
}

interface ConfigurationStorage {
    suspend fun saveTTSConfig(provider: String, config: Map<String, String>)
    suspend fun saveAIConfig(provider: String, config: Map<String, String>)
    suspend fun getTTSConfig(): Map<String, String>
    suspend fun getAIConfig(): Map<String, String>
    suspend fun clear()
}

@kotlinx.serialization.Serializable
data class TTSConfig(
    val provider: String,
    val apiKey: String,
    val groupId: String? = null,
    val fallbackProviders: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class AIConfig(
    val provider: String,
    val apiKey: String,
    val model: String? = null,
    val fallbackRules: Boolean = true
)

object ProviderConfig {
    const val MINIMAX = "minimax"
    const val DEEPSEEK = "deepseek"
    const val OPENAI = "openai"
    const val LOCAL = "local"
    
    val SUPPORTED_TTS_PROVIDERS = listOf(MINIMAX)
    val SUPPORTED_AI_PROVIDERS = listOf(DEEPSEEK, OPENAI, LOCAL)
}