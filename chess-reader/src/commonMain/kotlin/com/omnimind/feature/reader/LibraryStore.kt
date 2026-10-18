package com.creativeblisslabs.omnimind.feature.reader
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.creativeblisslabs.omnimind.core.data.database.dao.BookDao
import com.creativeblisslabs.omnimind.core.data.database.entity.BookEntity
import com.creativeblisslabs.omnimind.core.util.UuidUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

data class LibraryBook(
    val id: String = UuidUtils.generateUUID(),
    val title: String,
    val author: String,
    val progress: Int = 0,
    val currentChapterIndex: Int = 0,
    val lastReadPosition: Long = 0L,
    val mood: String = "Reading",
    val isFinished: Boolean = false,
    val filePath: String = "",
    val fileType: String = "EPUB",
    val publisher: String = "Unknown Publisher",
    val publishedDate: String = "",
    val addedDate: String = "",
    val fileSize: String = "",
    val subjects: String = "",
    val descriptionText: String = "",
    val chapters: List<String> = emptyList(),
    val coverImagePath: String = ""
)

object LibraryStore {
    var books by mutableStateOf<List<LibraryBook>>(emptyList())
    private var isInitialized = false

    fun initDatabase(dao: BookDao, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            dao.getAllBooks().collectLatest { entities ->
                val newBooks = entities.map { it.toLibraryBook() }
                withContext(Dispatchers.Main) {
                    books = newBooks
                }
            }
        }
    }

    fun addBook(book: LibraryBook, dao: BookDao?, scope: CoroutineScope) {
        val existingIndex = books.indexOfFirst { it.id == book.id }
        if (existingIndex != -1) {
            books = books.toMutableList().apply {
                this[existingIndex] = book
            }
        } else {
            books = listOf(book) + books
        }

        dao?.let {
            scope.launch(Dispatchers.IO) {
                if (existingIndex != -1) {
                    // Update existing — use upsertBook which does INSERT OR REPLACE safely
                    // but does NOT delete+reinsert so chunks are preserved
                    it.updateBook(book.toEntity())
                } else {
                    // New book — plain insert (IGNORE strategy, no cascade risk)
                    it.insertBook(book.toEntity())
                }
            }
        }
    }

    fun updateProgress(bookId: String, progress: Int, chapterIndex: Int, lastReadPosition: Long = 0L, dao: BookDao?, scope: CoroutineScope) {
        books = books.map {
            if (it.id == bookId) it.copy(progress = progress, currentChapterIndex = chapterIndex, lastReadPosition = lastReadPosition, isFinished = progress >= 100) else it
        }
        dao?.let {
            scope.launch(Dispatchers.IO) {
                it.updateReadingProgress(bookId, progress, chapterIndex, lastReadPosition)
            }
        }
    }

    fun deleteBook(book: LibraryBook, dao: BookDao?, scope: CoroutineScope, chunkDao: com.creativeblisslabs.omnimind.core.data.database.dao.BookChunkDao? = null) {
        books = books.filter { it.id != book.id }
        dao?.let {
            scope.launch(Dispatchers.IO) {
                // Must delete chunks first — NO_ACTION FK means DB rejects book delete if chunks exist
                chunkDao?.deleteChunksForBook(book.id)
                it.deleteBookById(book.id)
            }
        }
    }
}

fun BookEntity.toLibraryBook(): LibraryBook {
    val chaptersList = runCatching {
        com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.decodeFromString<List<String>>(chaptersJson)
    }.getOrDefault(emptyList())

    return LibraryBook(
        id = id,
        title = title,
        author = author,
        progress = progressPercent,
        currentChapterIndex = currentChapterIndex,
        lastReadPosition = lastReadPosition,
        mood = mood,
        isFinished = isFinished,
        filePath = filePath,
        fileType = fileType,
        publisher = publisher,
        publishedDate = publishedDate,
        addedDate = addedDate,
        fileSize = fileSize,
        subjects = subjects,
        descriptionText = descriptionText,
        chapters = chaptersList,
        coverImagePath = coverImagePath
    )
}

fun LibraryBook.toEntity(): BookEntity {
    val chaptersJsonStr = runCatching {
        com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<String>()),
            chapters
        )
    }.getOrDefault("[]")

    return BookEntity(
        id = id,
        title = title,
        author = author,
        filePath = filePath,
        fileType = fileType,
        progressPercent = progress,
        currentChapterIndex = currentChapterIndex,
        lastReadPosition = lastReadPosition,
        mood = mood,
        isFinished = isFinished,
        publisher = publisher,
        publishedDate = publishedDate,
        addedDate = addedDate,
        fileSize = fileSize,
        subjects = subjects,
        descriptionText = descriptionText,
        chaptersJson = chaptersJsonStr,
        coverImagePath = coverImagePath
    )
}
