package com.ttsreader.core.cache

import android.content.Context
import com.ttsreader.core.tts.TTSAudioData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileTTSCache(
    private val context: Context,
    private val maxSizeBytes: Long = 100 * 1024 * 1024 // 100MB
) : TTSCache {
    private val cacheDir: File by lazy {
        File(context.cacheDir, "tts_cache").apply { mkdirs() }
    }
    
    override suspend fun get(key: String): TTSAudioData? = withContext(Dispatchers.IO) {
        val file = File(cacheDir, key)
        if (file.exists() && file.isFile) {
            file.readBytes()
        } else {
            null
        }
    }
    
    override suspend fun put(key: String, audio: TTSAudioData) = withContext(Dispatchers.IO) {
        val file = File(cacheDir, key)
        file.writeBytes(audio)
        cleanupOldFiles()
    }
    
    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val file = File(cacheDir, key)
        if (file.exists()) file.delete()
    }
    
    override suspend fun clear() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
    
    override suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    override suspend fun getKeys(): Set<String> = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
    }
    
    private suspend fun cleanupOldFiles() = withContext(Dispatchers.IO) {
        val files = cacheDir.listFiles() ?: return@withContext
        
        if (files.sumOf { it.length() } > maxSizeBytes) {
            // Sort by last modified and remove oldest
            files.sortedBy { it.lastModified() }
                .takeWhile { files.sumOf { it.length() } > maxSizeBytes }
                .forEach { it.delete() }
        }
    }
}