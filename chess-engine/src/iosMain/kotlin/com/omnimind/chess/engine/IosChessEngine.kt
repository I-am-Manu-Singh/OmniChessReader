package com.creativeblisslabs.omnimind.chess.engine

class IosChessEngine : ChessEngine {
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
        onResult(EngineEvaluation(0.4, listOf("d2d4", "d7d5", "g1f3"), depth))
    }

    override fun getBestMove(depth: Int, skillLevel: Int, onMove: (String) -> Unit) {
        setSkillLevel(skillLevel)
        onMove("d2d4")
    }
}

