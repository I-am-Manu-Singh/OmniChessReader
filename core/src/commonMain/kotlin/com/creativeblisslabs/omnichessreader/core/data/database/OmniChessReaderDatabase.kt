package com.creativeblisslabs.omnichessreader.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.creativeblisslabs.omnichessreader.core.data.database.dao.*
import com.creativeblisslabs.omnichessreader.core.data.database.entity.*

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        BookChunkEntity::class,
    ],
    version = 7,
    exportSchema = true
)
abstract class OmniChessReaderDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookChunkDao(): BookChunkDao
}

// Platform-specific builder defined in each source set
expect fun getDatabaseBuilder(): OmniChessReaderDatabase
