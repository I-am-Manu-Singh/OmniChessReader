package com.creativeblisslabs.omnimind.core.chess

import com.creativeblisslabs.omnimind.core.chess.MoveQuality
import com.creativeblisslabs.omnimind.core.chess.GameVariant
import kotlin.math.abs

object ChessLogic {

    fun getInitialFen() = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    /**
     * Professional DGT Tournament Chess Etiquette:
     * When a game ends, kings are placed in the center of the board to signal the result:
     * - White Wins (1-0): Both Kings on White/Light central squares (d5 & e4).
     * - Black Wins (0-1): Both Kings on Black/Dark central squares (d4 & e5).
     * - Draw / Stalemate (1/2-1/2): Kings on differing colored central squares (d5 & e5 or e4 & e5).
     */
    fun getGameEndKingsFen(reasonOrResult: String): String {
        val lower = reasonOrResult.lowercase()
        return when {
            lower.contains("white wins") || lower.contains("1-0") || (lower.contains("white") && !lower.contains("black")) -> {
                // Both Kings on Light/White central squares (d5=light, e4=light)
                "8/8/8/3k4/4K3/8/8/8 w - - 0 1"
            }
            lower.contains("black wins") || lower.contains("0-1") || (lower.contains("black") && !lower.contains("white")) -> {
                // Both Kings on Dark/Black central squares (d4=dark, e5=dark)
                "8/8/8/4k3/3K4/8/8/8 w - - 0 1"
            }
            else -> {
                // Draw / Stalemate: Kings on differing colored central squares (e4=light, e5=dark)
                "8/8/8/4k3/4K3/8/8/8 w - - 0 1"
            }
        }
    }

    fun determineMoveQuality(
        scoreBefore: Double,
        scoreAfter: Double,
        isBestMove: Boolean,
        mateBefore: Int?,
        mateAfter: Int?
    ): MoveQuality {
        val diff = scoreAfter - scoreBefore
        return when {
            isBestMove && diff >= 0.4 -> MoveQuality.BRILLIANT
            isBestMove && diff >= 0.15 -> MoveQuality.GREAT
            isBestMove -> MoveQuality.BEST
            diff > -0.25 -> MoveQuality.EXCELLENT
            diff > -0.55 -> MoveQuality.GOOD
            diff > -1.10 -> MoveQuality.INACCURACY
            diff > -2.20 -> MoveQuality.MISTAKE
            else -> MoveQuality.BLUNDER
        }
    }

    // A lightweight representation of a chess board
    data class BoardState(
        val pieces: Map<String, Char>, // e.g. "e4" -> 'P', "e8" -> 'k'
        val isWhiteTurn: Boolean,
        val castlingRights: String, // e.g. "KQkq"
        val enPassantSquare: String?, // e.g. "e3"
        val halfmoveClock: Int,
        val fullmoveNumber: Int
    )

    fun parseFen(fen: String): BoardState {
        val parts = fen.split(" ")
        val boardPart = parts.getOrNull(0) ?: ""
        val activeColor = parts.getOrNull(1) ?: "w"
        val castling = parts.getOrNull(2) ?: "-"
        val ep = parts.getOrNull(3) ?: "-"
        val halfmove = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val fullmove = parts.getOrNull(5)?.toIntOrNull() ?: 1

        val pieces = mutableMapOf<String, Char>()
        val rows = boardPart.split("/")
        for (r in 0 until 8) {
            val rowStr = rows.getOrNull(r) ?: ""
            var c = 0
            for (char in rowStr) {
                if (char.isDigit()) {
                    c += char.toString().toInt()
                } else {
                    val file = ('a' + c).toString()
                    val rank = (8 - r).toString()
                    pieces[file + rank] = char
                    c++
                }
            }
        }

        return BoardState(
            pieces = pieces,
            isWhiteTurn = activeColor == "w",
            castlingRights = castling,
            enPassantSquare = if (ep == "-") null else ep,
            halfmoveClock = halfmove,
            fullmoveNumber = fullmove
        )
    }

