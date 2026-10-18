package com.creativeblisslabs.omnimind.core.data.database.dao

import androidx.room.*
import com.creativeblisslabs.omnimind.core.data.database.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM library_books ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM library_books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: BookEntity)

    @Upsert
    suspend fun upsertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE library_books SET progressPercent = :progress, currentChapterIndex = :chapterIndex, lastReadPosition = :position WHERE id = :id")
    suspend fun updateReadingProgress(id: String, progress: Int, chapterIndex: Int, position: Long)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("DELETE FROM library_books WHERE id = :id")
    suspend fun deleteBookById(id: String)
}
