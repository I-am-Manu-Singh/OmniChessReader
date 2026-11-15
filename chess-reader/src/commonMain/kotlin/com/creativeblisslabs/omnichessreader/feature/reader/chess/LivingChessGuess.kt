package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlin.math.abs
import kotlin.math.roundToInt

data class BookPlace(
    val moveId: String? = null,
    val diagramId: String? = null
)

sealed class GuessVerdict {
    data class Right(val move: BookMoveNode) : GuessVerdict()
    data class SideLine(val move: BookMoveNode) : GuessVerdict()
    object Wrong : GuessVerdict()
}

data class GuessScore(
    val asked: Int = 0,
    val firstTry: Int = 0
)

enum class MoveGrade {
    AS_GOOD,
    PLAYABLE,
    WORSE,
    LOSING
}

data class MoveJudgement(
    val grade: MoveGrade,
    val lossInPawns: Float
)

object LivingChessGuess {

    private const val MATE_AS_CENTIPAWNS = 10000
    private const val AS_GOOD_CENTIPAWNS = 30
    private const val PLAYABLE_CENTIPAWNS = 100
    private const val WORSE_CENTIPAWNS = 300

    fun nextMoves(overlay: LivingBookOverlay, moveId: String): List<BookMoveNode> {
        return overlay.moves.filter { it.parentId == moveId }
            .sortedByDescending { it.isMainLine }
    }

    fun bookMovesFrom(overlay: LivingBookOverlay, place: BookPlace): List<BookMoveNode> {
        if (place.moveId != null) return nextMoves(overlay, place.moveId)
        if (place.diagramId == null) return emptyList()
        return overlay.moves
            .filter { it.parentId == null && it.diagramId == place.diagramId }
            .sortedByDescending { it.isMainLine }
    }

    private fun plainSan(san: String): String = san.replace(Regex("[+#!?]+$"), "")

    fun judgeGuess(expected: List<BookMoveNode>, san: String): GuessVerdict {
        val target = plainSan(san)
        val match = expected.find { plainSan(it.san) == target } ?: return GuessVerdict.Wrong
        return if (match == expected.firstOrNull()) {
            GuessVerdict.Right(match)
        } else {
            GuessVerdict.SideLine(match)
        }
    }

    private fun asCentipawns(score: EngineScore): Int {
        return if (score.kind == "cp") {
            score.value
        } else {
            val sgn = if (score.value >= 0) 1 else -1
            sgn * (MATE_AS_CENTIPAWNS - abs(score.value))
        }
    }

    fun gradeMove(reader: EngineScore, book: EngineScore, moverIsWhite: Boolean): MoveJudgement {
        val diff = asCentipawns(book) - asCentipawns(reader)
        val loss = maxOf(0, if (moverIsWhite) diff else -diff)
        val grade = when {
            loss <= AS_GOOD_CENTIPAWNS -> MoveGrade.AS_GOOD
            loss <= PLAYABLE_CENTIPAWNS -> MoveGrade.PLAYABLE
            loss <= WORSE_CENTIPAWNS -> MoveGrade.WORSE
            else -> MoveGrade.LOSING
        }
        return MoveJudgement(grade, (loss / 100f * 100f).roundToInt() / 100f)
    }

    fun diagramKey(diagram: DetectedDiagram): String = "${diagram.page}|${diagram.placement}"

    /**
     * Computes the masking cover boxes over a page when working out a position or exercise:
     * masks the rest of the line and all content below the target box to hide solutions.
     */
    fun coverBoxes(pageWidth: Float, pageHeight: Float, targetBox: PdfBox): List<PdfBox> {
        val list = mutableListOf<PdfBox>()
        // 1. Mask rest of line to the right
        if (pageWidth > targetBox.x1 && targetBox.y1 > targetBox.y0) {
            list.add(PdfBox(targetBox.x1, targetBox.y0, pageWidth, targetBox.y1))
        }
        // 2. Mask all content below
        if (pageHeight > targetBox.y1 && pageWidth > 0f) {
            list.add(PdfBox(0f, targetBox.y1, pageWidth, pageHeight))
        }
        return list
    }

    /** Returns all moves still ahead in the book sequence to remain hidden while guessing. */
    fun movesAhead(overlay: LivingBookOverlay, place: BookPlace): Set<String> {
        val ahead = mutableSetOf<String>()
        val waiting = bookMovesFrom(overlay, place).toMutableList()
        while (waiting.isNotEmpty()) {
            val move = waiting.removeAt(waiting.size - 1)
            if (ahead.add(move.id)) {
                waiting.addAll(nextMoves(overlay, move.id))
            }
        }
        return ahead
    }

    fun solvableDiagrams(overlay: LivingBookOverlay): List<DetectedDiagram> {
        val withMoves = overlay.moves
            .filter { it.parentId == null && it.diagramId != null }
            .mapNotNull { it.diagramId }
            .toSet()
        return overlay.diagrams
            .filter { withMoves.contains(it.id) }
            .sortedWith(compareBy({ it.page }, { it.box.y0 }))
    }

    fun nextUnsolved(
        overlay: LivingBookOverlay,
        solvedKeys: Set<String>,
        fromPage: Int
    ): DetectedDiagram? {
        val open = solvableDiagrams(overlay).filter { !solvedKeys.contains(diagramKey(it)) }
        return open.find { it.page >= fromPage } ?: open.firstOrNull()
    }
}

