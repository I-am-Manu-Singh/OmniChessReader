package com.creativeblisslabs.omnimind.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.creativeblisslabs.omnimind.core.data.database.dao.*
import com.creativeblisslabs.omnimind.core.data.database.entity.*

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        BookChunkEntity::class,
    ],
    version = 7,
    exportSchema = true
)
abstract class OmniMindDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookChunkDao(): BookChunkDao
}

// Platform-specific builder defined in each source set
expect fun getDatabaseBuilder(): OmniMindDatabase
