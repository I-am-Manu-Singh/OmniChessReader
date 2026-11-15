package com.creativeblisslabs.omnichessreader.feature.reader.chess

import com.creativeblisslabs.omnichessreader.core.chess.ChessLogic
import kotlin.math.ln

/**
 * Learns OCR symbols for chess pieces in a specific book (e.g. damaged symbols, figurine letters).
 */
class GlyphModel {
    private val counts = mutableMapOf<String, MutableMap<Char, Float>>()

    init {
        // Plain text defaults: upper case letter names piece, empty prefix means pawn
        for (piece in listOf('n', 'b', 'r', 'q', 'k')) {
            observe(piece.uppercase(), piece, PRIOR_FOR_LETTERS)
        }
        observe("", 'p', PRIOR_FOR_NO_PREFIX)
    }

    fun observe(key: String, piece: Char, weight: Float = 1f) {
        val pieces = counts.getOrPut(key) { mutableMapOf() }
        pieces[piece] = (pieces[piece] ?: 0f) + weight
    }

    /** Log probability of a piece given a prefix symbol. */
    fun score(key: String, piece: Char): Float {
        val pieces = counts[key] ?: return 0f
        var total = 0f
        for (c in pieces.values) total += c
        val count = pieces[piece] ?: 0f
        return ln(((count + 0.3f) / (total + 0.3f * PIECE_COUNT)).toDouble()).toFloat()
    }

    companion object {
        private const val PIECE_COUNT = 6f
        private const val PRIOR_FOR_LETTERS = 20f
        private const val PRIOR_FOR_NO_PREFIX = 3f
    }
}

sealed class DecodedStep {
    data class MoveStep(
        val san: String,
        val piece: Char,
        val fen: String,
        val token: MoveToken,
        val tokenIndex: Int,
        val lowConfidence: Boolean
    ) : DecodedStep()

    data class InsertedStep(
        val san: String,
        val fen: String
    ) : DecodedStep()

    data class SkipStep(
        val token: MoveToken,
        val tokenIndex: Int
    ) : DecodedStep()
}

data class DecodedLine(
    val steps: List<DecodedStep>,
    val score: Float,
    val finalFen: String
)

data class DecodeOptions(
    val membership: List<Float>? = null,
    val model: GlyphModel = GlyphModel(),
    val offset: Int = 0
)

class PositionCache {
    private val legalMovesCache = mutableMapOf<String, List<LegalMoveCandidate>>()
    private val fenAfterCache = mutableMapOf<String, String>()

    data class LegalMoveCandidate(
        val from: String,
        val to: String,
        val san: String,
        val piece: Char,
        val isCapture: Boolean,
        val promotion: Char? = null
    )

    fun legalMoves(fen: String): List<LegalMoveCandidate> {
        return legalMovesCache.getOrPut(fen) {
            val uciMoves = ChessLogic.generateMoves(fen)
            val state = ChessLogic.parseFen(fen)
            uciMoves.mapNotNull { uci ->
                val from = uci.substring(0, 2)
                val to = uci.substring(2, 4)
                val promo = if (uci.length > 4) uci[4] else null
                val movingPiece = state.pieces[from] ?: return@mapNotNull null
                val isCapture = state.pieces.containsKey(to) || (movingPiece.lowercaseChar() == 'p' && to == state.enPassantSquare)
                val san = ChessLogic.moveToSan(fen, from, to, promo)
                LegalMoveCandidate(
                    from = from,
                    to = to,
                    san = san,
                    piece = movingPiece.lowercaseChar(),
                    isCapture = isCapture,
                    promotion = promo
                )
            }
        }
    }

    fun fenAfter(fen: String, move: LegalMoveCandidate): String {
        val key = "$fen|${move.san}"
        return fenAfterCache.getOrPut(key) {
            ChessLogic.applyMove(fen, move.from, move.to, move.promotion)
        }
    }
}

object LivingChessDecoder {

    private const val BEAM_WIDTH = 10
    private const val INSERTING_STATES = 2
    private const val SKIP_COST = -7f
    private const val UNLIKELY_USE_COST = -2f
    private const val INSERT_COST = -6f
    private const val WRONG_NUMBER_COST = -6f
    private const val WRONG_COLOR_COST = -2f
    private const val UNMARKED_CAPTURE_COST = -4f
    private const val WRONG_FILE_COST = -2f
    private const val RIGHT_FILE_BONUS = 1f
    private const val MISSING_PROMOTION_COST = -3f
    private val LOW_CONFIDENCE_SCORE = ln(0.25).toFloat()

    private class Trail(
        val step: DecodedStep,
        val previous: Trail?
    )

    private data class BeamState(
        val fen: String,
        val score: Float,
        val trail: Trail?
    )

    fun moveFullNumberOffset(tokens: List<MoveToken>): Int {
        val first = tokens.firstOrNull { it.moveNumber != null }
        return if (first?.moveNumber != null) first.moveNumber - 1 else 0
    }

    private fun colorOf(fen: String): Char = fen.split(" ").getOrNull(1)?.firstOrNull() ?: 'w'
    private fun fullMoveOf(fen: String): Int = fen.split(" ").getOrNull(5)?.toIntOrNull() ?: 1

