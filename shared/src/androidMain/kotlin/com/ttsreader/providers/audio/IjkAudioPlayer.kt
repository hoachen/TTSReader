package com.ttsreader.providers.audio

import android.content.Context
import android.net.Uri
import com.ttsreader.core.audio.AudioPlayer
import com.ttsreader.core.audio.PlaybackListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.File
import java.io.FileOutputStream

class IjkAudioPlayer(private val context: Context) : AudioPlayer {
    
    private var mediaPlayer: IjkMediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    private val _volume = MutableStateFlow(1.0f)
    private val _pitch = MutableStateFlow(1.0f)
    
    private val listeners = mutableListOf<PlaybackListener>()
    private var updateTimer: android.os.Handler? = null
    private var positionRunnable: Runnable? = null
    
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    override val duration: StateFlow<Long> = _duration.asStateFlow()
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    override val pitch: StateFlow<Float> = _pitch.asStateFlow()
    
    override suspend fun loadAudio(audioData: ByteArray) = withContext(Dispatchers.IO) {
        try {
            releasePlayer()
            
            val tempFile = File.createTempFile("audio", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(audioData) }
            
            setupMediaPlayer()
            mediaPlayer?.setDataSource(tempFile.absolutePath)
            mediaPlayer?.prepareAsync()
            
        } catch (e: Exception) {
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun loadAudioFromUrl(url: String) = withContext(Dispatchers.IO) {
        try {
            releasePlayer()
            
            setupMediaPlayer()
            mediaPlayer?.setDataSource(url)
            mediaPlayer?.prepareAsync()
            
        } catch (e: Exception) {
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun loadAudioFromFile(filePath: String) = withContext(Dispatchers.IO) {
        try {
            releasePlayer()
            
            setupMediaPlayer()
            mediaPlayer?.setDataSource(filePath)
            mediaPlayer?.prepareAsync()
            
        } catch (e: Exception) {
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun play() = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startPositionUpdates()
                notifyListeners { listener -> listener.onPlaybackStarted() }
            }
        }
    }
    
    override suspend fun pause() = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopPositionUpdates()
                notifyListeners { listener -> listener.onPlaybackPaused() }
            }
        }
    }
    
    override suspend fun stop() = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.stop()
            it.seekTo(0)
            _isPlaying.value = false
            _currentPosition.value = 0L
            stopPositionUpdates()
            notifyListeners { listener -> listener.onPlaybackStopped() }
        }
    }
    
    override suspend fun seekTo(position: Long) = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.seekTo(position.toInt())
            _currentPosition.value = position
        }
    }
    
    override suspend fun setSpeed(speed: Float) = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.setSpeed(speed)
            _playbackSpeed.value = speed
        }
    }
    
    override suspend fun setVolume(volume: Float) = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.setVolume(volume, volume)
            _volume.value = volume
        }
    }
    
    override suspend fun setPitch(pitch: Float) = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.setPitch(pitch)
            _pitch.value = pitch
        }
    }
    
    override fun addPlaybackListener(listener: PlaybackListener) {
        listeners.add(listener)
    }
    
    override fun removePlaybackListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }
    
    private fun setupMediaPlayer() {
        val player = IjkMediaPlayer()
        
        player.setOnPreparedListener { mp ->
            _duration.value = mp.duration
            notifyListeners { listener -> listener.onPlaybackStarted() }
        }
        
        player.setOnCompletionListener {
            _isPlaying.value = false
            _currentPosition.value = _duration.value
            stopPositionUpdates()
            notifyListeners { listener -> listener.onPlaybackCompleted() }
        }
        
        player.setOnErrorListener { _, what, extra ->
            _isPlaying.value = false
            notifyListeners { listener -> 
                listener.onError(Exception("IjkPlayer error: $what, $extra")) 
            }
            false
        }
        
        mediaPlayer = player
    }
    
    private fun startPositionUpdates() {
        updateTimer = android.os.Handler(android.os.Looper.getMainLooper())
        positionRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    _currentPosition.value = it.currentPosition.toLong()
                }
                positionRunnable?.let { runnable ->
                    updateTimer?.postDelayed(runnable, 100)
                }
            }
        }
        positionRunnable?.run()
    }
    
    private fun stopPositionUpdates() {
        positionRunnable?.let { runnable ->
            updateTimer?.removeCallbacks(runnable)
        }
        positionRunnable = null
        updateTimer = null
    }
    
    private fun releasePlayer() {
        stopPositionUpdates()
        mediaPlayer?.let {
            it.stop()
            it.release()
            mediaPlayer = null
        }
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }
    
    private fun notifyListeners(action: (PlaybackListener) -> Unit) {
        listeners.forEach { action(it) }
    }
}