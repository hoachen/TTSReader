package com.ttsreader.providers.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.ttsreader.core.audio.AudioConfig
import com.ttsreader.core.audio.AudioPlayer
import com.ttsreader.core.audio.PlaybackListener
import com.ttsreader.core.audio.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {
    
    private var mediaPlayer: MediaPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    private val _volume = MutableStateFlow(1.0f)
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    
    private val listeners = mutableListOf<PlaybackListener>()
    
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    override val duration: StateFlow<Long> = _duration.asStateFlow()
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    
    override suspend fun loadAudio(audioData: ByteArray) = withContext(Dispatchers.IO) {
        try {
            releasePlayer()
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setOnPreparedListener {
                    _duration.value = it.duration.toLong()
                    _playbackState.value = PlaybackState.PAUSED
                    notifyListeners { listener -> listener.onPlaybackStarted() }
                }
                
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackState.value = PlaybackState.COMPLETED
                    notifyListeners { listener -> listener.onPlaybackCompleted() }
                }
                
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = PlaybackState.ERROR
                    notifyListeners { listener -> listener.onError(Exception("MediaPlayer error: $what, $extra")) }
                    true
                }
                
                setDataSource(audioData.inputStream().buffered().use { it.readBytes() }.let {
                    Uri.parse("data:audio/mp3;base64,${android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT)}")
                })
                prepareAsync()
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun loadAudioFromUrl(url: String) = withContext(Dispatchers.IO) {
        try {
            releasePlayer()
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setOnPreparedListener {
                    _duration.value = it.duration.toLong()
                    _playbackState.value = PlaybackState.PAUSED
                    notifyListeners { listener -> listener.onPlaybackStarted() }
                }
                
                setOnCompletionListener {
                    _isPlaying.value = false
                    _playbackState.value = PlaybackState.COMPLETED
                    notifyListeners { listener -> listener.onPlaybackCompleted() }
                }
                
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = PlaybackState.ERROR
                    notifyListeners { listener -> listener.onError(Exception("MediaPlayer error: $what, $extra")) }
                    true
                }
                
                setDataSource(context, Uri.parse(url))
                prepareAsync()
            }
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun play() = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                _playbackState.value = PlaybackState.PLAYING
                notifyListeners { listener -> listener.onPlaybackStarted() }
            }
        }
    }
    
    override suspend fun pause() = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                _playbackState.value = PlaybackState.PAUSED
                notifyListeners { listener -> listener.onPlaybackPaused() }
            }
        }
    }
    
    override suspend fun stop() = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.stop()
            _isPlaying.value = false
            _currentPosition.value = 0L
            _playbackState.value = PlaybackState.STOPPED
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                it.playbackParams = it.playbackParams.setSpeed(speed)
            }
            _playbackSpeed.value = speed
        }
    }
    
    override suspend fun setVolume(volume: Float) = withContext(Dispatchers.Main) {
        mediaPlayer?.let {
            it.setVolume(volume, volume)
            _volume.value = volume
        }
    }
    
    override fun addPlaybackListener(listener: PlaybackListener) {
        listeners.add(listener)
    }
    
    override fun removePlaybackListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }
    
    private fun notifyListeners(action: (PlaybackListener) -> Unit) {
        listeners.forEach { action(it) }
    }
    
    private fun releasePlayer() {
        mediaPlayer?.let {
            it.release()
            mediaPlayer = null
        }
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _playbackState.value = PlaybackState.IDLE
    }
}