package com.creativeblisslabs.omnimind.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.creativeblisslabs.omnimind.core.data.database.entity.BookChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookChunkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<BookChunkEntity>)

    @Query("SELECT * FROM book_chunks WHERE bookId = :bookId ORDER BY chunkIndex ASC")
    fun getChunksForBook(bookId: String): Flow<List<BookChunkEntity>>

    @Query("SELECT * FROM book_chunks WHERE bookId = :bookId ORDER BY chunkIndex ASC")
    suspend fun getChunksListForBook(bookId: String): List<BookChunkEntity>

    @Query("SELECT * FROM book_chunks WHERE bookId = :bookId AND chunkIndex = :chunkIndex LIMIT 1")
    suspend fun getChunk(bookId: String, chunkIndex: Int): BookChunkEntity?

    @Query("DELETE FROM book_chunks WHERE bookId = :bookId")
    suspend fun deleteChunksForBook(bookId: String)
}
