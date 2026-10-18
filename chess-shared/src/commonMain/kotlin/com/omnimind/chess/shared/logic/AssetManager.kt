package com.creativeblisslabs.omnimind.chess.shared.logic

import com.creativeblisslabs.omnimind.chess.shared.OpeningCategory
import com.creativeblisslabs.omnimind.chess.shared.EndgamePosition
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

    suspend fun loadPositions(): List<OpeningCategory> = withContext(Dispatchers.Default) {
        try {
            val content = assetManager.readTextFile("chess/positions.json")
            json.decodeFromString<List<OpeningCategory>>(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadEndgames(): List<EndgamePosition> = withContext(Dispatchers.Default) {
        try {
            val content = assetManager.readTextFile("chess/endgames.json")
            json.decodeFromString<List<EndgamePosition>>(content)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadEcoOpenings(): List<EcoOpening> = withContext(Dispatchers.Default) {
        val allOpenings = mutableListOf<EcoOpening>()
        listOf("a.tsv", "b.tsv", "c.tsv", "d.tsv", "e.tsv").forEach { fileName ->
            try {
                val content = assetManager.readTextFile("chess/$fileName")
                allOpenings.addAll(OpeningTsvParser.parseTsvContent(content))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        allOpenings
    }
}
