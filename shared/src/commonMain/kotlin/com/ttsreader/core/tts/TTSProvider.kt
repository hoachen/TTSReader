package com.ttsreader.core.tts

typealias TTSAudioData = ByteArray

interface TTSProvider {
    val providerName: String
    suspend fun synthesize(request: TTSRequest): Result<TTSAudioData>
    suspend fun getAvailableVoices(): Result<List<TTSVoice>>
    fun isConfigured(): Boolean
}

@kotlinx.serialization.Serializable
data class TTSRequest(
    val text: String,
    val voice: TTSVoice,
    val parameters: TTSParameters = TTSParameters(),
    val format: TTSFormat = TTSFormat.MP3_32KHZ
)

@kotlinx.serialization.Serializable
data class TTSVoice(
    val id: String,
    val name: String,
    val language: String,
    val gender: TTSSpeakerGender,
    val provider: String
)

@kotlinx.serialization.Serializable
data class TTSParameters(
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val pitch: Float = 0.0f,
    val emotion: String? = null,
    val pronunciationDict: Map<String, String>? = null
)

@kotlinx.serialization.Serializable
enum class TTSFormat {
    MP3_32KHZ, MP3_48KHZ, WAV_16KHZ, AAC_44KHZ
}

@kotlinx.serialization.Serializable
enum class TTSSpeakerGender { MALE, FEMALE, NEUTRAL }