package com.ttsreader.providers.tts

import com.ttsreader.core.tts.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

class MiniMaxTTSProvider(
    private val apiKey: String,
    private val groupId: String,
    private val httpClient: HttpClient
) : TTSProvider {
    
    override val providerName: String = "minimax"
    
    companion object {
        private const val BASE_URL = "https://api.minimaxi.com"
        private const val TTS_ENDPOINT = "/v1/t2a_v2"
        private const val DEFAULT_MODEL = "speech-2.5-hd-preview"
    }
    
    override suspend fun synthesize(request: TTSRequest): Result<TTSAudioData> {
        return try {
            val miniMaxRequest = MiniMaxTTSRequest(
                model = DEFAULT_MODEL,
                text = request.text,
                stream = false,
                voice_setting = VoiceSetting(
                    voice_id = request.voice.id,
                    speed = request.parameters.speed.coerceIn(0.5f, 2.0f),
                    vol = request.parameters.volume.coerceIn(0f, 2f),
                    pitch = request.parameters.pitch.coerceIn(-12f, 12f),
                    emotion = request.parameters.emotion ?: "neutral"
                ),
                pronunciation_dict = request.parameters.pronunciationDict?.let {
                    PronunciationDict(
                        tone = it.map { (k, v) -> "$k/$v" }
                    )
                },
                audio_setting = AudioSetting(
                    sample_rate = when (request.format) {
                        TTSFormat.MP3_32KHZ -> 32000
                        TTSFormat.MP3_48KHZ -> 48000
                        TTSFormat.WAV_16KHZ -> 16000
                        TTSFormat.AAC_44KHZ -> 44100
                    },
                    bitrate = 128000,
                    format = "mp3",
                    channel = 1
                )
            )
            
            val response: HttpResponse = httpClient.post("$BASE_URL$TTS_ENDPOINT?GroupId=$groupId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $apiKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                setBody(miniMaxRequest)
            }
            
            if (response.status.isSuccess()) {
                Result.success(response.body<ByteArray>())
            } else {
                Result.failure(Exception("MiniMax API error: ${response.status} - ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAvailableVoices(): Result<List<TTSVoice>> {
        return Result.success(listOf(
            TTSVoice("male-qn-qingse", "青瑟男声", "zh-CN", TTSSpeakerGender.MALE, "minimax"),
            TTSVoice("female-qn-jingying", "精英女声", "zh-CN", TTSSpeakerGender.FEMALE, "minimax"),
            TTSVoice("male-qn-jingying", "精英男声", "zh-CN", TTSSpeakerGender.MALE, "minimax"),
            TTSVoice("female-shaonv", "少女女声", "zh-CN", TTSSpeakerGender.FEMALE, "minimax"),
            TTSVoice("male-yingxiong", "英雄男声", "zh-CN", TTSSpeakerGender.MALE, "minimax"),
            TTSVoice("female-zhiyin", "知音女声", "zh-CN", TTSSpeakerGender.FEMALE, "minimax"),
            TTSVoice("female-english", "英语女声", "en-US", TTSSpeakerGender.FEMALE, "minimax"),
            TTSVoice("male-english", "英语男声", "en-US", TTSSpeakerGender.MALE, "minimax")
        ))
    }
    
    override fun isConfigured(): Boolean = apiKey.isNotBlank() && groupId.isNotBlank()
    
    @Serializable
    private data class MiniMaxTTSRequest(
        val model: String,
        val text: String,
        val stream: Boolean = false,
        val voice_setting: VoiceSetting,
        val pronunciation_dict: PronunciationDict? = null,
        val audio_setting: AudioSetting
    )
    
    @Serializable
    private data class VoiceSetting(
        val voice_id: String,
        val speed: Float = 1.0f,
        val vol: Float = 1.0f,
        val pitch: Float = 0.0f,
        val emotion: String = "neutral"
    )
    
    @Serializable
    private data class PronunciationDict(
        val tone: List<String>
    )
    
    @Serializable
    private data class AudioSetting(
        val sample_rate: Int = 32000,
        val bitrate: Int = 128000,
        val format: String = "mp3",
        val channel: Int = 1
    )
}