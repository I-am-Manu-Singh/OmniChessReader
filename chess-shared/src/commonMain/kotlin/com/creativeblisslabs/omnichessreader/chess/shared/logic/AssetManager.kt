package com.creativeblisslabs.omnichessreader.chess.shared.logic

import com.creativeblisslabs.omnichessreader.chess.shared.OpeningCategory
import com.creativeblisslabs.omnichessreader.chess.shared.EndgamePosition
import com.creativeblisslabs.omnichessreader.chess.shared.ChessPuzzle
import com.creativeblisslabs.omnichessreader.chess.shared.LessonTopic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

expect class AssetManager {
    suspend fun readTextFile(path: String): String
}

class ChessAssetLoader(private val assetManager: AssetManager) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private var cachedPositions: List<OpeningCategory>? = null
    private var cachedEndgames: List<EndgamePosition>? = null
    private var cachedPuzzles: List<ChessPuzzle>? = null
    private var cachedMiddlegames: List<LessonTopic>? = null
    private var cachedEcoOpenings: List<EcoOpening>? = null

    suspend fun loadPositions(): List<OpeningCategory> = withContext(Dispatchers.Default) {
        cachedPositions?.let { return@withContext it }
        try {
            val content = assetManager.readTextFile("chess/positions.json")
            val decoded = json.decodeFromString<List<OpeningCategory>>(content)
            cachedPositions = decoded
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadEndgames(): List<EndgamePosition> = withContext(Dispatchers.Default) {
        cachedEndgames?.let { return@withContext it }
        try {
            val content = assetManager.readTextFile("chess/endgames.json")
            val decoded = json.decodeFromString<List<EndgamePosition>>(content)
            cachedEndgames = decoded
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadPuzzles500(): List<ChessPuzzle> = withContext(Dispatchers.Default) {
        cachedPuzzles?.let { return@withContext it }
        try {
            val content = assetManager.readTextFile("chess/puzzles_500.json")
            val decoded = json.decodeFromString<List<ChessPuzzle>>(content)
            cachedPuzzles = decoded
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadMiddlegames(): List<LessonTopic> = withContext(Dispatchers.Default) {
        cachedMiddlegames?.let { return@withContext it }
        try {
            val content = assetManager.readTextFile("chess/middlegames.json")
            val decoded = json.decodeFromString<List<LessonTopic>>(content)
            cachedMiddlegames = decoded
            decoded
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadEcoOpenings(): List<EcoOpening> = withContext(Dispatchers.Default) {
        cachedEcoOpenings?.let { return@withContext it }
        val allOpenings = mutableListOf<EcoOpening>()
        listOf("a.tsv", "b.tsv", "c.tsv", "d.tsv", "e.tsv").forEach { fileName ->
            try {
                val content = assetManager.readTextFile("chess/$fileName")
                allOpenings.addAll(OpeningTsvParser.parseTsvContent(content))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        cachedEcoOpenings = allOpenings
        allOpenings
    }
}
