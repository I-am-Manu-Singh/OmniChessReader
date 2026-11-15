package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlin.math.max
import kotlin.math.min

object LivingChessTokens {

    private val COMMON_SHORT_WORDS = setOf(
        "a", "i", "an", "to", "if", "is", "of", "in", "on", "or", "by", "so", "at",
        "as", "it", "he", "be", "no", "we", "up", "do", "go", "and", "the", "for",
        "but", "his", "has", "was", "not"
    )

    private val SQUARE_REGEX = Regex("[a-h][1-8]")
    private val ONLY_PUNCTUATION = Regex("^[•*·.,\\uFFFD]+$")
    private val LOST_SPACE = Regex("(?<=[!?+#\\w.])\\uFFFD(?=\\d{1,3}\\.|[A-Za-z]{3})|^\\uFFFD(?=\\d{1,3}\\.)")
    private val RESULT = Regex("^(1-0|0-1)$")
    private const val MAX_PREFIX_LENGTH = 7
    private const val SAME_LINE_OVERLAP = 0.3f
    private val BARE_NUMBER = Regex("^\\d{1,3}$")
    private val DOTTED_NUMBER = Regex("^\\d{1,3}[.\\u2026]+$")
    private val LOOKS_LIKE_MOVE = Regex("^(?:[KQRBN]?[a-h]?[1-8]?[xX\\u00d7]?[a-h][1-8]|[0O]-[0O])")

    fun normalizeSigns(text: String): String {
        return text
            .replace('\u2026'.toString(), "...")
            .replace('\u00d7'.toString(), "x")
            .replace(Regex("(?<=[KQRBNa-h1-8])X(?=[a-h][1-8])"), "x")
    }

    private fun repairMoveNumber(text: String): String {
        return text
            .replace(Regex("^[•*·.,]+(?=\\d)"), "")
            .replace(Regex("^[sS](\\.+)"), "5$1")
            .replace(Regex("^[lI](\\.+)"), "1$1")
            .replace(Regex("^[lI](\\d)(\\.+)"), "1$1$2")
            .replace(Regex("^(\\d{1,3})\\s*[•·]\\s*"), "$1.")
    }

    private fun splitLostSpaces(word: PdfWord): List<PdfWord> {
        val match = LOST_SPACE.find(word.text) ?: return listOf(word)
        val idx = match.range.first
        val box = word.box
        val width = box.x1 - box.x0
        val cut = box.x0 + (width * idx) / word.text.length.coerceAtLeast(1)
        val left = word.text.substring(0, idx)
        val right = word.text.substring(idx + 1)
        val list = mutableListOf<PdfWord>()
        if (left.isNotEmpty()) {
            list.add(word.copy(text = left, box = box.copy(x1 = cut)))
        }
        list.addAll(splitLostSpaces(word.copy(text = right, box = box.copy(x0 = cut))))
        return list
    }

    private fun isFragment(text: String): Boolean {
        if (text.startsWith("0-0") || text.startsWith("O-O")) return false
        if (Regex("^\\d{1,3}[.•· ]*$").matches(text) || Regex("^[.•·\\s]+$").matches(text)) return true
        if (text.length > 4 || SQUARE_REGEX.containsMatchIn(text)) return false
        if (Regex("^[a-zA-Z]{2,}$").matches(text) || Regex("^[A-Z]\\.?$").matches(text)) return false
        if (COMMON_SHORT_WORDS.contains(text.lowercase()) || RESULT.matches(text)) return false
        return !Regex("[,;:]$").containsMatchIn(text)
    }

    private fun isSameLine(a: PdfBox, b: PdfBox): Boolean {
        val overlap = min(a.y1, b.y1) - max(a.y0, b.y0)
        val minHeight = min(a.y1 - a.y0, b.y1 - b.y0)
        return overlap > SAME_LINE_OVERLAP * minHeight
    }

