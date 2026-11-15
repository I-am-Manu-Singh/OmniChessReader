package com.creativeblisslabs.omnichessreader.core.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun getDatabaseBuilder(): OmniChessReaderDatabase {
    val dbFile = File(System.getProperty("user.home"), ".omnichessreader/omnichessreader.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<OmniChessReaderDatabase>(
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
