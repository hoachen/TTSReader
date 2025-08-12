package com.ttsreader.android.di

import com.ttsreader.core.cache.FileTTSCache
import com.ttsreader.core.cache.TTSCache
import com.ttsreader.providers.tts.MiniMaxTTSProvider
import com.ttsreader.providers.text.DeepSeekTextProcessor
import com.ttsreader.providers.text.LocalTextProcessor
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

object AppModule {
    val module = module {
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    })
                }
                install(Logging)
            }
        }
        
        single<TTSCache> { FileTTSCache(androidContext()) }
        
        factory { (apiKey: String, groupId: String) ->
            MiniMaxTTSProvider(apiKey, groupId, get())
        }
        
        factory { (apiKey: String) ->
            DeepSeekTextProcessor(apiKey)
        }
        
        factory { LocalTextProcessor() }
    }
}