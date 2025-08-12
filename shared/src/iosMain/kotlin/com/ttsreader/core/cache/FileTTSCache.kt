package com.ttsreader.core.cache

import com.ttsreader.core.tts.TTSAudioData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSUserDomainMask

class FileTTSCache(
    private val maxSizeBytes: Long = 100 * 1024 * 1024 // 100MB
) : TTSCache {
    
    private val cacheDirectory: String by lazy {
        val fileManager = NSFileManager.defaultManager
        val urls = fileManager.URLsForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomains = NSUserDomainMask
        )
        val appSupportURL = urls.firstOrNull() as? NSURL
        val cacheURL = appSupportURL?.URLByAppendingPathComponent("tts_cache")
        
        cacheURL?.path?.let { path ->
            fileManager.createDirectoryAtPath(path, true, null, null)
            path
        } ?: throw IllegalStateException("Cannot create cache directory")
    }
    
    override suspend fun get(key: String): TTSAudioData? = withContext(Dispatchers.Default) {
        val filePath = "$cacheDirectory/$key"
        val fileManager = NSFileManager.defaultManager
        
        if (fileManager.fileExistsAtPath(filePath)) {
            NSData.dataWithContentsOfFile(filePath)?.toByteArray()
        } else {
            null
        }
    }
    
    override suspend fun put(key: String, audio: TTSAudioData) = withContext(Dispatchers.Default) {
        val filePath = "$cacheDirectory/$key"
        val data = audio.toNSData()
        data.writeToFile(filePath, true)
        cleanupOldFiles()
    }
    
    override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        val filePath = "$cacheDirectory/$key"
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)
    }
    
    override suspend fun clear() = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        val files = fileManager.contentsOfDirectoryAtPath(cacheDirectory, null)
        files?.forEach { file ->
            fileManager.removeItemAtPath("$cacheDirectory/$file", null)
        }
    }
    
    override suspend fun getCacheSize(): Long = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        val files = fileManager.contentsOfDirectoryAtPath(cacheDirectory, null)
        files?.sumOf { file ->
            val attributes = fileManager.attributesOfItemAtPath("$cacheDirectory/$file", null)
            attributes?.get("NSFileSize") as? Long ?: 0L
        } ?: 0L
    }
    
    override suspend fun getKeys(): Set<String> = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        val files = fileManager.contentsOfDirectoryAtPath(cacheDirectory, null)
        files?.map { it.toString() }?.toSet() ?: emptySet()
    }
    
    private suspend fun cleanupOldFiles() = withContext(Dispatchers.Default) {
        val fileManager = NSFileManager.defaultManager
        val files = fileManager.contentsOfDirectoryAtPath(cacheDirectory, null) ?: return@withContext
        
        val fileAttributes = files.mapNotNull { file ->
            val path = "$cacheDirectory/$file"
            val attributes = fileManager.attributesOfItemAtPath(path, null)
            attributes?.let { Triple(file, path, it) }
        }
        
        val totalSize = fileAttributes.sumOf { (_, _, attrs) ->
            attrs["NSFileSize"] as? Long ?: 0L
        }
        
        if (totalSize > maxSizeBytes) {
            // Sort by modification date and remove oldest
            fileAttributes
                .sortedBy { (_, _, attrs) -> attrs["NSFileModificationDate"] as? Long ?: 0L }
                .takeWhile { fileAttributes.sumOf { (_, _, attrs) -> attrs["NSFileSize"] as? Long ?: 0L } > maxSizeBytes }
                .forEach { (_, path, _) ->
                    fileManager.removeItemAtPath(path, null)
                }
        }
    }
}

private fun NSData.toByteArray(): ByteArray {
    val bytes = ByteArray(this.length.toInt())
    this.getBytes(bytes.refTo(0), this.length)
    return bytes
}

private fun ByteArray.toNSData(): NSData {
    return NSData.dataWithBytes(this.refTo(0), this.size.toULong())
}