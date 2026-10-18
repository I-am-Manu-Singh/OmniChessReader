package com.creativeblisslabs.omnimind.core.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val filePath: String,
    val fileType: String = "EPUB",
    val progressPercent: Int = 0,
    val currentChapterIndex: Int = 0,
    val lastReadPosition: Long = 0L,
    val mood: String = "Reading",
    val isFinished: Boolean = false,
    val publisher: String = "Unknown Publisher",
    val publishedDate: String = "",
    val addedDate: String = "",
    val fileSize: String = "",
    val subjects: String = "",
    val descriptionText: String = "",
    val chaptersJson: String = "[]",
    val coverImagePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
