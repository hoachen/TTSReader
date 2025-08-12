package com.ttsreader.core.ocr

import kotlinx.coroutines.flow.Flow

interface OCRProcessor {
    suspend fun processImage(imageData: ByteArray, language: String = "zh-CN"): Result<String>
    suspend fun processImageFromFile(filePath: String, language: String = "zh-CN"): Result<String>
    fun getSupportedLanguages(): List<String>
}

@kotlinx.serialization.Serializable
data class OCRResult(
    val text: String,
    val confidence: Float,
    val language: String,
    val blocks: List<TextBlock> = emptyList()
)

@kotlinx.serialization.Serializable
data class TextBlock(
    val text: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

@kotlinx.serialization.Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)