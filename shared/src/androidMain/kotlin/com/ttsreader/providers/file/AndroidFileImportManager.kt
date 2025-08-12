package com.ttsreader.providers.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.ttsreader.core.file.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.BufferedReader
import java.io.InputStreamReader

class AndroidFileImportManager(private val context: Context) : FileImportManager {
    
    override suspend fun importFile(filePath: String, fileType: SupportedFileType): Result<ImportedFile> {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(filePath)
                val content = extractText(filePath, fileType).getOrThrow()
                val fileName = getFileNameFromUri(uri)
                val fileSize = getFileSizeFromUri(uri)
                val lastModified = getLastModifiedFromUri(uri)
                
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
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(filePath)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val text = when (fileType) {
                        SupportedFileType.PDF -> extractTextFromPdf(inputStream)
                        SupportedFileType.TXT -> extractTextFromTxt(inputStream)
                        SupportedFileType.DOC -> extractTextFromDoc(inputStream)
                        SupportedFileType.DOCX -> extractTextFromDocx(inputStream)
                        SupportedFileType.MD -> extractTextFromTxt(inputStream) // Same as TXT
                    }
                    Result.success(text)
                } ?: Result.failure(Exception("Cannot open file"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override fun getSupportedFileTypes(): List<SupportedFileType> {
        return SupportedFileType.entries
    }
    
    private fun extractTextFromPdf(inputStream: java.io.InputStream): String {
        return PDDocument.load(inputStream).use { document ->
            PDFTextStripper().getText(document)
        }
    }
    
    private fun extractTextFromTxt(inputStream: java.io.InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).use { reader ->
            reader.readText()
        }
    }
    
    private fun extractTextFromDoc(inputStream: java.io.InputStream): String {
        return HWPFDocument(inputStream).use { document ->
            document.documentText.toString()
        }
    }
    
    private fun extractTextFromDocx(inputStream: java.io.InputStream): String {
        return XWPFDocument(inputStream).use { document ->
            document.paragraphs.joinToString("\n") { it.text }
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }
    
    private fun getFileSizeFromUri(uri: Uri): Long {
        var fileSize = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }
        return fileSize
    }
    
    private fun getLastModifiedFromUri(uri: Uri): Long {
        return DocumentFile.fromSingleUri(context, uri)?.lastModified() ?: System.currentTimeMillis()
    }
    
    private fun calculateReadingTime(text: String): Int {
        val wordCount = text.split(Regex("\\s+")).count { it.isNotBlank() }
        val wordsPerMinute = 200 // Average reading speed
        return (wordCount / wordsPerMinute).coerceAtLeast(1)
    }
}