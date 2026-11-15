package com.creativeblisslabs.omnichessreader.feature.reader.chess

import com.creativeblisslabs.omnichessreader.core.chess.ChessLogic

object LivingChessPlacement {

    /**
     * Builds a FEN placement field from 64 square labels (a8 to h1). '.' represents an empty square.
     */
    fun placementFromLabels(labels: List<Char>): String {
        val ranks = mutableListOf<String>()
        for (rank in 0 until 8) {
            val sb = StringBuilder()
            var empty = 0
            for (file in 0 until 8) {
                val label = labels.getOrElse(rank * 8 + file) { '.' }
                if (label == '.') {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(label)
                }
            }
            if (empty > 0) sb.append(empty)
            ranks.add(sb.toString())
        }
        return ranks.joinToString("/")
    }

    /**
     * Converts a FEN placement string into 64 square labels (a8 to h1).
     */
    fun labelsFromPlacement(placement: String): List<Char> {
        val labels = mutableListOf<Char>()
        val ranks = placement.split("/")
        for (rank in 0 until 8) {
            val rankStr = ranks.getOrNull(rank) ?: ""
            val squares = mutableListOf<Char>()
            for (char in rankStr) {
                if (char.isDigit()) {
                    val count = char.digitToInt()
                    repeat(count) { squares.add('.') }
                } else {
                    squares.add(char)
                }
            }
            for (file in 0 until 8) {
                labels.add(squares.getOrElse(file) { '.' })
            }
        }
        return labels
    }

    /**
     * Lists the reasons why a placement cannot come from a valid legal chess game or study.
     */
    fun checkPlacement(placement: String): List<String> {
        val issues = mutableListOf<String>()
        val shape = placement.split("/")
        if (shape.size != 8) {
            issues.add("A position needs 8 ranks")
            return issues
        }

        shape.forEachIndexed { index, rank ->
            val squares = rank.fold(0) { sum, char ->
                sum + if (char.isDigit()) char.digitToInt() else 1
            }
            if (squares != 8) {
                issues.add("Rank ${8 - index} does not have 8 squares")
            }
        }

        val whiteKings = placement.count { it == 'K' }
        val blackKings = placement.count { it == 'k' }
        val whitePawns = placement.count { it == 'P' }
        val blackPawns = placement.count { it == 'p' }

        if (whiteKings != 1) issues.add("White king count is $whiteKings (expected 1)")
        if (blackKings != 1) issues.add("Black king count is $blackKings (expected 1)")
        if (whitePawns > 8) issues.add("More than 8 white pawns ($whitePawns)")
        if (blackPawns > 8) issues.add("More than 8 black pawns ($blackPawns)")

        val topRank = shape.firstOrNull() ?: ""
        val bottomRank = shape.lastOrNull() ?: ""
        if (topRank.any { it == 'P' || it == 'p' } || bottomRank.any { it == 'P' || it == 'p' }) {
            issues.add("Pawn found on the first or eighth rank")
        }

        return issues
    }

    private fun pieceAt(rankStr: String, fileIdx: Int): Char? {
        var idx = 0
        for (char in rankStr) {
            if (char.isDigit()) {
                idx += char.digitToInt()
            } else {
                if (idx == fileIdx) return char
                idx++
            }
        }
        return null
    }

    /**
     * Infers castling rights from piece placement when generating full FEN from a diagram.
     */
    fun fenFromDiagram(placement: String, sideToMove: ChessColor): String {
        val ranks = placement.split("/")
        val blackRank = ranks.getOrElse(0) { "" }
        val whiteRank = ranks.getOrElse(7) { "" }

        val rights = StringBuilder()
        if (pieceAt(whiteRank, 4) == 'K') {
            if (pieceAt(whiteRank, 7) == 'R') rights.append('K')
            if (pieceAt(whiteRank, 0) == 'R') rights.append('Q')
        }
        if (pieceAt(blackRank, 4) == 'k') {
            if (pieceAt(blackRank, 7) == 'r') rights.append('k')
            if (pieceAt(blackRank, 0) == 'r') rights.append('q')
        }

        val castling = if (rights.isNotEmpty()) rights.toString() else "-"
        return "$placement ${sideToMove.symbol} $castling - 0 1"
    }
}