    /**
     * Merges fragmented words from OCR (e.g. "2", "...", "fxe6") into single move words.
     */
    fun mergeWords(words: List<PdfWord>): List<PdfWord> {
        val repaired = words.flatMap { splitLostSpaces(it) }.map {
            it.copy(text = repairMoveNumber(it.text))
        }

        val kept = mutableListOf<PdfWord>()
        for (i in repaired.indices.reversed()) {
            val word = repaired[i]
            val next = kept.firstOrNull()
            if (ONLY_PUNCTUATION.matches(word.text) && next != null && next.text.firstOrNull()?.isDigit() == true) {
                continue
            }
            kept.add(0, word)
        }

        val merged = mutableListOf<PdfWord>()
        var i = 0
        while (i < kept.size) {
            var current = kept[i]
            var next = kept.getOrNull(i + 1)

            while (
                next != null &&
                isFragment(current.text) &&
                next.page == current.page &&
                (isSameLine(current.box, next.box) || (DOTTED_NUMBER.matches(current.text) && LOOKS_LIKE_MOVE.containsMatchIn(next.text)))
            ) {
                var newBox = current.box
                if (!isSameLine(current.box, next.box)) {
                    newBox = next.box
                }
                var glyphLost = current.glyphLost
                if (current.text.endsWith(".") && Regex("^x?[a-h][1-8]").containsMatchIn(next.text)) {
                    glyphLost = true
                }
                var updatedText = current.text
                if (BARE_NUMBER.matches(current.text) && LOOKS_LIKE_MOVE.containsMatchIn(next.text)) {
                    updatedText += "."
                }
                if (next.box.x0 - current.box.x1 > current.box.y1 - current.box.y0) {
                    newBox = newBox.copy(x0 = next.box.x0)
                }

                val currentArea = newBox.area
                val nextArea = next.box.area
                val totalArea = currentArea + nextArea
                val newInk = if (totalArea > 0) {
                    (current.ink * currentArea + next.ink * nextArea) / totalArea
                } else current.ink

                current = current.copy(
                    text = updatedText + next.text,
                    box = newBox.copy(
                        x1 = next.box.x1,
                        y1 = max(newBox.y1, next.box.y1)
                    ),
                    ink = newInk,
                    glyphLost = glyphLost
                )
                i++
                next = kept.getOrNull(i + 1)
            }
            merged.add(current)
            i++
        }
        return merged
    }

    private fun repairSquare(text: String): String {
        return text
            .replace(Regex("([a-h])S"), "$15")
            .replace(Regex("([a-h])[lI]"), "$11")
            .replace(Regex("([a-h])O"), "$10")
            .replace(Regex("([^a-zA-Z\\d.][a-h])t(?![a-zA-Z])"), "$11")
    }

    /**
     * Parses a merged word into a MoveToken. Returns null if not a valid chess move notation.
     */
    fun parseToken(text: String, glyphLost: Boolean = false): MoveToken? {
        var rest = repairSquare(normalizeSigns(text.replace("\\s".toRegex(), "")))
        var moveNumber: Int? = null
        var dots = 0

        val numbered = Regex("^(\\d{1,3})([.•·]+)(.*)$").find(rest)
        if (numbered != null) {
            moveNumber = numbered.groupValues[1].toIntOrNull()
            dots = numbered.groupValues[2].length
            rest = numbered.groupValues[3]
            if (dots == 2 || dots >= 4) {
                dots -= 1
                rest = ".$rest"
            }
        }

        val castling = Regex("^[0O]-[0O](-[0O])?(?![-\\d])").find(rest)
        if (castling != null) {
            val isQueenSide = castling.groupValues[1].isNotEmpty()
            return MoveToken(
                moveNumber = moveNumber,
                dots = dots,
                castle = if (isQueenSide) 'q' else 'k'
            )
        }

        val move = Regex("^(.*?)(x?)([a-h][1-8])(=.{0,3})?[^a-zA-Z0-9]*$").find(rest) ?: return null

        var prefix = move.groupValues[1].replace(Regex("^[=+#]+"), "").replace(Regex("^\\.+"), ".")
        if (prefix.length > MAX_PREFIX_LENGTH || prefix.contains('-')) return null

        var fromFile: Char? = null
        val withFile = Regex("^(.*?)([a-h])$").find(prefix)
        if (withFile != null) {
            fromFile = withFile.groupValues[2].firstOrNull()
            prefix = withFile.groupValues[1]
        }

        val key = if (glyphLost && prefix.isEmpty()) "_lost" else prefix
        val capture = move.groupValues[2] == "x"
        val toSquare = move.groupValues[3]
        val promotion = move.groupValues[4].isNotEmpty()

        return MoveToken(
            moveNumber = moveNumber,
            dots = dots,
            key = key,
            fromFile = fromFile,
            capture = capture,
            to = toSquare,
            promotion = promotion,
            castle = null
        )
    }
}
