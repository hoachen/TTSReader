package com.ttsreader.providers.text

import com.ttsreader.core.text.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class DeepSeekTextProcessor(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com"
) : TextProcessor {
    
    override val processorName: String = "deepseek"
    
    companion object {
        private const val CHAT_ENDPOINT = "/chat/completions"
        private const val DEFAULT_MODEL = "deepseek-chat"
    }
    
    override suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult> {
        return try {
            val prompt = buildProcessingPrompt(request)
            val deepSeekRequest = DeepSeekRequest(
                model = DEFAULT_MODEL,
                messages = listOf(
                    Message("system", getSystemPrompt()),
                    Message("user", prompt + "\n\nText to process:\n${request.text}")
                ),
                max_tokens = 2048,
                temperature = 0.3
            )
            
            val response = httpClient.post("$baseUrl$CHAT_ENDPOINT") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(deepSeekRequest)
            }
            
            if (response.status.isSuccess()) {
                val result = response.body<DeepSeekResponse>()
                val processedText = result.choices.firstOrNull()?.message?.content?.trim() ?: request.text
                
                Result.success(
                    TextProcessingResult(
                        processedText = processedText,
                        changes = calculateChanges(request.text, processedText),
                        metadata = ProcessingMetadata(
                            originalLength = request.text.length,
                            processedLength = processedText.length,
                            processingTimeMs = 0, // Would measure actual time
                            operationCount = request.operations.size
                        )
                    )
                )
            } else {
                Result.failure(Exception("DeepSeek API error: ${response.status} - ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCapabilities(): Set<TextProcessingCapability> = setOf(
        TextProcessingCapability.FILTERING,
        TextProcessingCapability.SEMANTIC_ANALYSIS,
        TextProcessingCapability.CONTENT_OPTIMIZATION
    )
    
    private fun buildProcessingPrompt(request: TextProcessingRequest): String {
        val operations = request.operations.joinToString("\n") { op ->
            when (op) {
                is TextOperation.FilterUrls -> "- Remove URLs and web links"
                is TextOperation.FilterBrackets -> "- Remove text in ${op.types.joinToString(", ") { it.name.lowercase() }} brackets"
                is TextOperation.RemoveHeaders -> "- Remove headers matching patterns: ${op.patterns.joinToString(", ")}"
                is TextOperation.SemanticSegment -> "- Break long sentences into segments of max ${op.maxLength} characters"
                is TextOperation.OptimizePronunciation -> "- Apply custom pronunciations: ${op.customDict.entries.joinToString { "${it.key}=${it.value}" }}"
            }
        }
        
        return """
            You are a text processing assistant for a TTS (Text-to-Speech) application.
            Process the following text according to these rules:
            
            $operations
            
            Guidelines:
            1. Preserve the meaning and readability
            2. Maintain natural speech flow
            3. Remove only clearly identifiable unwanted content
            4. Keep the text suitable for audio narration
            5. Return only the processed text, no explanations
            
            Processed text:
        """.trimIndent()
    }
    
    private fun getSystemPrompt(): String = """
        You are a text processing expert for Chinese and English TTS applications.
        Your role is to clean and optimize text for natural speech synthesis.
        Focus on removing distractions while preserving meaning.
    """.trimIndent()
    
    private fun calculateChanges(original: String, processed: String): List<TextChange> {
        val changes = mutableListOf<TextChange>()
        
        // Simple change detection - would need more sophisticated diffing for production
        if (original != processed) {
            changes.add(
                TextChange(
                    type = ChangeType.REPLACEMENT,
                    original = original,
                    replacement = processed,
                    position = 0..original.length
                )
            )
        }
        
        return changes
    }
    
    @Serializable
    private data class DeepSeekRequest(
        val model: String,
        val messages: List<Message>,
        val max_tokens: Int,
        val temperature: Double
    )
    
    @Serializable
    private data class Message(
        val role: String,
        val content: String
    )
    
    @Serializable
    private data class DeepSeekResponse(
        val choices: List<Choice>
    )
    
    @Serializable
    private data class Choice(
        val message: Message
    )
}