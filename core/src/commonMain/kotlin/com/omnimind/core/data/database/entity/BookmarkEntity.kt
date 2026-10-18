package com.creativeblisslabs.omnimind.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reader_bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val title: String,
    val pageProgress: Int,
    val chapterIndex: Int,
    val noteText: String = "",
    val highlightColorHex: String = "#FFEA00",
    val createdAt: Long = System.currentTimeMillis()
)
