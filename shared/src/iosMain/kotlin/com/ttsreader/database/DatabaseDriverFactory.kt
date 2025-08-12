package com.ttsreader.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ttsreader.database.TTSReaderDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(TTSReaderDatabase.Schema, "ttsreader.db")
    }
}