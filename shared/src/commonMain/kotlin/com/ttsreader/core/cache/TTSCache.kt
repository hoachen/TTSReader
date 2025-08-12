package com.ttsreader.core.cache

import com.ttsreader.core.tts.TTSAudioData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

interface TTSCache {
    suspend fun get(key: String): TTSAudioData?
    suspend fun put(key: String, audio: TTSAudioData)
    suspend fun remove(key: String)
    suspend fun clear()
    suspend fun getCacheSize(): Long
    suspend fun getKeys(): Set<String>
}

class InMemoryTTSCache(
    private val maxSizeBytes: Long = 50 * 1024 * 1024 // 50MB
) : TTSCache {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()
    private var currentSizeBytes = 0L
    
    data class CacheEntry(
        val audio: TTSAudioData,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    override suspend fun get(key: String): TTSAudioData? = mutex.withLock {
        cache[key]?.let { entry ->
            // Update timestamp on access
            cache[key] = entry.copy(timestamp = System.currentTimeMillis())
            entry.audio
        }
    }
    
    override suspend fun put(key: String, audio: TTSAudioData) = mutex.withLock {
        val entrySize = audio.size.toLong()
        
        // Remove existing entry if any
        cache[key]?.let { existing ->
            currentSizeBytes -= existing.audio.size
        }
        
        // Evict old entries if necessary
        while (currentSizeBytes + entrySize > maxSizeBytes && cache.isNotEmpty()) {
            val oldest = cache.minByOrNull { it.value.timestamp }
            oldest?.let { (oldKey, oldEntry) ->
                cache.remove(oldKey)
                currentSizeBytes -= oldEntry.audio.size
            }
        }
        
        // Add new entry
        cache[key] = CacheEntry(audio)
        currentSizeBytes += entrySize
    }
    
    override suspend fun remove(key: String) = mutex.withLock {
        cache[key]?.let { entry ->
            currentSizeBytes -= entry.audio.size
            cache.remove(key)
        }
    }
    
    override suspend fun clear() = mutex.withLock {
        cache.clear()
        currentSizeBytes = 0L
    }
    
    override suspend fun getCacheSize(): Long = mutex.withLock {
        currentSizeBytes
    }
    
    override suspend fun getKeys(): Set<String> = mutex.withLock {
        cache.keys.toSet()
    }
    
    private fun generateCacheKey(text: String, voiceId: String, parameters: String): String {
        // Simple hash-based key generation
        val combined = "$text|$voiceId|$parameters"
        return abs(combined.hashCode()).toString()
    }
}

// File-based cache for persistent storage (platform-specific implementations)
expect class FileTTSCache : TTSCache