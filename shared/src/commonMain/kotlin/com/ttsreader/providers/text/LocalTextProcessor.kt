package com.ttsreader.providers.text

import com.ttsreader.core.text.*

class LocalTextProcessor : TextProcessor {
    
    override val processorName: String = "local"
    
    override suspend fun process(request: TextProcessingRequest): Result<TextProcessingResult> {
        return try {
            val startTime = System.currentTimeMillis()
            var processedText = request.text
            val changes = mutableListOf<TextChange>()
            
            request.operations.forEach { operation ->
                val originalLength = processedText.length
                val result = when (operation) {
                    is TextOperation.FilterUrls -> filterUrls(processedText)
                    is TextOperation.FilterBrackets -> filterBrackets(processedText, operation.types)
                    is TextOperation.RemoveHeaders -> removeHeaders(processedText, operation.patterns)
                    is TextOperation.SemanticSegment -> segmentText(processedText, operation.maxLength)
                    is TextOperation.OptimizePronunciation -> optimizePronunciation(processedText, operation.customDict)
                }
                
                if (result != processedText) {
                    changes.add(
                        TextChange(
                            type = ChangeType.REPLACEMENT,
                            original = processedText,
                            replacement = result,
                            position = 0..originalLength
                        )
                    )
                    processedText = result
                }
            }
            
            val processingTime = System.currentTimeMillis() - startTime
            
            Result.success(
                TextProcessingResult(
                    processedText = processedText,
                    changes = changes,
                    metadata = ProcessingMetadata(
                        originalLength = request.text.length,
                        processedLength = processedText.length,
                        processingTimeMs = processingTime,
                        operationCount = request.operations.size
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getCapabilities(): Set<TextProcessingCapability> = setOf(
        TextProcessingCapability.FILTERING
    )
    
    private fun filterUrls(text: String): String {
        val urlRegex = """https?://[^\s]+|www\.[^\s]+|[\w.-]+@[\w.-]+\.\w+""".toRegex()
        return urlRegex.replace(text, "").trim()
    }
    
    private fun filterBrackets(text: String, types: List<BracketType>): String {
        var result = text
        
        types.forEach { type ->
            val (open, close) = when (type) {
                BracketType.ROUND -> "(" to ")"
                BracketType.SQUARE -> "[" to "]"
                BracketType.CURLY -> "{" to "}"
                BracketType.ANGLE -> "<" to ">"
            }
            
            val pattern = "\\$open[^$close]*\\$close".toRegex()
            result = pattern.replace(result, "").trim()
        }
        
        return result
    }
    
    private fun removeHeaders(text: String, patterns: List<String>): String {
        var result = text
        patterns.forEach { pattern ->
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            result = regex.replace(result, "").trim()
        }
        return result
    }
    
    private fun segmentText(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        
        val sentences = text.split("[。！？.!?]".toRegex())
        val segments = mutableListOf<String>()
        var currentSegment = StringBuilder()
        
        sentences.forEach { sentence ->
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) return@forEach
            
            if (currentSegment.length + trimmed.length <= maxLength) {
                currentSegment.append(trimmed)
                currentSegment.append("。")
            } else {
                if (currentSegment.isNotEmpty()) {
                    segments.add(currentSegment.toString().trim())
                    currentSegment = StringBuilder()
                }
                
                // Handle very long sentences
                if (trimmed.length > maxLength) {
                    var start = 0
                    while (start < trimmed.length) {
                        val end = minOf(start + maxLength, trimmed.length)
                        val segment = trimmed.substring(start, end)
                        segments.add(segment)
                        start = end
                    }
                } else {
                    currentSegment.append(trimmed)
                    currentSegment.append("。")
                }
            }
        }
        
        if (currentSegment.isNotEmpty()) {
            segments.add(currentSegment.toString().trim())
        }
        
        return segments.joinToString(" ")
    }
    
    private fun optimizePronunciation(text: String, customDict: Map<String, String>): String {
        var result = text
        customDict.forEach { (original, replacement) ->
            result = result.replace(original, replacement, ignoreCase = true)
        }
        return result
    }
}