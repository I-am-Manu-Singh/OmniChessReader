package com.creativeblisslabs.omnimind.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE conversation_messages ADD COLUMN attachmentType TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE conversation_messages ADD COLUMN attachmentName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE conversation_messages ADD COLUMN attachmentPath TEXT NOT NULL DEFAULT ''")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tool_actions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                toolKey TEXT NOT NULL,
                title TEXT NOT NULL,
                input TEXT NOT NULL,
                output TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

// Migration 6→7: Recreate book_chunks with NO_ACTION foreign key
// Previously CASCADE was deleting all chunks whenever a book row was replaced
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Copy existing chunks to temp table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS book_chunks_backup (
                chunkId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookId TEXT NOT NULL,
                chunkIndex INTEGER NOT NULL,
                text TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("INSERT INTO book_chunks_backup SELECT chunkId, bookId, chunkIndex, text FROM book_chunks")

        // 2. Drop old table with CASCADE constraint
        db.execSQL("DROP TABLE IF EXISTS book_chunks")

        // 3. Recreate with NO_ACTION (no cascade delete on book replace)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS book_chunks (
                chunkId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bookId TEXT NOT NULL,
                chunkIndex INTEGER NOT NULL,
                text TEXT NOT NULL,
                FOREIGN KEY (bookId) REFERENCES library_books(id) ON DELETE NO ACTION
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_book_chunks_bookId ON book_chunks (bookId)")

        // 4. Restore data
        db.execSQL("INSERT INTO book_chunks SELECT chunkId, bookId, chunkIndex, text FROM book_chunks_backup")
        db.execSQL("DROP TABLE IF EXISTS book_chunks_backup")
    }
}
