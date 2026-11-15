package com.creativeblisslabs.omnichessreader.feature.reader.chess

import com.creativeblisslabs.omnichessreader.core.chess.ChessLogic
import kotlin.math.abs

object LivingChessBook {

    const val INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private const val MIN_BOLD_GAP = 0.06f
    private const val LOW_DIAGRAM_CONFIDENCE = 0.05f
    private const val LEARNING_PASSES = 2
    private const val MIN_SIDE_LINE_SHARE = 0.5f
    private const val MIN_MAIN_LINE_SHARE = 0.5f
    private const val MIN_NEW_GAME_SHARE = 0.85f
    private const val MIN_NEW_GAME_MOVES = 8

    private val LABEL_WORDS = listOf("problem", "exercise", "diagram", "position", "puzzle", "example", "test")

    private fun editDistance(a: String, b: String): Int {
        val row = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = row[0]
            row[0] = i
            for (j in 1..b.length) {
                val current = row[j]
                row[j] = minOf(
                    row[j] + 1,
                    row[j - 1] + 1,
                    prev + if (a[i - 1] == b[j - 1]) 0 else 1
                )
                prev = current
            }
        }
        return row[b.length]
    }

    private fun labelWord(text: String): String? {
        val lower = text.lowercase()
        return LABEL_WORDS.find { editDistance(lower, it) <= 2 && lower.length >= 4 }
    }

    fun labelFromWords(words: List<PdfWord>): String? {
        for (i in 0 until words.size - 1) {
            val word = labelWord(words[i].text)
            val match = Regex("^#?\\s*(\\d{1,4})\\b").find(words[i + 1].text)
            if (word != null && match != null) {
                return "$word ${match.groupValues[1].toInt()}"
            }
        }
        return null
    }

    fun sideToMoveFromWords(words: List<PdfWord>): ChessColor? {
        val text = words.joinToString(" ") { it.text }
        val match = Regex("\\b(white|black)\\s+to\\s+(move|play)\\b", RegexOption.IGNORE_CASE).find(text) ?: return null
        return if (match.groupValues[1].lowercase() == "white") ChessColor.WHITE else ChessColor.BLACK
    }

    fun boldThreshold(inks: List<Float>): Float? {
        if (inks.size < 2) return null
        val sorted = inks.sorted()
        var bestGapScore = -1f
        var threshold = 0f
        var leftMean = 0f
        var rightMean = 0f
        val total = sorted.sum()
        var leftSum = 0f

        for (i in 0 until sorted.size - 1) {
            leftSum += sorted[i]
            val leftCount = i + 1
            val rightCount = sorted.size - leftCount
            val lMean = leftSum / leftCount
            val rMean = (total - leftSum) / rightCount
            val score = leftCount * rightCount * (rMean - lMean) * (rMean - lMean)
            if (score > bestGapScore) {
                bestGapScore = score
                threshold = (sorted[i] + sorted[i + 1]) / 2f
                leftMean = lMean
                rightMean = rMean
            }
        }
        return if (rightMean - leftMean >= MIN_BOLD_GAP) threshold else null
    }

    data class PlacedToken(
        val token: MoveToken,
        val word: PdfWord,
        val run: Int
    )

    data class Section(
        val diagram: DetectedDiagram?,
        val tokens: List<PlacedToken>,
        val words: List<PdfWord>
    )

    private fun isBelow(word: PdfWord, diagram: DetectedDiagram): Boolean {
        return word.page > diagram.page || (word.page == diagram.page && word.box.y0 >= diagram.box.y1 - 2)
    }

    private fun isAbove(word: PdfWord, diagram: DetectedDiagram): Boolean {
        return word.page < diagram.page || (word.page == diagram.page && word.box.y1 <= diagram.box.y0 + 2)
    }

    private fun isWordClose(a: PdfBox, b: PdfBox): Boolean {
        return b.x0 - a.x1 <= (a.y1 - a.y0) * 2.5f
    }

    fun buildSections(pages: List<List<PdfWord>>, diagrams: List<DetectedDiagram>): List<Section> {
        val sortedDiagrams = diagrams.sortedWith(compareBy({ it.page }, { it.box.y0 }))
        val allWords = pages.flatten()
        val sections = mutableListOf<Section>()

        for (i in sortedDiagrams.indices) {
            val diagram = sortedDiagrams[i]
            val nextDiagram = sortedDiagrams.getOrNull(i + 1)
            val sectionWords = allWords.filter { word ->
                isBelow(word, diagram) && (nextDiagram == null || isAbove(word, nextDiagram))
            }
            sections.add(buildSection(diagram, sectionWords))
        }

        val firstDiagram = sortedDiagrams.firstOrNull()
        val preamble = if (firstDiagram != null) {
            allWords.filter { isAbove(it, firstDiagram) }
        } else {
            allWords
        }
        if (preamble.isNotEmpty()) {
            sections.add(0, buildSection(null, preamble))
        }

        return sections
    }

    private fun buildSection(diagram: DetectedDiagram?, words: List<PdfWord>): Section {
        val merged = LivingChessTokens.mergeWords(words)
        val tokens = mutableListOf<PlacedToken>()
        var run = 0
        var previousWord: PdfWord? = null

        for (word in merged) {
            val token = LivingChessTokens.parseToken(word.text, word.glyphLost)
            if (token != null) {
                if (previousWord == null || !isWordClose(previousWord.box, word.box) || previousWord.page != word.page) {
                    run++
                }
                tokens.add(PlacedToken(token, word, run))
                previousWord = word
            }
        }
        return Section(diagram, tokens, merged)
    }

    private fun startFens(diagram: DetectedDiagram): List<String> {
        return if (diagram.sideToMove != null) {
            listOf(LivingChessPlacement.fenFromDiagram(diagram.placement, ChessColor.fromChar(diagram.sideToMove.first()) ?: ChessColor.WHITE))
        } else {
            listOf(
                LivingChessPlacement.fenFromDiagram(diagram.placement, ChessColor.WHITE),
                LivingChessPlacement.fenFromDiagram(diagram.placement, ChessColor.BLACK)
            )
        }
    }

    /**
     * Builds the complete move tree and review queue across all sections of a book.
     */
    fun analyzeWords(
        pages: List<List<PdfWord>>,
        diagrams: List<DetectedDiagram>,
        onProgress: ((done: Int, total: Int) -> Unit)? = null
    ): Pair<List<BookMoveNode>, List<BookReviewItem>> {
        val sections = buildSections(pages, diagrams)
        val allInks = sections.flatMap { s -> s.tokens.map { it.word.ink } }
        val threshold = boldThreshold(allInks)

        var model = GlyphModel()
        val allMoves = mutableListOf<BookMoveNode>()
        val allReview = mutableListOf<BookReviewItem>()
        var nextId = 0

        fun boxOfToken(placed: PlacedToken): PdfBox {
            val b = placed.word.box
            val width = b.x1 - b.x0
            return b.copy(x0 = b.x0 + width * 0.3f)
        }

        // Run learning passes
        for (pass in 0 until LEARNING_PASSES) {
            allMoves.clear()
            allReview.clear()

            // Review diagrams first
            diagrams.forEach { d ->
                if (d.issues.isNotEmpty()) {
                    allReview.add(BookReviewItem("rd-${d.id}", "invalid-diagram", d.page, d.box, d.id))
                } else if (d.confidence < LOW_DIAGRAM_CONFIDENCE) {
                    allReview.add(BookReviewItem("rd-${d.id}", "low-confidence-diagram", d.page, d.box, d.id))
                }
            }

            sections.forEachIndexed { sIdx, section ->
                val diagram = section.diagram
                val placedTokens = section.tokens
                if (placedTokens.isNotEmpty()) {
                    val candidateFens = if (diagram != null && diagram.issues.isEmpty()) {
                        startFens(diagram)
                    } else {
                        listOf(INITIAL_FEN)
                    }

                    var bestLine: DecodedLine? = null
                    var bestStartFen = candidateFens.first()

                    for (startFen in candidateFens) {
                        val offset = LivingChessDecoder.moveFullNumberOffset(placedTokens.map { it.token })
                        val membership = threshold?.let { th ->
                            placedTokens.map { if (it.word.ink >= th) 1f else 0.4f }
                        }
                        val decoded = LivingChessDecoder.decodeLine(
                            startFen,
                            placedTokens.map { it.token },
                            DecodeOptions(membership = membership, model = model, offset = offset)
                        )
                        if (bestLine == null || decoded.score > bestLine!!.score) {
                            bestLine = decoded
                            bestStartFen = startFen
                        }
                    }

                    if (bestLine != null) {
                        val moveSteps = bestLine.steps.filterIsInstance<DecodedStep.MoveStep>()
                        val resolvedRatio = moveSteps.size.toFloat() / placedTokens.size.coerceAtLeast(1)

                        if (resolvedRatio >= MIN_MAIN_LINE_SHARE) {
                            var parentId: String? = null
                            moveSteps.forEach { step ->
                                val placed = placedTokens.getOrNull(step.tokenIndex) ?: placedTokens[0]
                                val nodeId = "m${nextId++}"
                                val node = BookMoveNode(
                                    id = nodeId,
                                    parentId = parentId,
                                    diagramId = diagram?.id,
                                    san = step.san,
                                    fen = step.fen,
                                    page = placed.word.page,
                                    box = boxOfToken(placed),
                                    isMainLine = true,
                                    confidence = if (step.lowConfidence) "low" else "high"
                                )
                                allMoves.add(node)
                                parentId = nodeId

                                // Feed back learned glyphs to GlyphModel
                                model.observe(step.token.key, step.piece)
                            }
                        } else {
                            allReview.add(
                                BookReviewItem(
                                    id = "r${nextId++}",
                                    reason = "unresolved-move",
                                    page = placedTokens[0].word.page,
                                    box = boxOfToken(placedTokens[0]),
                                    targetId = diagram?.id
                                )
                            )
                        }
                    }
                }
                onProgress?.invoke(pass * sections.size + sIdx + 1, LEARNING_PASSES * sections.size)
            }
        }

        return Pair(allMoves, allReview)
    }
}

