package com.ttsreader.core.file

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FileImportManager {
    suspend fun importFile(filePath: String, fileType: SupportedFileType): Result<ImportedFile>
    suspend fun extractText(filePath: String, fileType: SupportedFileType): Result<String>
    fun getSupportedFileTypes(): List<SupportedFileType>
}

enum class SupportedFileType(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    TXT("txt", "text/plain"),
    DOC("doc", "application/msword"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    MD("md", "text/markdown");
    
    companion object {
        fun fromFileName(fileName: String): SupportedFileType? {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return entries.find { it.extension == extension }
        }
    }
}

data class ImportedFile(
    val originalPath: String,
    val fileName: String,
    val fileType: SupportedFileType,
    val content: String,
    val metadata: FileMetadata
)

data class FileMetadata(
    val wordCount: Int,
    val charCount: Int,
    val readingTimeMinutes: Int,
    val fileSize: Long,
    val lastModified: Long
)