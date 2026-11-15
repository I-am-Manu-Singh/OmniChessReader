package com.creativeblisslabs.omnichessreader.core.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext

actual fun getDatabaseBuilder(): OmniChessReaderDatabase {
    val appContext: Context = GlobalContext.get().get<Context>()
    val dbFile = appContext.getDatabasePath("omnichessreader.db")
    return Room.databaseBuilder<OmniChessReaderDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_1_2, MIGRATION_6_7)
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()
}
