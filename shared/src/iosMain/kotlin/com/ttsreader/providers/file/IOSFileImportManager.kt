package com.ttsreader.providers.file

import com.ttsreader.core.file.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

class IOSFileImportManager : FileImportManager {
    
    override suspend fun importFile(filePath: String, fileType: SupportedFileType): Result<ImportedFile> {
        return withContext(Dispatchers.Default) {
            try {
                val content = extractText(filePath, fileType).getOrThrow()
                val fileName = NSURL.fileURLWithPath(filePath).lastPathComponent ?: "unknown"
                val fileSize = getFileSize(filePath)
                val lastModified = getLastModified(filePath)
                
                val metadata = FileMetadata(
                    wordCount = content.split(Regex("\\s+")).count { it.isNotBlank() },
                    charCount = content.length,
                    readingTimeMinutes = calculateReadingTime(content),
                    fileSize = fileSize,
                    lastModified = lastModified
                )
                
                Result.success(
                    ImportedFile(
                        originalPath = filePath,
                        fileName = fileName,
                        fileType = fileType,
                        content = content,
                        metadata = metadata
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun extractText(filePath: String, fileType: SupportedFileType): Result<String> {
        return withContext(Dispatchers.Default) {
            try {
                val url = NSURL.fileURLWithPath(filePath)
                val text = when (fileType) {
                    SupportedFileType.PDF -> extractTextFromPdf(url)
                    SupportedFileType.TXT -> extractTextFromTxt(url)
                    SupportedFileType.DOC -> Result.failure(Exception("DOC format not supported on iOS"))
                    SupportedFileType.DOCX -> extractTextFromDocx(url)
                    SupportedFileType.MD -> extractTextFromTxt(url)
                }
                text
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override fun getSupportedFileTypes(): List<SupportedFileType> {
        return listOf(
            SupportedFileType.PDF,
            SupportedFileType.TXT,
            SupportedFileType.DOCX,
            SupportedFileType.MD
        )
    }
    
    private fun extractTextFromPdf(url: NSURL): Result<String> {
        return try {
            val pdfDocument = PDFDocument.alloc().initWithURL(url)
                ?: return Result.failure(Exception("Failed to load PDF"))
            
            val text = pdfDocument.string()
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractTextFromTxt(url: NSURL): Result<String> {
        return try {
            val text = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null)
            Result.success(text.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun extractTextFromDocx(url: NSURL): Result<String> {
        return try {
            // For iOS, we'll use a simple approach to extract text from DOCX
            // In a real app, you might want to use a proper DOCX parser library
            val data = NSData.dataWithContentsOfURL(url)
                ?: return Result.failure(Exception("Failed to load DOCX"))
            
            // This is a simplified approach - in production, use a proper DOCX parser
            val stringData = NSString.stringWithData(data, NSUTF8StringEncoding)
            val text = stringData.toString()
            
            // Remove XML tags and extract text content
            val cleanText = text.replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            
            Result.success(cleanText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getFileSize(filePath: String): Long {
        val fileManager = NSFileManager.defaultManager()
        val attributes = fileManager.attributesOfItemAtPath(filePath, null)
        return attributes?.get(NSFileSize)?.let { (it as NSNumber).longLongValue } ?: 0L
    }
    
    private fun getLastModified(filePath: String): Long {
        val fileManager = NSFileManager.defaultManager()
        val attributes = fileManager.attributesOfItemAtPath(filePath, null)
        return attributes?.get(NSFileModificationDate)?.let { (it as NSDate).timeIntervalSince1970.toLong() * 1000 } ?: System.currentTimeMillis()
    }
    
    private fun calculateReadingTime(text: String): Int {
        val wordCount = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val wordsPerMinute = 200 // Average reading speed
        return (wordCount / wordsPerMinute).coerceAtLeast(1)
    }
}