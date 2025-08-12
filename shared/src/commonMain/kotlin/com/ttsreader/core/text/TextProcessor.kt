package com.ttsreader.core.text

interface TextProcessor {
    val processorName: String
    suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult>
    suspend fun getCapabilities(): Set<TextProcessingCapability>
}

@kotlinx.serialization.Serializable
data class TextProcessingRequest(
    val text: String,
    val operations: List<TextOperation>,
    val context: ProcessingContext = ProcessingContext()
)

@kotlinx.serialization.Serializable
data class TextProcessingResult(
    val processedText: String,
    val changes: List<TextChange> = emptyList(),
    val metadata: ProcessingMetadata = ProcessingMetadata()
)

@kotlinx.serialization.Serializable
sealed class TextOperation {
    @kotlinx.serialization.Serializable
    data class FilterUrls(val remove: Boolean = true) : TextOperation()
    
    @kotlinx.serialization.Serializable
    data class FilterBrackets(val types: List<BracketType>) : TextOperation()
    
    @kotlinx.serialization.Serializable
    data class RemoveHeaders(val patterns: List<String>) : TextOperation()
    
    @kotlinx.serialization.Serializable
    data class SemanticSegment(val maxLength: Int = 500) : TextOperation()
    
    @kotlinx.serialization.Serializable
    data class OptimizePronunciation(val customDict: Map<String, String> = emptyMap()) : TextOperation()
}

@kotlinx.serialization.Serializable
enum class BracketType { ROUND, SQUARE, CURLY, ANGLE }

@kotlinx.serialization.Serializable
data class ProcessingContext(
    val documentType: String? = null,
    val language: String = "zh-CN"
)

@kotlinx.serialization.Serializable
data class TextChange(
    val type: ChangeType,
    val original: String,
    val replacement: String,
    val position: IntRange
)

@kotlinx.serialization.Serializable
enum class ChangeType { REMOVAL, REPLACEMENT, SEGMENTATION }

@kotlinx.serialization.Serializable
data class ProcessingMetadata(
    val originalLength: Int = 0,
    val processedLength: Int = 0,
    val processingTimeMs: Long = 0,
    val operationCount: Int = 0
)

enum class TextProcessingCapability {
    FILTERING,
    SEMANTIC_ANALYSIS,
    CONTENT_OPTIMIZATION
}