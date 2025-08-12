package com.ttsreader.core.service

import com.ttsreader.core.cache.TTSCache
import com.ttsreader.core.config.ConfigurationManager
import com.ttsreader.core.tts.*
import com.ttsreader.providers.tts.MiniMaxTTSProvider
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class TTSProviderManager(
    private val configManager: ConfigurationManager,
    private val cache: TTSCache,
    private val httpClient: HttpClient
) {
    
    private val providers = mutableMapOf<String, TTSProvider>()
    
    suspend fun synthesize(text: String, voice: TTSVoice, parameters: TTSParameters = TTSParameters()): Result<TTSAudioData> {
        return withContext(Dispatchers.IO) {
            try {
                val cacheKey = generateCacheKey(text, voice, parameters)
                
                // Check cache first
                cache.get(cacheKey)?.let { cachedAudio ->
                    return@withContext Result.success(cachedAudio)
                }
                
                // Get current provider
                val providerName = configManager.getCurrentTTSProvider()
                val provider = getOrCreateProvider(providerName)
                
                if (!provider.isConfigured()) {
                    return@withContext Result.failure(
                        IllegalStateException("Provider $providerName is not configured")
                    )
                }
                
                // Synthesize audio
                val request = TTSRequest(
                    text = text,
                    voice = voice,
                    parameters = parameters
                )
                
                val result = provider.synthesize(request)
                
                // Cache successful results
                result.onSuccess { audio ->
                    cache.put(cacheKey, audio)
                }
                
                result
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun getAvailableVoices(): Result<List<TTSVoice>> {
        return try {
            val providerName = configManager.getCurrentTTSProvider()
            val provider = getOrCreateProvider(providerName)
            
            if (!provider.isConfigured()) {
                return Result.failure(
                    IllegalStateException("Provider $providerName is not configured")
                )
            }
            
            provider.getAvailableVoices()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun switchProvider(provider: String, config: Map<String, String>): Result<Unit> {
        return try {
            if (provider !in ProviderConfig.SUPPORTED_TTS_PROVIDERS) {
                return Result.failure(IllegalArgumentException("Unsupported TTS provider: $provider"))
            }
            
            configManager.updateTTSProvider(provider, config)
            providers.clear() // Clear cached providers
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun getOrCreateProvider(providerName: String): TTSProvider {
        return providers.getOrPut(providerName) {
            createProvider(providerName)
        }
    }
    
    private suspend fun createProvider(providerName: String): TTSProvider {
        val config = configManager.getProviderConfig(providerName)
        
        return when (providerName) {
            ProviderConfig.MINIMAX -> {
                val apiKey = config["apiKey"] ?: throw IllegalArgumentException("MiniMax API key required")
                val groupId = config["groupId"] ?: throw IllegalArgumentException("MiniMax group ID required")
                MiniMaxTTSProvider(apiKey, groupId, httpClient)
            }
            else -> throw IllegalArgumentException("Unsupported TTS provider: $providerName")
        }
    }
    
    private fun generateCacheKey(text: String, voice: TTSVoice, parameters: TTSParameters): String {
        val combined = buildString {
            append(text)
            append("|")
            append(voice.id)
            append("|")
            append(parameters.speed)
            append("|")
            append(parameters.volume)
            append("|")
            append(parameters.pitch)
            append("|")
            append(parameters.emotion)
        }
        
        return MessageDigest.getInstance("MD5")
            .digest(combined.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}