package com.creativeblisslabs.omnimind.core.chess

enum class EngineVersion(val label: String, val assetName: String, val sizeMb: Int) {
    SF17("Stockfish 17.1", "chess/stockfish17", 79)
}

enum class MoveQuality {
    BRILLIANT, GREAT, BEST, EXCELLENT, GOOD, BOOK, INACCURACY, MISTAKE, BLUNDER, FORCED
}

data class MoveAnalysis(
    val move: String,
    val quality: MoveQuality,
    val evaluationChange: Double,
    val bestMove: String? = null
)

interface ChessEngine {
    fun start()
    fun stop()
    fun setPosition(fen: String)
    fun analyze(depth: Int, onResult: (EngineEvaluation) -> Unit)
    fun getBestMove(depth: Int, skillLevel: Int, onMove: (String) -> Unit)
    fun setSkillLevel(level: Int)
    fun switchVersion(version: EngineVersion) {}
}

data class EngineEvaluation(
    val score: Double, // + for white, - for black
    val bestLine: List<String>,
    val depth: Int,
    val mate: Int? = null,
    val multiPV: List<MultiPVLine> = emptyList()
)

data class MultiPVLine(
    val score: Double,
    val bestLine: List<String>,
    val mate: Int? = null
)
