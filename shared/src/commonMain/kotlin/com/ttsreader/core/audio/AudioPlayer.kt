package com.ttsreader.core.audio

import kotlinx.coroutines.flow.Flow

interface AudioPlayer {
    suspend fun loadAudio(audioData: ByteArray)
    suspend fun loadAudioFromUrl(url: String)
    suspend fun loadAudioFromFile(filePath: String)
    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun seekTo(position: Long)
    suspend fun setSpeed(speed: Float)
    suspend fun setVolume(volume: Float)
    suspend fun setPitch(pitch: Float)
    
    val isPlaying: Flow<Boolean>
    val currentPosition: Flow<Long>
    val duration: Flow<Long>
    val playbackSpeed: Flow<Float>
    val volume: Flow<Float>
    val pitch: Flow<Float>
    
    fun addPlaybackListener(listener: PlaybackListener)
    fun removePlaybackListener(listener: PlaybackListener)
}

interface PlaybackListener {
    fun onPlaybackStarted()
    fun onPlaybackPaused()
    fun onPlaybackStopped()
    fun onPlaybackCompleted()
    fun onError(error: Throwable)
}

enum class PlaybackState {
    IDLE, LOADING, PLAYING, PAUSED, COMPLETED, ERROR
}

@kotlinx.serialization.Serializable
data class AudioConfig(
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f
)