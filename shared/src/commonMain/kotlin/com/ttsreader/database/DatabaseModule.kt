package com.ttsreader.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ttsreader.core.config.ConfigurationStorage
import com.ttsreader.database.TTSReaderDatabase

object DatabaseModule {
    
    fun createDatabase(driver: SqlDriver): TTSReaderDatabase {
        return TTSReaderDatabase(driver)
    }
    
    fun createConfigurationStorage(database: TTSReaderDatabase): ConfigurationStorage {
        return SqlDelightConfigurationStorage(database)
    }
}

class SqlDelightConfigurationStorage(
    private val database: TTSReaderDatabase
) : ConfigurationStorage {
    
    override suspend fun saveTTSConfig(provider: String, config: Map<String, String>) {
        database.ttsReaderDatabaseQueries.saveTTSConfig(
            provider = provider,
            api_key = config["apiKey"] ?: "",
            group_id = config["groupId"] ?: ""
        )
    }
    
    override suspend fun saveAIConfig(provider: String, config: Map<String, String>) {
        database.ttsReaderDatabaseQueries.saveAIConfig(
            provider = provider,
            api_key = config["apiKey"] ?: "",
            model = config["model"] ?: "",
            fallback_rules = if (config["fallbackRules"] == "true") 1 else 0
        )
    }
    
    override suspend fun getTTSConfig(): Map<String, String> {
        return database.ttsReaderDatabaseQueries.getTTSConfig()
            .executeAsOneOrNull()
            ?.let { config ->
                mapOf(
                    "provider" to config.provider,
                    "apiKey" to (config.api_key ?: ""),
                    "groupId" to (config.group_id ?: "")
                )
            } ?: emptyMap()
    }
    
    override suspend fun getAIConfig(): Map<String, String> {
        return database.ttsReaderDatabaseQueries.getAIConfig()
            .executeAsOneOrNull()
            ?.let { config ->
                mapOf(
                    "provider" to config.provider,
                    "apiKey" to (config.api_key ?: ""),
                    "model" to (config.model ?: ""),
                    "fallbackRules" to (if (config.fallback_rules == 1L) "true" else "false")
                )
            } ?: emptyMap()
    }
    
    override suspend fun clear() {
        database.ttsReaderDatabaseQueries.clearAudioCache()
    }
}

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}