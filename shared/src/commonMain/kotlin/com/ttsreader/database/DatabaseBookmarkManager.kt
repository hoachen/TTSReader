package com.ttsreader.database

import com.ttsreader.core.bookmark.Bookmark
import com.ttsreader.core.bookmark.BookmarkManager
import com.ttsreader.database.TTSReaderDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseBookmarkManager(
    private val database: TTSReaderDatabase
) : BookmarkManager {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val bookmarkQueries = database.bookmarkQueries
    
    override suspend fun addBookmark(bookmark: Bookmark) = withContext(Dispatchers.Default) {
        bookmarkQueries.insertBookmark(
            id = bookmark.id,
            text_id = bookmark.textId,
            title = bookmark.title,
            content = bookmark.content,
            position = bookmark.position.toLong(),
            audio_position = bookmark.audioPosition,
            note = bookmark.note,
            created_at = bookmark.createdAt,
            updated_at = bookmark.updatedAt,
            tags = json.encodeToString(bookmark.tags)
        )
    }
    
    override suspend fun removeBookmark(bookmarkId: String) = withContext(Dispatchers.Default) {
        bookmarkQueries.deleteBookmark(bookmarkId)
    }
    
    override suspend fun getBookmarksForText(textId: String): List<Bookmark> = withContext(Dispatchers.Default) {
        bookmarkQueries.getBookmarksForText(textId)
            .executeAsList()
            .map { it.toBookmark() }
    }
    
    override suspend fun updateBookmark(bookmark: Bookmark) = withContext(Dispatchers.Default) {
        bookmarkQueries.updateBookmark(
            title = bookmark.title,
            content = bookmark.content,
            position = bookmark.position.toLong(),
            audio_position = bookmark.audioPosition,
            note = bookmark.note,
            updated_at = bookmark.updatedAt,
            tags = json.encodeToString(bookmark.tags),
            id = bookmark.id
        )
    }
    
    override suspend fun getBookmarkById(bookmarkId: String): Bookmark? = withContext(Dispatchers.Default) {
        bookmarkQueries.getBookmarkById(bookmarkId)
            .executeAsOneOrNull()
            ?.toBookmark()
    }
    
    override fun observeBookmarksForText(textId: String): Flow<List<Bookmark>> {
        return bookmarkQueries.getBookmarksForText(textId)
            .asFlow()
            .map { query -> query.executeAsList().map { it.toBookmark() } }
    }
    
    override fun observeAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkQueries.getAllBookmarks()
            .asFlow()
            .map { query -> query.executeAsList().map { it.toBookmark() } }
    }
    
    override suspend fun exportBookmarks(textId: String?): List<Bookmark> = withContext(Dispatchers.Default) {
        if (textId != null) {
            getBookmarksForText(textId)
        } else {
            bookmarkQueries.getAllBookmarks()
                .executeAsList()
                .map { it.toBookmark() }
        }
    }
    
    override suspend fun importBookmarks(bookmarks: List<Bookmark>) = withContext(Dispatchers.Default) {
        for (bookmark in bookmarks) {
            addBookmark(bookmark)
        }
    }
    
    private fun com.ttsreader.database.Bookmark.toBookmark(): Bookmark {
        return Bookmark(
            id = id,
            textId = text_id,
            title = title,
            content = content,
            position = position.toInt(),
            audioPosition = audio_position,
            note = note,
            createdAt = created_at,
            updatedAt = updated_at,
            tags = try {
                json.decodeFromString(tags)
            } catch (e: Exception) {
                emptyList()
            }
        )
    }
}