package com.ttsreader.providers.audio

import com.ttsreader.core.audio.AudioConfig
import com.ttsreader.core.audio.AudioPlayer
import com.ttsreader.core.audio.PlaybackListener
import com.ttsreader.core.audio.PlaybackState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.Foundation.*

class IOSAudioPlayer : AudioPlayer, NSObject() {
    
    private var audioPlayer: AVAudioPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    private val _currentPosition = MutableStateFlow(0L)
    private val _duration = MutableStateFlow(0L)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    private val _volume = MutableStateFlow(1.0f)
    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    
    private val listeners = mutableListOf<PlaybackListener>()
    private var timer: NSTimer? = null
    
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    override val duration: StateFlow<Long> = _duration.asStateFlow()
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    
    override suspend fun loadAudio(audioData: ByteArray) = withContext(Dispatchers.Default) {
        try {
            releasePlayer()
            
            val data = audioData.toNSData()
            val audioPlayer = AVAudioPlayer(data, "mp3", null)
            
            setupAudioPlayer(audioPlayer)
            
            this@IOSAudioPlayer.audioPlayer = audioPlayer
            _duration.value = (audioPlayer.duration * 1000).toLong()
            _playbackState.value = PlaybackState.PAUSED
            notifyListeners { listener -> listener.onPlaybackStarted() }
            
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun loadAudioFromUrl(url: String) = withContext(Dispatchers.Default) {
        try {
            releasePlayer()
            
            val nsUrl = NSURL(string = url)
            val data = NSData.dataWithContentsOfURL(nsUrl)
                ?: throw Exception("Failed to load audio from URL")
            
            val audioPlayer = AVAudioPlayer(data, "mp3", null)
            
            setupAudioPlayer(audioPlayer)
            
            this@IOSAudioPlayer.audioPlayer = audioPlayer
            _duration.value = (audioPlayer.duration * 1000).toLong()
            _playbackState.value = PlaybackState.PAUSED
            notifyListeners { listener -> listener.onPlaybackStarted() }
            
        } catch (e: Exception) {
            _playbackState.value = PlaybackState.ERROR
            notifyListeners { listener -> listener.onError(e) }
            throw e
        }
    }
    
    override suspend fun play() = withContext(Dispatchers.Main) {
        audioPlayer?.let {
            if (!it.playing) {
                it.play()
                _isPlaying.value = true
                _playbackState.value = PlaybackState.PLAYING
                startPositionTimer()
                notifyListeners { listener -> listener.onPlaybackStarted() }
            }
        }
    }
    
    override suspend fun pause() = withContext(Dispatchers.Main) {
        audioPlayer?.let {
            if (it.playing) {
                it.pause()
                _isPlaying.value = false
                _playbackState.value = PlaybackState.PAUSED
                stopPositionTimer()
                notifyListeners { listener -> listener.onPlaybackPaused() }
            }
        }
    }
    
    override suspend fun stop() = withContext(Dispatchers.Main) {
        audioPlayer?.let {
            it.stop()
            _isPlaying.value = false
            _currentPosition.value = 0L
            _playbackState.value = PlaybackState.STOPPED
            stopPositionTimer()
            notifyListeners { listener -> listener.onPlaybackStopped() }
        }
    }
    
    override suspend fun seekTo(position: Long) = withContext(Dispatchers.Main) {
        audioPlayer?.let {
            it.currentTime = position.toDouble() / 1000.0
            _currentPosition.value = position
        }
    }
    
    override suspend fun setSpeed(speed: Float) = withContext(Dispatchers.Main) {
        audioPlayer?.let {
            it.rate = speed.toDouble()
            _playbackSpeed.value = speed
        }
    }
    
    override suspend fun setVolume(volume: Float) = withContext(Dispatchers.Main) {
        audioPlayer?.let {
            it.volume = volume.toDouble()
            _volume.value = volume
        }
    }
    
    override fun addPlaybackListener(listener: PlaybackListener) {
        listeners.add(listener)
    }
    
    override fun removePlaybackListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }
    
    private fun setupAudioPlayer(audioPlayer: AVAudioPlayer) {
        audioPlayer.delegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
            override fun audioPlayerDidFinishPlaying(
                player: AVAudioPlayer,
                successfully: Boolean
            ) {
                _isPlaying.value = false
                _playbackState.value = PlaybackState.COMPLETED
                stopPositionTimer()
                notifyListeners { listener -> listener.onPlaybackCompleted() }
            }
            
            override fun audioPlayerDecodeErrorDidOccur(
                player: AVAudioPlayer,
                error: NSError?
            ) {
                _playbackState.value = PlaybackState.ERROR
                notifyListeners { listener -> listener.onError(Exception(error?.localizedDescription)) }
            }
        }
    }
    
    private fun startPositionTimer() {
        stopPositionTimer()
        timer = NSTimer.scheduledTimerWithTimeInterval(0.1, repeats = true) { _ ->
            audioPlayer?.let {
                _currentPosition.value = (it.currentTime * 1000).toLong()
            }
        }
    }
    
    private fun stopPositionTimer() {
        timer?.invalidate()
        timer = null
    }
    
    private fun releasePlayer() {
        audioPlayer?.let {
            it.stop()
            it.delegate = null
            audioPlayer = null
        }
        stopPositionTimer()
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
        _playbackState.value = PlaybackState.IDLE
    }
    
    private fun notifyListeners(action: (PlaybackListener) -> Unit) {
        listeners.forEach { action(it) }
    }
    
    private fun ByteArray.toNSData(): NSData {
        return NSData.dataWithBytes(this.refTo(0), this.size.toULong())
    }
    
    private fun NSData.toByteArray(): ByteArray {
        val bytes = ByteArray(this.length.toInt())
        this.getBytes(bytes.refTo(0), this.length)
        return bytes
    }
}

// Helper extension for refTo
private fun ByteArray.refTo(index: Int): CPointer<ByteVar> {
    return this@refTo.refTo(index)
}