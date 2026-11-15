package com.creativeblisslabs.omnichessreader.feature.reader.chess

import com.creativeblisslabs.omnichessreader.core.chess.ChessLogic

data class EngineScore(val kind: String, val value: Int) // kind: "cp" or "mate"

data class EngineLineInfo(
    val rank: Int,
    val depth: Int,
    val score: EngineScore,
    val pvMoves: List<String>
)

data class EvaluatedLineMove(
    val san: String,
    val fen: String,
    val from: String,
    val to: String,
    val uci: String
)

data class EvaluatedEngineLine(
    val rank: Int,
    val depth: Int,
    val score: EngineScore,
    val moves: List<EvaluatedLineMove>
)

enum class DiagramPlayability {
    PLAYABLE,
    OVER,
    IMPOSSIBLE
}

object LivingChessAnalysis {

    /**
     * Formats engine score as pawn advantage (e.g. "+1.25", "-0.80", "#-2").
     */
    fun formatScore(score: EngineScore): String {
        return if (score.kind == "mate") {
            "#${score.value}"
        } else {
            val pawns = score.value / 100.0
            "${if (pawns > 0) "+" else ""}${((pawns * 100).toInt() / 100.0)}"
        }
    }

    /**
     * Checks if a detected position is legal, playable, or physically impossible.
     */
    fun checkPlayability(fen: String): DiagramPlayability {
        return try {
            val state = ChessLogic.parseFen(fen)
            val pieces = state.pieces
            val whiteKing = pieces.entries.find { it.value == 'K' }?.key
            val blackKing = pieces.entries.find { it.value == 'k' }?.key

            if (whiteKing == null || blackKing == null) return DiagramPlayability.IMPOSSIBLE

            // King cannot be already in check if it's the other player's turn to move
            val movingColorWhite = state.isWhiteTurn
            val otherColorInCheck = ChessLogic.isKingInCheck(fen, !movingColorWhite)
            if (otherColorInCheck) return DiagramPlayability.IMPOSSIBLE

            // Pawns cannot be on 1st or 8th rank
            for ((sq, p) in pieces) {
                if (p.lowercaseChar() == 'p') {
                    val rank = sq[1]
                    if (rank == '1' || rank == '8') return DiagramPlayability.IMPOSSIBLE
                }
            }

            val whitePieces = pieces.values.filter { it.isUpperCase() }
            val blackPieces = pieces.values.filter { !it.isUpperCase() }
            if (whitePieces.size > 16 || blackPieces.size > 16) return DiagramPlayability.IMPOSSIBLE
            if (whitePieces.count { it == 'P' } > 8 || blackPieces.count { it == 'p' } > 8) return DiagramPlayability.IMPOSSIBLE

            val legalMoves = ChessLogic.generateMoves(fen)
            if (legalMoves.isEmpty()) DiagramPlayability.OVER else DiagramPlayability.PLAYABLE
        } catch (_: Exception) {
            DiagramPlayability.IMPOSSIBLE
        }
    }

    /**
     * Evaluates a chess position from a diagram, finding candidate moves and evaluations.
     */
    fun evaluatePosition(fen: String, depth: Int = 4): EvaluatedEngineLine {
        val state = ChessLogic.parseFen(fen)
        val playability = checkPlayability(fen)

        if (playability != DiagramPlayability.PLAYABLE) {
            return EvaluatedEngineLine(
                rank = 1,
                depth = 0,
                score = EngineScore("cp", 0),
                moves = emptyList()
            )
        }

        val evalResult = ChessLogic.minimax(fen, depth, -1000000.0, 1000000.0, state.isWhiteTurn)
        val bestMove = evalResult.second
        val scoreCp = (evalResult.first * 100).toInt()

        val evaluatedMoves = mutableListOf<EvaluatedLineMove>()
        if (bestMove != null && bestMove.length >= 4) {
            val from = bestMove.substring(0, 2)
            val to = bestMove.substring(2, 4)
            val promo = if (bestMove.length > 4) bestMove[4] else null
            val san = ChessLogic.moveToSan(fen, from, to, promo)
            val nextFen = ChessLogic.applyMove(fen, from, to, promo)
            evaluatedMoves.add(
                EvaluatedLineMove(
                    san = san,
                    fen = nextFen,
                    from = from,
                    to = to,
                    uci = bestMove
                )
            )
        }

        return EvaluatedEngineLine(
            rank = 1,
            depth = depth,
            score = EngineScore("cp", scoreCp),
            moves = evaluatedMoves
        )
    }

    /**
     * Analyzes an entire book PDF overlay, evaluating all detected diagrams and organizing study positions.
     */
    fun analyzeOverlayDiagrams(
        overlay: LivingBookOverlay,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): Map<String, EvaluatedEngineLine> {
        val results = mutableMapOf<String, EvaluatedEngineLine>()
        val total = overlay.diagrams.size

        overlay.diagrams.forEachIndexed { index, diagram ->
            if (diagram.isValid) {
                val side = ChessColor.fromChar(diagram.sideToMove?.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                val fen = LivingChessPlacement.fenFromDiagram(diagram.placement, side)
                val line = evaluatePosition(fen)
                results[diagram.id] = line
            }
            onProgress?.invoke(index + 1, total)
        }

        return results
    }
}

