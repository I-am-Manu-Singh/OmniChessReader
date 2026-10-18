package com.creativeblisslabs.omnimind.feature.reader
import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

data class ReaderSessionState(
    val bookId: String = "",
    val title: String = "No Active Document",
    val author: String = "Select a book from Library",
    val progress: Int = 0,
    val currentChapter: Int = 0,
    val lastReadPosition: Long = 0L,
    val mood: String = "Unread",
    val tags: List<String> = emptyList(),
    val sourcePath: String = "",
    val chunks: List<String> = emptyList(),
    val chapters: List<String> = emptyList(),
    val importedTitle: String? = null,
    val publisher: String = "",
    val publishedDate: String = "",
    val addedDate: String = "",
    val updatedDate: String = "",
    val format: String = "EPUB",
    val fileSize: String = "",
    val subjects: String = "",
    val descriptionText: String = "",
    val isPasswordRequired: Boolean = false,
    val coverImagePath: String = ""
)

object ReaderSessionStore {
    var state by mutableStateOf(ReaderSessionState())
        private set

    fun update(block: ReaderSessionState.() -> ReaderSessionState) {
        val oldState = state
        val newState = oldState.block()
        // Never allow an empty chunks update to overwrite pre-existing valid chunks
        if (oldState.chunks.isNotEmpty() && newState.chunks.isEmpty()) {
            println("READER_LOG: ReaderSessionStore.update IGNORED empty chunks overwrite.")
            state = newState.copy(chunks = oldState.chunks)
        } else {
            state = newState
        }
    }

    fun loadBook(book: LibraryBook, bookChunks: List<String> = emptyList()) {
        println("READER_LOG: ReaderSessionStore.loadBook called for bookId=${book.id}, chunksCount=${bookChunks.size}, currentChapter=${book.currentChapterIndex}, position=${book.lastReadPosition}")
        val chaptersList = if (book.chapters.filter { it.isNotBlank() }.isNotEmpty()) {
            book.chapters.filter { it.isNotBlank() }
        } else if (bookChunks.isNotEmpty()) {
            val combined = bookChunks.joinToString("\n\n")
            val autoSplit = combined.split(Regex("(?m)^---\\s*$"))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            
            if (autoSplit.size > 1) {
                autoSplit
            } else {
                val regexSplit = combined.split(Regex("(?i)(chapter\\s+\\d+[:\\-]|part\\s+\\d+[:\\-])"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                if (regexSplit.size > 1) regexSplit else listOf(combined)
            }
        } else {
            emptyList()
        }

        state = ReaderSessionState(
            bookId = book.id,
            title = book.title,
            author = book.author,
            progress = book.progress,
            currentChapter = book.currentChapterIndex,
            lastReadPosition = book.lastReadPosition,
            mood = book.mood,
            sourcePath = book.filePath,
            chunks = bookChunks,
            chapters = chaptersList,
            importedTitle = book.title,
            publisher = book.publisher.ifBlank { "OmniMind Local Reader" },
            publishedDate = book.publishedDate.ifBlank { "Local Import" },
            addedDate = book.addedDate.ifBlank { "Recently Added" },
            updatedDate = "Active Session",
            format = book.fileType,
            fileSize = book.fileSize.ifBlank { "${bookChunks.sumOf { it.length } / 1024} KB" },
            subjects = book.subjects.ifBlank { "Local Library Document" },
            descriptionText = book.descriptionText.ifBlank { "No extended description provided for this document." },
            coverImagePath = book.coverImagePath
        )
    }
}
