package com.creativeblisslabs.omnimind.chess.shared.logic

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.creativeblisslabs.omnimind.chess.shared.OpeningPosition
import com.creativeblisslabs.omnimind.chess.shared.OpeningRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidOpeningRepository(private val context: Context) : OpeningRepository {
    private val dbName = "chess_openings.db"
    private val dbFile: File by lazy {
        val file = context.getDatabasePath(dbName)
        if (!file.exists()) {
            copyDatabase(file)
        }
        file
    }

    private fun copyDatabase(file: File) {
        file.parentFile?.mkdirs()
        context.assets.open("chess/$dbName").use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun getDb(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    override suspend fun getOpeningByFen(fen: String): OpeningPosition? = withContext(Dispatchers.IO) {
        val db = getDb()
        val cursor = db.query(
            "openings",
            arrayOf("eco", "name", "epd"),
            "epd LIKE ?",
            arrayOf("${fen.substringBefore(" ")}%"),
            null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                OpeningPosition(
                    eco = it.getString(0),
                    name = it.getString(1),
                    fen = it.getString(2)
                )
            } else null
        }
    }

    override suspend fun getOpeningByEco(eco: String): OpeningPosition? = withContext(Dispatchers.IO) {
        val db = getDb()
        val cursor = db.query(
            "openings",
            arrayOf("eco", "name", "epd"),
            "eco = ?",
            arrayOf(eco),
            null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                OpeningPosition(
                    eco = it.getString(0),
                    name = it.getString(1),
                    fen = it.getString(2)
                )
            } else null
        }
    }

    override suspend fun searchOpenings(query: String): List<OpeningPosition> = withContext(Dispatchers.IO) {
        val db = getDb()
        val cursor = db.query(
            "openings",
            arrayOf("eco", "name", "epd"),
            "name LIKE ? OR eco LIKE ?",
            arrayOf("%$query%", "%$query%"),
            null, null, "eco ASC", "50"
        )
        val list = mutableListOf<OpeningPosition>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    OpeningPosition(
                        eco = it.getString(0),
                        name = it.getString(1),
                        fen = it.getString(2)
                    )
                )
            }
        }
        list
    }
}
