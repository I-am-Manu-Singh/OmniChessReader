package com.creativeblisslabs.omnimind.chess.engine

class DesktopChessEngine : ChessEngine {
    private var skill = 20
    private var currentFen = ""

    override fun start() {}
    override fun stop() {}
    override fun setPosition(fen: String) {
        currentFen = fen
    }
    
    override fun setSkillLevel(level: Int) {
        skill = level
    }

    override fun analyze(depth: Int, onResult: (EngineEvaluation) -> Unit) {
        val multiPV = listOf(
            MultiPVLine(0.35, listOf("e2e4", "e7e5", "g1f3")),
            MultiPVLine(0.28, listOf("d2d4", "d7d5", "c2c4")),
            MultiPVLine(0.15, listOf("g1f3", "d7d5", "d2d4"))
        )
        onResult(EngineEvaluation(0.35, listOf("e2e4", "e7e5", "g1f3"), depth, multiPV = multiPV))
    }

    override fun getBestMove(depth: Int, skillLevel: Int, onMove: (String) -> Unit) {
        setSkillLevel(skillLevel)
        onMove("e2e4")
    }
}

