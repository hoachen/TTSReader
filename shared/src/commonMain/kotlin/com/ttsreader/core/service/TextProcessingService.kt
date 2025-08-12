package com.ttsreader.core.service

import com.ttsreader.core.config.ConfigurationManager
import com.ttsreader.core.text.*
import com.ttsreader.providers.text.DeepSeekTextProcessor
import com.ttsreader.providers.text.LocalTextProcessor
import com.ttsreader.providers.text.OpenAITextProcessor
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextProcessingService(
    private val configManager: ConfigurationManager,
    private val httpClient: HttpClient
) {
    
    private val processors = mutableMapOf<String, TextProcessor>()
    
    suspend fun processText(
        text: String,
        operations: List<TextOperation> = emptyList()
    ): Result<TextProcessingResult> {
        return withContext(Dispatchers.IO) {
            try {
                val providerName = configManager.getCurrentAIProvider()
                val processor = getOrCreateProcessor(providerName)
                
                val request = TextProcessingRequest(
                    text = text,
                    operations = operations.ifEmpty { getDefaultOperations() }
                )
                
                val result = processor.process(request)
                
                // Handle fallback if network fails
                result.recoverCatching { error ->
                    if (shouldUseFallback(error)) {
                        val localProcessor = getOrCreateProcessor("local")
                        localProcessor.process(request).getOrThrow()
                    } else {
                        throw error
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    suspend fun switchProvider(provider: String, config: Map<String, String>): Result<Unit> {
        return try {
            if (provider !in ProviderConfig.SUPPORTED_AI_PROVIDERS) {
                return Result.failure(IllegalArgumentException("Unsupported AI provider: $provider"))
            }
            
            configManager.updateAIProvider(provider, config)
            processors.clear() // Clear cached processors
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getProviderCapabilities(): Set<TextProcessingCapability> {
        return try {
            val providerName = configManager.getCurrentAIProvider()
            val processor = getOrCreateProcessor(providerName)
            processor.getCapabilities()
        } catch (e: Exception) {
            emptySet()
        }
    }
    
    private suspend fun getOrCreateProcessor(providerName: String): TextProcessor {
        return processors.getOrPut(providerName) {
            createProcessor(providerName)
        }
    }
    
    private suspend fun createProcessor(providerName: String): TextProcessor {
        val config = configManager.getProviderConfig(providerName)
        
        return when (providerName) {
            ProviderConfig.DEEPSEEK -> {
                val apiKey = config["apiKey"] ?: throw IllegalArgumentException("DeepSeek API key required")
                DeepSeekTextProcessor(apiKey, httpClient)
            }
            ProviderConfig.OPENAI -> {
                val apiKey = config["apiKey"] ?: throw IllegalArgumentException("OpenAI API key required")
                val model = config["model"] ?: "gpt-4o-mini"
                OpenAITextProcessor(apiKey, model, httpClient)
            }
            ProviderConfig.LOCAL -> LocalTextProcessor()
            else -> throw IllegalArgumentException("Unsupported AI provider: $providerName")
        }
    }
    
    private fun shouldUseFallback(error: Throwable): Boolean {
        return error is java.net.UnknownHostException ||
               error is java.net.ConnectException ||
               error is java.net.SocketTimeoutException
    }
    
    private fun getDefaultOperations(): List<TextOperation> {
        return listOf(
            TextOperation.FilterUrls(true),
            TextOperation.FilterBrackets(listOf(BracketType.ROUND, BracketType.SQUARE)),
            TextOperation.RemoveHeaders(listOf("^第\\d+章", "^\\d+\\.", "^\\*\\*\\*")),
            TextOperation.SemanticSegment(500)
        )
    }
}