    private fun numberPenalty(token: MoveToken, fen: String, offset: Int): Float {
        if (token.moveNumber == null) return 0f
        var penalty = if (fullMoveOf(fen) + offset == token.moveNumber) 0f else WRONG_NUMBER_COST
        if (token.dots >= 3 && colorOf(fen) == 'w') penalty += WRONG_COLOR_COST
        if (token.dots == 1 && colorOf(fen) == 'b') penalty += WRONG_COLOR_COST
        return penalty
    }

    private fun isReplyMissing(token: MoveToken, fen: String, offset: Int): Boolean {
        if (token.moveNumber == null) return false
        val here = fullMoveOf(fen) + offset
        val tokenColor = if (token.dots >= 3) 'b' else 'w'
        if (here > token.moveNumber) return false
        return !(here == token.moveNumber && colorOf(fen) == tokenColor)
    }

    private fun candidatesFor(
        fen: String,
        token: MoveToken,
        cache: PositionCache,
        model: GlyphModel
    ): List<Pair<PositionCache.LegalMoveCandidate, Float>> {
        val result = mutableListOf<Pair<PositionCache.LegalMoveCandidate, Float>>()
        for (move in cache.legalMoves(fen)) {
            if (move.promotion != null && move.promotion != 'q') continue

            if (token.castle != null) {
                val isLong = move.san.startsWith("O-O-O")
                if (move.san.startsWith("O-O") && isLong == (token.castle == 'q')) {
                    result.add(move to 0f)
                }
                continue
            }

            if (move.to != token.to) continue
            if (token.capture && !move.isCapture) continue

            var score = model.score(token.key, move.piece)
            if (!token.capture && move.isCapture) score += UNMARKED_CAPTURE_COST
            if (token.fromFile != null) {
                score += if (move.from.firstOrNull() == token.fromFile) RIGHT_FILE_BONUS else WRONG_FILE_COST
            }
            if (token.promotion && move.promotion == null) score += MISSING_PROMOTION_COST

            result.add(move to score)
        }
        return result
    }

    private fun keepBest(states: List<BeamState>): List<BeamState> {
        val sorted = states.sortedByDescending { it.score }
        val seen = mutableSetOf<String>()
        val best = mutableListOf<BeamState>()
        for (state in sorted) {
            if (seen.add(state.fen)) {
                best.add(state)
                if (best.size >= BEAM_WIDTH) break
            }
        }
        return best
    }

    private fun toSteps(trail: Trail?): List<DecodedStep> {
        val steps = mutableListOf<DecodedStep>()
        var curr = trail
        while (curr != null) {
            steps.add(curr.step)
            curr = curr.previous
        }
        return steps.reversed()
    }

    /**
     * Finds the most likely sequence of legal chess moves for a list of tokens via Beam Search.
     */
    fun decodeLine(
        startFen: String,
        tokens: List<MoveToken>,
        options: DecodeOptions = DecodeOptions()
    ): DecodedLine {
        val model = options.model
        val offset = options.offset
        val cache = PositionCache()
        var beam = listOf(BeamState(startFen, 0f, null))

        tokens.forEachIndexed { tokenIndex, token ->
            val nextStates = mutableListOf<BeamState>()
            val belonging = options.membership?.getOrNull(tokenIndex) ?: 1f
            val skipCost = SKIP_COST * belonging
            val useCost = UNLIKELY_USE_COST * (1f - belonging)

            fun addMoves(state: BeamState, fen: String, extraCost: Float, trail: Trail?) {
                for ((move, score) in candidatesFor(fen, token, cache, model)) {
                    val after = cache.fenAfter(fen, move)
                    val step = DecodedStep.MoveStep(
                        san = move.san,
                        piece = move.piece,
                        fen = after,
                        token = token,
                        tokenIndex = tokenIndex,
                        lowConfidence = score < LOW_CONFIDENCE_SCORE
                    )
                    nextStates.add(
                        BeamState(
                            fen = after,
                            score = state.score + score + extraCost + useCost + numberPenalty(token, fen, offset),
                            trail = Trail(step, trail)
                        )
                    )
                }
            }

            beam.forEachIndexed { rank, state ->
                // Option 1: Skip token
                nextStates.add(
                    BeamState(
                        fen = state.fen,
                        score = state.score + skipCost,
                        trail = Trail(DecodedStep.SkipStep(token, tokenIndex), state.trail)
                    )
                )

                // Option 2: Match move directly
                addMoves(state, state.fen, 0f, state.trail)

                // Option 3: Insert missing intermediate move if turn/number indicates missing reply
                if (rank < INSERTING_STATES && isReplyMissing(token, state.fen, offset)) {
                    for (reply in cache.legalMoves(state.fen)) {
                        if (reply.promotion != null && reply.promotion != 'q') continue
                        val after = cache.fenAfter(state.fen, reply)
                        val insertedTrail = Trail(
                            DecodedStep.InsertedStep(reply.san, after),
                            state.trail
                        )
                        addMoves(state, after, INSERT_COST, insertedTrail)
                    }
                }
            }

            beam = keepBest(nextStates)
        }

        val best = beam.firstOrNull() ?: BeamState(startFen, 0f, null)
        return DecodedLine(
            steps = toSteps(best.trail),
            score = best.score,
            finalFen = best.fen
        )
    }
}

