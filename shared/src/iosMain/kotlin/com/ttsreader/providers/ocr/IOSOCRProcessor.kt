package com.ttsreader.providers.ocr

import com.ttsreader.core.ocr.OCRProcessor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.UIImage
import platform.Vision.*

class IOSOCRProcessor : OCRProcessor {
    
    override suspend fun processImage(imageData: ByteArray, language: String): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val data = NSData.dataWithBytes(imageData.toCValues(), imageData.size.toULong())
                val image = UIImage(data = data)
                    ?: return@withContext Result.failure(Exception("Failed to create UIImage"))
                
                processImageInternal(image, language)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun processImageFromFile(filePath: String, language: String): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val image = UIImage.imageWithContentsOfFile(filePath)
                    ?: return@withContext Result.failure(Exception("Failed to load image from file"))
                
                processImageInternal(image, language)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private suspend fun processImageInternal(image: UIImage, language: String): Result<String> {
        return try {
            val cgImage = image.CGImage
                ?: return Result.failure(Exception("Failed to get CGImage"))
            
            val requestHandler = VNImageRequestHandler(CGImage = cgImage, options = null)
            val request = VNRecognizeTextRequest().apply {
                when (language) {
                    "zh-CN", "zh-TW" -> recognitionLanguages = listOf("zh-CN", "zh-Hans")
                    "en-US" -> recognitionLanguages = listOf("en-US")
                    else -> recognitionLanguages = listOf("en-US")
                }
                usesLanguageCorrection = true
                minimumTextHeight = 0.0
            }
            
            val result = suspendCoroutine <Result<String>> { continuation ->
                requestHandler.performRequests(listOf(request), error = { error ->
                    if (error != null) {
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        val observations = request.results as? List<VNRecognizedTextObservation>
                        val text = observations?.joinToString("\n") { it.topCandidates(1).firstOrNull()?.string ?: "" }
                        
                        if (text.isNullOrBlank()) {
                            continuation.resume(Result.failure(Exception("No text found in image")))
                        } else {
                            continuation.resume(Result.success(text))
                        }
                    }
                })
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSupportedLanguages(): List<String> {
        return listOf("zh-CN", "zh-TW", "en-US", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES")
    }
}

// Helper extension for suspendCoroutine
private suspend fun <T> suspendCoroutine(block: (kotlin.coroutines.Continuation<T>) -> Unit): T {
    return kotlinx.coroutines.suspendCoroutine(block)
}