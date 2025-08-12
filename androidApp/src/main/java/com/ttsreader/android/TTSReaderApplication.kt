package com.ttsreader.android

import android.app.Application
import com.ttsreader.android.di.AppModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class TTSReaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@TTSReaderApplication)
            modules(AppModule.module)
        }
    }
}