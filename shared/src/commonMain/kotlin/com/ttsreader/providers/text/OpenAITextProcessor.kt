package com.ttsreader.providers.text

import com.ttsreader.core.text.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class OpenAITextProcessor(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini",
    private val baseUrl: String = "https://api.openai.com"
) : TextProcessor {
    
    override val processorName: String = "openai"
    
    companion object {
        private const val CHAT_ENDPOINT = "/v1/chat/completions"
    }
    
    override suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult> {
        return try {
            val messages = buildOpenAIMessages(request)
            val openAIRequest = OpenAIRequest(
                model = model,
                messages = messages,
                max_tokens = 2048,
                temperature = 0.3
            )
            
            val response = httpClient.post("$baseUrl$CHAT_ENDPOINT") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(openAIRequest)
            }
            
            if (response.status.isSuccess()) {
                val result = response.body<OpenAIResponse>()
                val processedText = result.choices.firstOrNull()?.message?.content?.trim() ?: request.text
                
                Result.success(
                    TextProcessingResult(
                        processedText = processedText,
                        changes = calculateChanges(request.text, processedText),
                        metadata = ProcessingMetadata(
                            originalLength = request.text.length,
                            processedLength = processedText.length,
                            processingTimeMs = 0,
                            operationCount = request.operations.size
                        )
                    )
                )
            } else {
                Result.failure(Exception("OpenAI API error: ${response.status} - ${response.bodyAsText()}"))
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
    
    private fun buildOpenAIMessages(request: TextProcessingRequest): List<OpenAIMessage> {
        val systemPrompt = """
            You are a text processing expert for TTS (Text-to-Speech) applications.
            Your task is to clean and optimize text for natural speech synthesis.
            
            Processing rules:
            ${request.operations.joinToString("\n") { op ->
                when (op) {
                    is TextOperation.FilterUrls -> "- Remove URLs, web links, and email addresses"
                    is TextOperation.FilterBrackets -> "- Remove content within ${op.types.joinToString(", ") { it.name.lowercase() }} brackets"
                    is TextOperation.RemoveHeaders -> "- Remove headers matching: ${op.patterns.joinToString(", ")}"
                    is TextOperation.SemanticSegment -> "- Break long text into ${op.maxLength}-character segments"
                    is TextOperation.OptimizePronunciation -> "- Apply pronunciations: ${op.customDict.entries.joinToString { "${it.key}=${it.value}" }}"
                }
            }}
            
            Guidelines:
            1. Preserve meaning and natural flow
            2. Remove only clearly unwanted content
            3. Ensure text is suitable for audio narration
            4. Return only processed text, no explanations
        """.trimIndent()
        
        return listOf(
            OpenAIMessage("system", systemPrompt),
            OpenAIMessage("user", request.text)
        )
    }
    
    private fun calculateChanges(original: String, processed: String): List<TextChange> {
        val changes = mutableListOf<TextChange>()
        
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
    private data class OpenAIRequest(
        val model: String,
        val messages: List<OpenAIMessage>,
        val max_tokens: Int,
        val temperature: Double
    )
    
    @Serializable
    private data class OpenAIMessage(
        val role: String,
        val content: String
    )
    
    @Serializable
    private data class OpenAIResponse(
        val choices: List<OpenAIChoice>
    )
    
    @Serializable
    private data class OpenAIChoice(
        val message: OpenAIMessage
    )
}