package com.creativeblisslabs.omnimind.chess.shared.logic

import com.creativeblisslabs.omnimind.core.chess.ChessLogic

data class EcoOpening(
    val eco: String,
    val name: String,
    val pgn: String,
    val fen: String = ""
)

object OpeningTsvParser {

    /** Parses TSV content lines formatted as: eco \t name \t pgn */
    fun parseTsvContent(tsvContent: String): List<EcoOpening> {
        val results = mutableListOf<EcoOpening>()
        val lines = tsvContent.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("eco")) continue
            val parts = trimmed.split("\t")
            if (parts.size >= 3) {
                val eco = parts[0].trim()
                val name = parts[1].trim()
                val pgn = parts[2].trim()
                val fen = computeFenFromPgn(pgn)
                results.add(EcoOpening(eco, name, pgn, fen))
            }
        }
        return results
    }

    /** Computes resultant position FEN by playing PGN moves from initial FEN */
    fun computeFenFromPgn(pgn: String): String {
        var currentFen = ChessLogic.getInitialFen()
        val moves = pgn.split(" ").filter { it.isNotBlank() && !it.contains(".") }
        for (san in moves) {
            val moveTriple = ChessLogic.sanToMove(currentFen, san) ?: break
            val (from, to, promo) = moveTriple
            try {
                currentFen = ChessLogic.applyMove(currentFen, from, to, promo)
            } catch (_: Exception) {
                break
            }
        }
        return currentFen
    }

    /** Finds closest matching ECO opening by position FEN */
    fun findOpeningByFen(openings: List<EcoOpening>, fen: String): EcoOpening? {
        val targetBoard = fen.split(" ").firstOrNull() ?: return null
        return openings.firstOrNull { op ->
            op.fen.split(" ").firstOrNull() == targetBoard
        }
    }
}
