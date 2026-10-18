package com.creativeblisslabs.omnimind.core.data.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

actual fun getDatabaseBuilder(): OmniMindDatabase {
    val dbFile = File(System.getProperty("user.home"), ".omnimind/omnimind.db")
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<OmniMindDatabase>(
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