    fun toFen(state: BoardState): String {
        val sb = StringBuilder()
        for (r in 0 until 8) {
            val rank = 8 - r
            var emptyCount = 0
            for (c in 0 until 8) {
                val file = ('a' + c).toString()
                val sq = file + rank
                val p = state.pieces[sq]
                if (p == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(p)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (r < 7) sb.append("/")
        }
        sb.append(" ").append(if (state.isWhiteTurn) "w" else "b")
        sb.append(" ").append(state.castlingRights)
        sb.append(" ").append(state.enPassantSquare ?: "-")
        sb.append(" ").append(state.halfmoveClock)
        sb.append(" ").append(state.fullmoveNumber)
        return sb.toString()
    }

    fun getValidDestinations(fen: String, from: String, variant: GameVariant = GameVariant.CLASSIC): List<String> {
        val destinations = mutableListOf<String>()
        val state = parseFen(fen)
        if (!state.pieces.containsKey(from)) return emptyList()
        for (f in 0 until 8) {
            val file = ('a' + f).toString()
            for (r in 1..8) {
                val to = "$file$r"
                if (isValidMove(fen, from, to, variant)) {
                    destinations.add(to)
                }
            }
        }
        return destinations
    }

    // Validates if a move coordinate transition is legal under basic piece movement rules
    fun isValidMove(fen: String, from: String, to: String, variant: GameVariant = GameVariant.CLASSIC): Boolean {
        if (variant == GameVariant.FREESTYLE) {
            val state = parseFen(fen)
            return state.pieces.containsKey(from) && from != to
        }
        if (from == to) return false
        val state = parseFen(fen)
        val piece = state.pieces[from] ?: return false

        // Check turn matching
        if (state.isWhiteTurn && !piece.isUpperCase()) return false
        if (!state.isWhiteTurn && piece.isUpperCase()) return false

        val targetPiece = state.pieces[to]
        if (targetPiece != null) {
            // Cannot capture own piece
            if (piece.isUpperCase() == targetPiece.isUpperCase()) return false
            // FIX: Kings can NEVER be captured in standard chess!
            if (targetPiece.lowercaseChar() == 'k') return false
        }

        val fromFile = from[0] - 'a'
        val fromRank = from[1] - '1'
        val toFile = to[0] - 'a'
        val toRank = to[1] - '1'

        val df = abs(toFile - fromFile)
        val dr = abs(toRank - fromRank)

        val basicValid = when (piece.lowercaseChar()) {
            'p' -> {
                val direction = if (piece.isUpperCase()) 1 else -1
                val startRank = if (piece.isUpperCase()) 1 else 6

                if (fromFile == toFile) {
                    // Forward move
                    if (toRank == fromRank + direction) {
                        targetPiece == null
                    } else if (fromRank == startRank && toRank == fromRank + 2 * direction) {
                        val intermediateSq = "${from[0]}${fromRank + 1 + direction}"
                        targetPiece == null && state.pieces[intermediateSq] == null
                    } else {
                        false
                    }
                } else if (df == 1 && toRank == fromRank + direction) {
                    // Capture or En Passant
                    targetPiece != null || to == state.enPassantSquare
                } else {
                    false
                }
            }

            'r' -> {
                (df == 0 || dr == 0) && isPathClear(
                    state.pieces,
                    fromFile,
                    fromRank,
                    toFile,
                    toRank
                )
            }

            'n' -> {
                (df == 1 && dr == 2) || (df == 2 && dr == 1)
            }

            'b' -> {
                (df == dr) && isPathClear(state.pieces, fromFile, fromRank, toFile, toRank)
            }

            'q' -> {
                (df == dr || df == 0 || dr == 0) && isPathClear(
                    state.pieces,
                    fromFile,
                    fromRank,
                    toFile,
                    toRank
                )
            }

            'k' -> {
                if (df <= 1 && dr <= 1) {
                    true
                } else if (dr == 0 && df == 2) {
                    val isWhite = piece.isUpperCase()
                    // 1. Cannot castle if in check
                    if (isKingInCheck(fen, isWhite)) return false
                    
                    if (isWhite && from == "e1") {
                        if (to == "g1" && state.castlingRights.contains('K')) {
                            // Kingside: f1 and g1 must be empty
                            if (state.pieces["f1"] != null || state.pieces["g1"] != null) return false
                            // Path (f1) must not be under attack
                            val f1Fen = applyMove(fen, "e1", "f1")
                            if (isKingInCheck(f1Fen, true)) return false
                            return true
                        }
                        if (to == "c1" && state.castlingRights.contains('Q')) {
                            // Queenside: d1, c1, b1 must be empty
                            if (state.pieces["d1"] != null || state.pieces["c1"] != null || state.pieces["b1"] != null) return false
                            // Path (d1) must not be under attack
                            val d1Fen = applyMove(fen, "e1", "d1")
                            if (isKingInCheck(d1Fen, true)) return false
                            return true
                        }
                    } else if (!isWhite && from == "e8") {
                        if (to == "g8" && state.castlingRights.contains('k')) {
                            if (state.pieces["f8"] != null || state.pieces["g8"] != null) return false
                            val f8Fen = applyMove(fen, "e8", "f8")
                            if (isKingInCheck(f8Fen, false)) return false
                            return true
                        }
                        if (to == "c8" && state.castlingRights.contains('q')) {
                            if (state.pieces["d8"] != null || state.pieces["c8"] != null || state.pieces["b8"] != null) return false
                            val d8Fen = applyMove(fen, "e8", "d8")
                            if (isKingInCheck(d8Fen, false)) return false
                            return true
                        }
                    }
                    false
                } else {
                    false
                }
            }

            else -> false
        }

        if (!basicValid) return false

        // Check if move leaves own king in check
        val nextFen = applyMove(fen, from, to, null)
        return !isKingInCheck(nextFen, piece.isUpperCase())
    }

    fun generateChess960Fen(): String {
        val squares = Array<String?>(8) { null }
        val random = kotlin.random.Random.Default
        val b1 = random.nextInt(4) * 2
        val b2 = random.nextInt(4) * 2 + 1
        squares[b1] = "B"
        squares[b2] = "B"
        val remaining1 = (0..7).filter { squares[it] == null }
        squares[remaining1[random.nextInt(remaining1.size)]] = "Q"
        val remaining2 = (0..7).filter { squares[it] == null }
        squares[remaining2[random.nextInt(remaining2.size)]] = "N"
        val remaining3 = (0..7).filter { squares[it] == null }
        squares[remaining3[random.nextInt(remaining3.size)]] = "N"
        val remainingLast = (0..7).filter { squares[it] == null }
        squares[remainingLast[0]] = "R"
        squares[remainingLast[1]] = "K"
        squares[remainingLast[2]] = "R"
        val backRank = squares.filterNotNull().joinToString("")
        val whiteRank = backRank.uppercase()
        val blackRank = backRank.lowercase()
        return "$blackRank/pppppppp/8/8/8/8/PPPPPPPP/$whiteRank w KQkq - 0 1"
    }

    fun isPromotionMove(fen: String, from: String, to: String): Boolean {
        val state = parseFen(fen)
        val piece = state.pieces[from] ?: return false
        val toRank = to[1] - '1'
        return (piece == 'P' && toRank == 7) || (piece == 'p' && toRank == 0)
    }

    fun isKingInCheck(fen: String, isWhite: Boolean): Boolean {
        val state = parseFen(fen)
        val kingChar = if (isWhite) 'K' else 'k'
        val kingSquare = state.pieces.entries.find { it.value == kingChar }?.key ?: return false

        // Check all opponent pieces
        for ((square, piece) in state.pieces) {
            if (piece.isUpperCase() != isWhite) {
                if (canPieceAttack(state, square, kingSquare)) return true
            }
        }
        return false
    }

    private fun canPieceAttack(state: BoardState, from: String, to: String): Boolean {
        val piece = state.pieces[from] ?: return false
        val fromFile = from[0] - 'a'
        val fromRank = from[1] - '1'
        val toFile = to[0] - 'a'
        val toRank = to[1] - '1'
        val df = abs(toFile - fromFile)
        val dr = abs(toRank - fromRank)

        return when (piece.lowercaseChar()) {
            'p' -> {
                val direction = if (piece.isUpperCase()) 1 else -1
                df == 1 && toRank == fromRank + direction
            }
            'r' -> (df == 0 || dr == 0) && isPathClear(state.pieces, fromFile, fromRank, toFile, toRank)
            'n' -> (df == 1 && dr == 2) || (df == 2 && dr == 1)
            'b' -> (df == dr) && isPathClear(state.pieces, fromFile, fromRank, toFile, toRank)
            'q' -> (df == dr || df == 0 || dr == 0) && isPathClear(state.pieces, fromFile, fromRank, toFile, toRank)
            'k' -> df <= 1 && dr <= 1
            else -> false
        }
    }

    private fun isPathClear(
        pieces: Map<String, Char>,
        f1: Int,
        r1: Int,
        f2: Int,
        r2: Int
    ): Boolean {
        val stepFile = if (f2 == f1) 0 else if (f2 > f1) 1 else -1
        val stepRank = if (r2 == r1) 0 else if (r2 > r1) 1 else -1

        var currentFile = f1 + stepFile
        var currentRank = r1 + stepRank

        while (currentFile != f2 || currentRank != r2) {
            val sq = "${('a' + currentFile)}${currentRank + 1}"
            if (pieces.containsKey(sq)) return false
            currentFile += stepFile
            currentRank += stepRank
        }
        return true
    }

    // Applies a move to the board, implementing Autoqueen promotion if promotionPiece is null
    fun applyMove(fen: String, from: String, to: String, promotionPiece: Char? = null): String {
        val state = parseFen(fen)
        val mutablePieces = state.pieces.toMutableMap()
        val movingPiece = mutablePieces.remove(from) ?: return fen

        val fromRank = from[1] - '1'
        val toRank = to[1] - '1'

        // Determine target piece (Handle explicit or Autoqueen promotion)
        val targetPiece = if ((movingPiece == 'P' && toRank == 7) || (movingPiece == 'p' && toRank == 0)) {
            if (promotionPiece != null) {
                if (state.isWhiteTurn) promotionPiece.uppercaseChar() else promotionPiece.lowercaseChar()
            } else {
                if (state.isWhiteTurn) 'Q' else 'q'
            }
        } else {
            movingPiece
        }

        // En Passant capture pawn removal
        if ((movingPiece == 'P' || movingPiece == 'p') && to == state.enPassantSquare) {
            val capturedPawnSquare = "${to[0]}${from[1]}"
            mutablePieces.remove(capturedPawnSquare)
        }

        // Castling Rook move execution
        if (movingPiece == 'K' && from == "e1" && to == "g1") {
            mutablePieces.remove("h1")
            mutablePieces["f1"] = 'R'
        } else if (movingPiece == 'K' && from == "e1" && to == "c1") {
            mutablePieces.remove("a1")
            mutablePieces["d1"] = 'R'
        } else if (movingPiece == 'k' && from == "e8" && to == "g8") {
            mutablePieces.remove("h8")
            mutablePieces["f8"] = 'r'
        } else if (movingPiece == 'k' && from == "e8" && to == "c8") {
            mutablePieces.remove("a8")
            mutablePieces["d8"] = 'r'
        }

        // Place the moving or promoted piece onto the destination
        val isCapture = state.pieces.containsKey(to)
        mutablePieces[to] = targetPiece

        // Set en passant target square for 2-square pawn pushes
        val nextEp = if (movingPiece == 'P' && from[1] == '2' && to[1] == '4') {
            "${from[0]}3"
        } else if (movingPiece == 'p' && from[1] == '7' && to[1] == '5') {
            "${from[0]}6"
        } else {
            null
        }

        // Update castling rights
        var newCastling = state.castlingRights
        if (movingPiece == 'K') newCastling = newCastling.replace("K", "").replace("Q", "")
        if (movingPiece == 'k') newCastling = newCastling.replace("k", "").replace("q", "")
        if (from == "a1" || to == "a1") newCastling = newCastling.replace("Q", "")
        if (from == "h1" || to == "h1") newCastling = newCastling.replace("K", "")
        if (from == "a8" || to == "a8") newCastling = newCastling.replace("q", "")
        if (from == "h8" || to == "h8") newCastling = newCastling.replace("k", "")
        if (newCastling.isEmpty()) newCastling = "-"

        // Meta rules for fullmove and halfmove tracking
        val nextTurn = !state.isWhiteTurn
        val newFullmove = if (state.isWhiteTurn) state.fullmoveNumber else state.fullmoveNumber + 1
        val newHalfmove = if (movingPiece.lowercaseChar() == 'p' || isCapture) 0 else state.halfmoveClock + 1

        return toFen(
            BoardState(
                mutablePieces,
                nextTurn,
                newCastling,
                nextEp,
                newHalfmove,
                newFullmove
            )
        )
    }


    // Looks up Lichess Opening Database names and ECO codes
        fun getOpeningName(moves: List<String>): String {
            if (moves.isEmpty()) return "Initial Position (A00)"
            val moveStr = moves.joinToString(" ")
            return when {
                moveStr.startsWith("e2e4 e7e5 g1f3 b8c6 f1b5") -> "Ruy Lopez (C60)"
                moveStr.startsWith("e2e4 e7e5 g1f3 b8c6 f1c4") -> "Italian Game (C50)"
                moveStr.startsWith("e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6") -> "Sicilian Defence: Najdorf Variation (B90)"
                moveStr.startsWith("e2e4 c7c5 g1f3 e7e6") -> "Sicilian Defence: French Variation (B40)"
                moveStr.startsWith("e2e4 c7c5") -> "Sicilian Defence (B20)"
                moveStr.startsWith("d2d4 d7d5 c2c4 c7c6") -> "Slav Defence (D10)"
                moveStr.startsWith("d2d4 d7d5 c2c4 e7e6") -> "Queen's Gambit Declined (D30)"
                moveStr.startsWith("d2d4 d7d5 c2c4") -> "Queen's Gambit (D06)"
                moveStr.startsWith("d2d4 g8f6 c2c4 g7g6 b1c3 f8g7") -> "King's Indian Defence (E60)"
                moveStr.startsWith("d2d4 g8f6 c2c4 e7e6 g2g3") -> "Catalan Opening (E00)"
                moveStr.startsWith("e2e4 e7e6 d2d4 d7d5") -> "French Defence (C00)"
                moveStr.startsWith("e2e4 c7c6 d2d4 d7d5") -> "Caro-Kann Defence (B10)"
                moveStr.startsWith("e2e4 e7e5 g1f3 g8f6") -> "Petrov's Defence (C42)"
                moveStr.startsWith("e2e4 e7e5 f2f4") -> "King's Gambit (C30)"
                moveStr.startsWith("e2e4 d7d5") -> "Scandinavian Defence (B01)"
                moveStr.startsWith("e2e4 g8f6") -> "Alekhine's Defence (B02)"
                moveStr.startsWith("c2c4") -> "English Opening (A10)"
                moveStr.startsWith("g1f3") -> "Réti Opening (A04)"
                moveStr.startsWith("f2f4") -> "Bird's Opening (A02)"
                moveStr.startsWith("b2b3") -> "Nimzo-Larsen Attack (A01)"
                moveStr.startsWith("e2e4") -> "King's Pawn Game (B00)"
                moveStr.startsWith("d2d4") -> "Queen's Pawn Game (D00)"
                else -> "Open Game (A00)"
            }
        }

        // Computes a map representing the control/attacks on the board
        // Returns value > 0 for white threats, < 0 for black threats
        fun computeThreatMap(fen: String): Map<String, Int> {
            val state = parseFen(fen)
            val threatMap = mutableMapOf<String, Int>()

            for (f in 0 until 8) {
                for (r in 1..8) {
                    val sq = "${('a' + f)}$r"
                    threatMap[sq] = 0
                }
            }

            for ((from, piece) in state.pieces) {
                for (f in 0 until 8) {
                    for (r in 1..8) {
                        val to = "${('a' + f)}$r"
                        if (isValidMove(fen, from, to)) {
                            val threat = if (piece.isUpperCase()) 1 else -1
                            threatMap[to] = (threatMap[to] ?: 0) + threat
                        }
                    }
                }
            }
            return threatMap
        }

        fun isGameOver(fen: String): GameOverResult {
        val state = parseFen(fen)
        val moves = generateMoves(fen)
        val inCheck = isKingInCheck(fen, state.isWhiteTurn)

        if (moves.isEmpty()) {
            if (inCheck) {
                return GameOverResult(true, if (state.isWhiteTurn) "Black wins by Checkmate" else "White wins by Checkmate")
            } else {
                return GameOverResult(true, "Draw by Stalemate")
            }
        }

        // Draw by 50-move rule
        if (state.halfmoveClock >= 100) {
            return GameOverResult(true, "Draw by 50-move rule")
        }

        // Check for insufficient material
        if (isInsufficientMaterial(state.pieces)) {
            return GameOverResult(true, "Draw by Insufficient Material")
        }

        return GameOverResult(false, "")
    }

    fun moveToSan(fen: String, from: String, to: String, promotion: Char? = null): String {
        val state = parseFen(fen)
        val piece = state.pieces[from] ?: return ""
        val isCapture = state.pieces.containsKey(to) || (piece.lowercaseChar() == 'p' && to == state.enPassantSquare)
        
        if (piece.lowercaseChar() == 'k' && abs(to[0] - from[0]) == 2) {
            return if (to[0] == 'g') "O-O" else "O-O-O"
        }

        val sb = StringBuilder()
        if (piece.lowercaseChar() != 'p') {
            sb.append(piece.uppercaseChar())
            
            // Disambiguation
            val others = state.pieces.filter { 
                it.value == piece && it.key != from && isValidMove(fen, it.key, to)
            }
            if (others.isNotEmpty()) {
                val sameFile = others.keys.any { it[0] == from[0] }
                val sameRank = others.keys.any { it[1] == from[1] }
                
                if (!sameFile) {
                    sb.append(from[0])
                } else if (!sameRank) {
                    sb.append(from[1])
                } else {
                    sb.append(from[0]).append(from[1])
                }
            }
        } else if (isCapture) {
            sb.append(from[0])
        }

        if (isCapture) sb.append('x')
        sb.append(to)
        
        if (promotion != null) {
            sb.append('=').append(promotion.uppercaseChar())
        }

        val nextFen = applyMove(fen, from, to, promotion)
        if (isKingInCheck(nextFen, !state.isWhiteTurn)) {
            val result = isGameOver(nextFen)
            if (result.isGameOver && result.reason.contains("Checkmate")) {
                sb.append('#')
            } else {
                sb.append('+')
            }
        }

        return sb.toString()
    }

    /**
     * Converts a sequence of UCI moves (e.g. ["e2e4", "e7e5", "g1f3"]) starting from a given FEN
     * into clean Standard Algebraic Notation (SAN) with move numbering (e.g. "1. e4 e5 2. Nf3").
     */
    fun convertUciLineToSan(startFen: String, uciMoves: List<String>): String {
        var currentFen = startFen
        val sb = StringBuilder()
        var prevWasBlack = false
        for (i in uciMoves.take(15).indices) {
            val uci = uciMoves[i]
            if (uci.length < 4) continue
            val from = uci.substring(0, 2)
            val to = uci.substring(2, 4)
            val promo = if (uci.length > 4) uci[4] else null
            val state = parseFen(currentFen)
            val moveNum = state.fullmoveNumber
            val isWhite = state.isWhiteTurn
            val san = moveToSan(currentFen, from, to, promo)

            if (isWhite) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append("$moveNum.")
            } else if (i == 0) {
                // Starting from a black move
                sb.append("$moveNum...")
            }
            sb.append(" ")

            if (san.isNotBlank()) {
                sb.append(san)
                try {
                    currentFen = applyMove(currentFen, from, to, promo)
                } catch (_: Exception) { break }
            } else {
                sb.append(uci)
            }
            prevWasBlack = !isWhite
        }
        return sb.toString().trim()
    }


    private fun isInsufficientMaterial(pieces: Map<String, Char>): Boolean {
        val activePieces = pieces.entries.filter { it.value.lowercaseChar() != 'k' }
        if (activePieces.isEmpty()) return true // King vs King
        
        if (activePieces.size == 1) {
            val p = activePieces[0].value.lowercaseChar()
            if (p == 'b' || p == 'n') return true // K+B or K+N
        }
        
        if (activePieces.size == 2) {
            val p1 = activePieces[0].value.lowercaseChar()
            val p2 = activePieces[1].value.lowercaseChar()
            val color1 = activePieces[0].value.isUpperCase()
            val color2 = activePieces[1].value.isUpperCase()
            
            // K+B vs K+B (same color bishops)
            if (p1 == 'b' && p2 == 'b' && color1 != color2) {
                val sq1 = activePieces[0].key
                val sq2 = activePieces[1].key
                val isLight1 = ((sq1[0] - 'a') + (sq1[1] - '1')) % 2 != 0
                val isLight2 = ((sq2[0] - 'a') + (sq2[1] - '1')) % 2 != 0
                if (isLight1 == isLight2) return true
            }
        }
        
        return false
    }

    data class GameOverResult(val isGameOver: Boolean, val reason: String)

    fun generateMoves(fen: String): List<String> {
        val state = parseFen(fen)
        val moves = mutableListOf<String>()
        val activePieces = state.pieces.filter { (_, piece) ->
            if (state.isWhiteTurn) piece.isUpperCase() else !piece.isUpperCase()
        }
        for ((from, _) in activePieces) {
            for (f in 0 until 8) {
                for (r in 1..8) {
                    val to = "${('a' + f)}$r"
                    if (isValidMove(fen, from, to)) {
                        if (isPromotionMove(fen, from, to)) {
                            moves.add("${from}${to}q")
                            moves.add("${from}${to}r")
                            moves.add("${from}${to}b")
                            moves.add("${from}${to}n")
                        } else {
                            moves.add("$from$to")
                        }
                    }
                }
            }
        }
        return moves
    }

    fun sanToMove(fen: String, san: String): Triple<String, String, Char?>? {
        val cleanSan = san.replace("+", "")
            .replace("#", "")
            .replace("!", "")
            .replace("?", "")
            .trim()
        if (cleanSan.isEmpty()) return null

        val state = parseFen(fen)
        val isWhite = state.isWhiteTurn

        // 1. Direct Castling Handling
        if (cleanSan == "O-O" || cleanSan == "0-0") {
            val from = if (isWhite) "e1" else "e8"
            val to = if (isWhite) "g1" else "g8"
            return Triple(from, to, null)
        }
        if (cleanSan == "O-O-O" || cleanSan == "0-0-0") {
            val from = if (isWhite) "e1" else "e8"
            val to = if (isWhite) "c1" else "c8"
            return Triple(from, to, null)
        }

        // 2. Promotion detection (e.g. e8=Q, e8Q, exd8=Q)
        var promoChar: Char? = null
        var moveCore = cleanSan
        if (cleanSan.contains("=")) {
            val parts = cleanSan.split("=")
            moveCore = parts[0]
            promoChar = parts.getOrNull(1)?.firstOrNull()?.lowercaseChar()
        } else if (cleanSan.length >= 3 && cleanSan.last() in "QRBNqrbn" && cleanSan[cleanSan.length - 2] in '1'..'8') {
            promoChar = cleanSan.last().lowercaseChar()
            moveCore = cleanSan.dropLast(1)
        }

        // 3. Match generated legal moves
        val moves = generateMoves(fen)
        for (moveUci in moves) {
            val from = moveUci.substring(0, 2)
            val to = moveUci.substring(2, 4)
            val promotion = if (moveUci.length > 4) moveUci[4] else promoChar

            val moveSan = moveToSan(fen, from, to, promotion)
            if (moveSan.replace("+", "").replace("#", "").replace("=", "") == cleanSan.replace("=", "")) {
                return Triple(from, to, promotion)
            }
        }

        // 4. Structural Disambiguation Fallback
        val targetSq = moveCore.takeLast(2)
        if (targetSq.length == 2 && targetSq[0] in 'a'..'h' && targetSq[1] in '1'..'8') {
            val isPawnMove = moveCore[0].isLowerCase()
            val pieceChar = if (isPawnMove) 'p' else moveCore[0].lowercaseChar()

            for (moveUci in moves) {
                val from = moveUci.substring(0, 2)
                val to = moveUci.substring(2, 4)
                val piece = state.pieces[from] ?: continue

                if (to == targetSq && piece.lowercaseChar() == pieceChar) {
                    val prefix = if (isPawnMove) moveCore.dropLast(2) else moveCore.drop(1).dropLast(2)
                    val cleanPrefix = prefix.replace("x", "")

                    var match = true
                    if (cleanPrefix.isNotEmpty()) {
                        for (ch in cleanPrefix) {
                            if (ch in 'a'..'h' && from[0] != ch) match = false
                            if (ch in '1'..'8' && from[1] != ch) match = false
                        }
                    }
                    if (match) {
                        return Triple(from, to, promoChar)
                    }
                }
            }
        }

        return null
    }

    data class PgnImportResult(
        val initialFen: String, 
        val moves: List<Triple<String, String, Char?>>,
        val variant: GameVariant = GameVariant.CLASSIC
    )

    fun importPgn(pgn: String): PgnImportResult {
        var initialFen = getInitialFen()
        val fenMatch = """\[FEN\s+"([^"]+)"\]""".toRegex(RegexOption.IGNORE_CASE).find(pgn)
        if (fenMatch != null) {
            initialFen = fenMatch.groupValues[1]
        }
        
        val variantMatch = """\[Variant\s+"(.*?)"\]""".toRegex(RegexOption.IGNORE_CASE).find(pgn)
        val variant = when (variantMatch?.groupValues?.get(1)?.lowercase()) {
            "chess 960", "chess960", "fischerandom" -> GameVariant.CHESS960
            else -> GameVariant.CLASSIC
        }

        // Clean PGN: Remove headers, comments {}, variations (), NAGs $1, move numbers
        val movesOnly = pgn.replace("""\[[\s\S]*?\]""".toRegex(), "")
            .replace("""\{[\s\S]*?\}""".toRegex(), "")
            .replace("""\([\s\S]*?\)""".toRegex(), "")
            .replace("""\$\d+""".toRegex(), "")
            .replace("""\d+\.+\s*""".toRegex(), " ")
            .replace("""\s+""".toRegex(), " ")
            .trim()
            
        val sanMoves = movesOnly.split(" ").filter { 
            it.isNotBlank() && it != "1-0" && it != "0-1" && it != "1/2-1/2" && it != "*"
        }
        
        val result = mutableListOf<Triple<String, String, Char?>>()
        var currentFen = initialFen
        
        for (san in sanMoves) {
            val move = sanToMove(currentFen, san) ?: continue
            result.add(move)
            currentFen = applyMove(currentFen, move.first, move.second, move.third)
        }
        return PgnImportResult(initialFen, result, variant)
    }

        // Advanced Kotlin Minimax Alpha-Beta chess engine fallback
        fun evaluateBoard(fen: String): Double {
            val state = parseFen(fen)
            var score = 0.0
            for ((sq, piece) in state.pieces) {
                val valOfPiece = when (piece.lowercaseChar()) {
                    'p' -> 10.0
                    'n' -> 30.0
                    'b' -> 30.0
                    'r' -> 50.0
                    'q' -> 90.0
                    'k' -> 1000.0
                    else -> 0.0
                }
                val sign = if (piece.isUpperCase()) 1.0 else -1.0
                score += valOfPiece * sign

                val file = sq[0] - 'a'
                val rank = sq[1] - '1'
                // Center control bonus
                if (file in 3..4 && rank in 3..4) {
                    score += 1.5 * sign
                } else if (file in 2..5 && rank in 2..5) {
                    score += 0.5 * sign
                }
                // Development bonus for Knights and Bishops
                if (piece.lowercaseChar() == 'n' || piece.lowercaseChar() == 'b') {
                    if (piece.isUpperCase() && rank > 0) score += 0.3
                    if (!piece.isUpperCase() && rank < 7) score -= 0.3
                }
            }
            return score
        }

        fun minimax(
            fen: String,
            depth: Int,
            alpha: Double,
            beta: Double,
            isMaximizing: Boolean
        ): Pair<Double, String?> {
            val moves = generateMoves(fen)
            if (depth == 0 || moves.isEmpty()) {
                return Pair(evaluateBoard(fen), null)
            }

            var localAlpha = alpha
            var localBeta = beta
            var bestMove: String? = null

            if (isMaximizing) {
                var maxEval = -1000000.0
                val sortedMoves = moves.sortedByDescending { m ->
                    val captureBonus =
                        if (parseFen(fen).pieces.containsKey(m.substring(2, 4))) 10.0 else 0.0
                    captureBonus
                }
                for (move in sortedMoves) {
                    val nextFen = applyMove(fen, move.substring(0, 2), move.substring(2, 4))
                    val eval = minimax(nextFen, depth - 1, localAlpha, localBeta, false).first
                    if (eval > maxEval) {
                        maxEval = eval
                        bestMove = move
                    }
                    localAlpha = maxOf(localAlpha, eval)
                    if (localBeta <= localAlpha) break
                }
                return Pair(maxEval, bestMove)
            } else {
                var minEval = 1000000.0
                val sortedMoves = moves.sortedBy { m ->
                    val captureBonus =
                        if (parseFen(fen).pieces.containsKey(m.substring(2, 4))) 10.0 else 0.0
                    captureBonus
                }
                for (move in sortedMoves) {
                    val nextFen = applyMove(fen, move.substring(0, 2), move.substring(2, 4))
                    val eval = minimax(nextFen, depth - 1, localAlpha, localBeta, true).first
                    if (eval < minEval) {
                        minEval = eval
                        bestMove = move
                    }
                    localBeta = minOf(localBeta, eval)
                    if (localBeta <= localAlpha) break
                }
                return Pair(minEval, bestMove)
            }
        }
}
