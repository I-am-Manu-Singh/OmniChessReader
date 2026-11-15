package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlinx.serialization.Serializable

/** A rectangle in PDF page points, with origin at top-left. */
@Serializable
data class PdfBox(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float
) {
    val width: Float get() = maxOf(0f, x1 - x0)
    val height: Float get() = maxOf(0f, y1 - y0)
    val area: Float get() = width * height
}

/** A word extracted from a PDF text layer. */
@Serializable
data class PdfWord(
    val text: String,
    val page: Int,
    val box: PdfBox,
    val ink: Float = 0f,
    val glyphLost: Boolean = false
)

/** A move token parsed from text before matching with a chess position. */
@Serializable
data class MoveToken(
    val moveNumber: Int? = null,
    val dots: Int = 0,
    val key: String = "",
    val fromFile: Char? = null,
    val capture: Boolean = false,
    val to: String? = null,
    val promotion: Boolean = false,
    val castle: Char? = null // 'k' (kingside), 'q' (queenside), or null
)

/** Color of pieces / side to move. */
enum class ChessColor(val symbol: Char) {
    WHITE('w'),
    BLACK('b');

    companion object {
        fun fromChar(c: Char): ChessColor? = when (c) {
            'w', 'W' -> WHITE
            'b', 'B' -> BLACK
            else -> null
        }
    }
}

/** Represents a table of contents or chapter navigation item. */
@Serializable
data class LivingChessChapter(
    val title: String,
    val page: Int,
    val level: Int = 0,
    val href: String = ""
)

/** A detected diagram on a page with recognized placement. */
@Serializable
data class DetectedDiagram(
    val id: String,
    val page: Int,
    val box: PdfBox,
    val placement: String,
    val sideToMove: String? = null, // "w" or "b"
    val label: String? = null,
    val pageWidth: Float = 420f,
    val pageHeight: Float = 595f,
    val nearbyText: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val issues: List<String> = emptyList(),
    val confidence: Float = 1.0f
) {
    val isValid: Boolean get() = issues.isEmpty()
}

/** A move in the book's move tree. */
@Serializable
data class BookMoveNode(
    val id: String,
    val parentId: String? = null,
    val diagramId: String? = null,
    val san: String,
    val fen: String,
    val page: Int,
    val box: PdfBox,
    val isMainLine: Boolean = true,
    val confidence: String = "high", // "high" or "low"
    val numberOffset: Int = 0
)

/** Items flagged for user review. */
@Serializable
data class BookReviewItem(
    val id: String,
    val reason: String, // "invalid-diagram", "low-confidence-diagram", "low-confidence-move", "unresolved-move"
    val page: Int,
    val box: PdfBox,
    val targetId: String? = null
)

/** Complete book analysis overlay. */
@Serializable
data class LivingBookOverlay(
    val schemaVersion: Int = 1,
    val bookHash: String = "",
    val pageCount: Int = 0,
    val diagrams: List<DetectedDiagram> = emptyList(),
    val moves: List<BookMoveNode> = emptyList(),
    val review: List<BookReviewItem> = emptyList()
)

