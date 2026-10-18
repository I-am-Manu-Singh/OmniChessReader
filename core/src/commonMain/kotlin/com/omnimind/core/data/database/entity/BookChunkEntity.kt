package com.creativeblisslabs.omnimind.core.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_chunks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class BookChunkEntity(
    @PrimaryKey(autoGenerate = true) val chunkId: Long = 0L,
    val bookId: String,
    val chunkIndex: Int,
    val text: String
)
