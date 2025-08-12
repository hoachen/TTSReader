package com.ttsreader.providers.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ttsreader.core.ocr.OCRProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class AndroidOCRProcessor(private val context: Context) : OCRProcessor {
    
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }
    
    private val latinRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    override suspend fun processImage(imageData: ByteArray, language: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                    ?: return@withContext Result.failure(Exception("Failed to decode image"))
                
                processImageInternal(bitmap, language)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun processImageFromFile(filePath: String, language: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(filePath)
                    ?: return@withContext Result.failure(Exception("Failed to decode image"))
                
                processImageInternal(bitmap, language)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun processImageInternal(bitmap: Bitmap, language: String): Result<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizerToUse = when (language) {
                "zh-CN", "zh-TW" -> recognizer
                else -> latinRecognizer
            }
            
            val result = recognizerToUse.process(image).await()
            val text = result.text
            
            if (text.isBlank()) {
                Result.failure(Exception("No text found in image"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getSupportedLanguages(): List<String> {
        return listOf("zh-CN", "zh-TW", "en-US", "ja-JP", "ko-KR", "fr-FR", "de-DE", "es-ES")
    }
}

// Extension for CameraController
fun LifecycleCameraController.takePicture(
    context: Context,
    onSuccess: (ByteArray) -> Unit,
    onError: (Exception) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    
    takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            image.close()
            onSuccess(bytes)
        }
        
        override fun onError(exception: ImageCaptureException) {
            onError(exception)
        }
    })
}