package com.ttsreader.core.bookmark

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BookmarkManager {
    suspend fun addBookmark(bookmark: Bookmark)
    suspend fun removeBookmark(bookmarkId: String)
    suspend fun getBookmarksForText(textId: String): List<Bookmark>
    suspend fun updateBookmark(bookmark: Bookmark)
    suspend fun getBookmarkById(bookmarkId: String): Bookmark?
    
    fun observeBookmarksForText(textId: String): Flow<List<Bookmark>>
    fun observeAllBookmarks(): Flow<List<Bookmark>>
    
    suspend fun exportBookmarks(textId: String? = null): List<Bookmark>
    suspend fun importBookmarks(bookmarks: List<Bookmark>)
}

data class Bookmark(
    val id: String,
    val textId: String,
    val title: String,
    val content: String,
    val position: Int, // Character position in text
    val audioPosition: Long? = null, // Audio playback position in milliseconds
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList()
)

data class BookmarkPosition(
    val textPosition: Int,
    val audioPosition: Long?
)

sealed class BookmarkEvent {
    data class BookmarkAdded(val bookmark: Bookmark) : BookmarkEvent()
    data class BookmarkRemoved(val bookmarkId: String) : BookmarkEvent()
    data class BookmarkUpdated(val bookmark: Bookmark) : BookmarkEvent()
}