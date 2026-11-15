package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Diagnostic log dump for a scanned PDF / EPUB page containing chess diagrams and move notation.
 * Used to compare against the reference book image and pinpoint parsing or OCR defects.
 */
@Serializable
data class PageDiagramAuditLog(
    val bookTitle: String,
    val pageNumber: Int,
    val diagramsFoundCount: Int,
    val diagrams: List<ScannedDiagramLog>,
    val movesRecognized: List<String>,
    val candidateFenList: List<String>,
    val issuesFlagged: List<String>,
    val engineEvaluationReady: Boolean
)

@Serializable
data class ScannedDiagramLog(
    val diagramId: String,
    val boundingBox: String, // "x0,y0 -> x1,y1"
    val detectedFen: String,
    val sideToMove: String,
    val confidence: Float,
    val legalityPassed: Boolean,
    val validationIssues: List<String>,
    val label: String? = null,
    val exerciseNumber: String? = null,
    val caption: String? = null,
    val nearbyText: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val engineTopLine: String? = null
)

object LivingChessDiagnostics {
    private val json = Json { prettyPrint = true }

    /**
     * Dumps complete diagnostics for an entire book (all pages) into a single formatted diagnostic log.
     */
    fun createBookDiagnosticDump(
        bookTitle: String,
        totalPages: Int,
        allPagesText: List<String>,
        extractedDiagramsByPage: Map<Int, List<com.creativeblisslabs.omnichessreader.core.media.PdfDiagramPosition>> = emptyMap()
    ): String {
        val pageAudits = mutableListOf<PageDiagramAuditLog>()
        val fenRegex = Regex("([rnbqkpRNBQKP1-8]{1,8}/){7}[rnbqkpRNBQKP1-8]{1,8}")

        val numPages = totalPages.coerceAtLeast(allPagesText.size).coerceAtLeast(extractedDiagramsByPage.keys.maxOrNull() ?: 1).coerceAtLeast(1)

        for (page in 1..numPages) {
            val pageText = allPagesText.getOrNull(page - 1)?.trim() ?: ""

            val normalized = LivingChessTokens.normalizeSigns(pageText)
            val moveRegex = Regex("(?:[KQRBN]?[a-h]?[1-8]?[xX\u00d7]?[a-h][1-8](?:=[QRBN])?|O-O(?:-O)?|0-0(?:-0)?)(?:[+#?!]+)?")
            val moves = moveRegex.findAll(normalized).map { it.value.trim().replace(Regex("[?!]+$"), "") }.toList()

            val diagrams = mutableListOf<DetectedDiagram>()

            // 1. First add font-extracted / raster-detected PDF diagrams for this page
            extractedDiagramsByPage[page]?.forEachIndexed { index, diagPos ->
                val box = if (diagPos.boxX1 > diagPos.boxX0 && diagPos.boxY1 > diagPos.boxY0) {
                    PdfBox(diagPos.boxX0, diagPos.boxY0, diagPos.boxX1, diagPos.boxY1)
                } else {
                    PdfBox(50f, 100f + index * 120f, 250f, 300f + index * 120f)
                }
                diagrams.add(
                    DetectedDiagram(
                        id = "diag_${page}_$index",
                        page = page,
                        box = box,
                        placement = diagPos.placement,
                        sideToMove = diagPos.sideToMove,
                        label = diagPos.label,
                        pageWidth = diagPos.pageWidth,
                        pageHeight = diagPos.pageHeight,
                        nearbyText = diagPos.nearbyText,
                        metadata = diagPos.metadata
                    )
                )
            }

            // 2. Add any ASCII FEN matches if not already detected
            fenRegex.findAll(pageText).forEachIndexed { index, match ->
                if (diagrams.none { it.placement.startsWith(match.value) }) {
                    val idx = diagrams.size
                    diagrams.add(
                        DetectedDiagram(
                            id = "diag_${page}_$idx",
                            page = page,
                            box = PdfBox(50f, 100f + idx * 120f, 250f, 300f + idx * 120f),
                            placement = match.value,
                            label = "Diagram ${idx + 1} (Page $page)"
                        )
                    )
                }
            }

            if (diagrams.isNotEmpty() || moves.isNotEmpty() || pageText.isNotBlank()) {
                val diagramLogs = diagrams.map { d ->
                    val placementCheck = LivingChessPlacement.checkPlacement(d.placement)
                    val candidateFen = LivingChessPlacement.fenFromDiagram(
                        d.placement.split(" ").firstOrNull() ?: d.placement,
                        ChessColor.fromChar(d.sideToMove?.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                    )
                    val playability = LivingChessAnalysis.checkPlayability(candidateFen)
                    val playabilityIssues = if (playability == DiagramPlayability.IMPOSSIBLE) listOf("Position is impossible/illegal") else emptyList()
                    ScannedDiagramLog(
                        diagramId = d.id,
                        boundingBox = "(${d.box.x0}, ${d.box.y0}) -> (${d.box.x1}, ${d.box.y1}) [Size: ${d.box.width}x${d.box.height}] in Page (${d.pageWidth}x${d.pageHeight})",
                        detectedFen = d.placement,
                        sideToMove = d.sideToMove ?: "w",
                        confidence = d.confidence,
                        legalityPassed = placementCheck.isEmpty() && playability != DiagramPlayability.IMPOSSIBLE,
                        validationIssues = placementCheck + d.issues + playabilityIssues,
                        label = d.label,
                        exerciseNumber = d.metadata["exerciseNumber"],
                        caption = d.metadata["caption"],
                        nearbyText = d.nearbyText,
                        metadata = d.metadata
                    )
                }

                val allIssues = diagramLogs.flatMap { it.validationIssues }.distinct()
                pageAudits.add(
                    PageDiagramAuditLog(
                        bookTitle = bookTitle,
                        pageNumber = page,
                        diagramsFoundCount = diagrams.size,
                        diagrams = diagramLogs,
                        movesRecognized = moves,
                        candidateFenList = diagrams.map { it.placement },
                        issuesFlagged = allIssues,
                        engineEvaluationReady = allIssues.isEmpty() && diagrams.isNotEmpty()
                    )
                )
            }
        }

        val totalDiagrams = pageAudits.sumOf { it.diagramsFoundCount }
        val totalMoves = pageAudits.sumOf { it.movesRecognized.size }
        val pagesWithDiagrams = pageAudits.count { it.diagramsFoundCount > 0 }
        val healthyDiagrams = pageAudits.flatMap { it.diagrams }.count { it.legalityPassed }

        return buildString {
            appendLine("================================================================================")
            appendLine("OMNICHESS READER - COMPREHENSIVE LIVING BOOK DIAGNOSTICS LOG (ALL PAGES)")
            appendLine("================================================================================")
            appendLine("Book Title:            $bookTitle")
            appendLine("Total Pages:           $totalPages")
            appendLine("Pages Audited:         ${pageAudits.size}")
            appendLine("Pages With Diagrams:   $pagesWithDiagrams")
            appendLine("Total Diagrams Found:  $totalDiagrams")
            appendLine("Healthy Playable FENs: $healthyDiagrams")
            appendLine("Total Moves Parsed:    $totalMoves")
            appendLine("Engine Ready Pages:    ${pageAudits.count { it.engineEvaluationReady }}")
            appendLine("================================================================================")
            appendLine()

            if (pageAudits.isEmpty()) {
                appendLine("No chess diagrams or notation detected across the scanned pages.")
            } else {
                pageAudits.forEach { audit ->
                    appendLine("--------------------------------------------------------------------------------")
                    appendLine("PAGE ${audit.pageNumber} • ${audit.diagramsFoundCount} Board(s) • ${audit.movesRecognized.size} Move(s)")
                    appendLine("Status: ${if (audit.issuesFlagged.isEmpty()) "HEALTHY" else "ISSUES DETECTED: " + audit.issuesFlagged.joinToString("; ")}")
                    appendLine("--------------------------------------------------------------------------------")
                    
                    if (audit.diagrams.isEmpty()) {
                        appendLine("  [No Board Diagrams Detected On This Page]")
                    } else {
                        audit.diagrams.forEachIndexed { idx, d ->
                            appendLine("  [Board #${idx + 1}] ID: ${d.diagramId}")
                            appendLine("    • Label:          ${d.label ?: "Unnamed"}")
                            if (d.exerciseNumber != null) {
                                appendLine("    • Exercise #:     ${d.exerciseNumber}")
                            }
                            if (d.caption != null) {
                                appendLine("    • Caption:        ${d.caption}")
                            }
                            appendLine("    • Turn:           ${if (d.sideToMove == "b") "Black to move ('b')" else "White to move ('w')"} (Confidence: ${(d.confidence * 100).toInt()}%)")
                            appendLine("    • Bounding Box:   ${d.boundingBox}")
                            appendLine("    • Raw FEN:        ${d.detectedFen}")
                            appendLine("    • Legal/Valid:    ${if (d.legalityPassed) "YES (Playable)" else "NO (Rule issues)"}")
                            if (d.validationIssues.isNotEmpty()) {
                                appendLine("    • Issues:         ${d.validationIssues.joinToString("; ")}")
                            }
                            if (!d.nearbyText.isNullOrBlank()) {
                                appendLine("    • Nearby Text:    \"${d.nearbyText}\"")
                            }
                            if (d.metadata.isNotEmpty()) {
                                appendLine("    • Metadata:       ${d.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
                            }
                            if (d.metadata["solution"] != null) {
                                appendLine("    • Book Solution:  ${d.metadata["solution"]}")
                            }
                            if (d.metadata["expectedMoves"] != null) {
                                appendLine("    • Expected Moves: ${d.metadata["expectedMoves"]}")
                            }
                            appendLine()
                        }
                    }

                    if (audit.movesRecognized.isNotEmpty()) {
                        appendLine("  Recognized Move Notation (${audit.movesRecognized.size} moves):")
                        appendLine("    ${audit.movesRecognized.joinToString(" ")}")
                        appendLine()
                    }

                    val pageRawText = allPagesText.getOrNull(audit.pageNumber - 1)?.trim()
                    if (!pageRawText.isNullOrBlank()) {
                        val textSnippet = if (pageRawText.length > 250) pageRawText.take(250) + " ... [truncated]" else pageRawText
                        appendLine("  Page Text Snippet:")
                        appendLine("    \"${textSnippet.replace("\n", " ")}\"")
                        appendLine()
                    }
                }
            }

            appendLine("================================================================================")
            appendLine("RAW JSON SUMMARY (FULL SERIALIZED AUDIT):")
            appendLine(json.encodeToString(pageAudits))
            appendLine("================================================================================")
        }
    }

    /**
     * Dumps complete diagnostics for a single page or whole book.
     */
    fun createDiagnosticDump(
        bookTitle: String,
        pageNumber: Int,
        detectedDiagrams: List<DetectedDiagram>,
        recognizedMoves: List<String>
    ): String {
        val diagramLogs = detectedDiagrams.map { d ->
            val placementCheck = LivingChessPlacement.checkPlacement(d.placement)
            ScannedDiagramLog(
                diagramId = d.id,
                boundingBox = "(${d.box.x0}, ${d.box.y0}) -> (${d.box.x1}, ${d.box.y1})",
                detectedFen = d.placement,
                sideToMove = d.sideToMove ?: "w",
                confidence = d.confidence,
                legalityPassed = placementCheck.isEmpty(),
                validationIssues = placementCheck + d.issues
            )
        }

        val allIssues = diagramLogs.flatMap { it.validationIssues }.distinct()

        val audit = PageDiagramAuditLog(
            bookTitle = bookTitle,
            pageNumber = pageNumber,
            diagramsFoundCount = detectedDiagrams.size,
            diagrams = diagramLogs,
            movesRecognized = recognizedMoves,
            candidateFenList = detectedDiagrams.map { it.placement },
            issuesFlagged = allIssues,
            engineEvaluationReady = allIssues.isEmpty() && detectedDiagrams.isNotEmpty()
        )

        return buildString {
            appendLine("================================================================================")
            appendLine("OMNICHESS READER - LIVING BOOK DIAGNOSTICS LOG")
            appendLine("================================================================================")
            appendLine("Book Title:     $bookTitle")
            appendLine("Page:           $pageNumber")
            appendLine("Diagrams Found: ${detectedDiagrams.size}")
            appendLine("Status:         ${if (allIssues.isEmpty()) "HEALTHY / PLAYABLE" else "ISSUES FLAGGED (${allIssues.size})"}")
            appendLine("--------------------------------------------------------------------------------")
            diagramLogs.forEachIndexed { i, d ->
                appendLine("DIAGRAM #$i [${d.diagramId}]")
                appendLine("  Box:        ${d.boundingBox}")
                appendLine("  FEN:        ${d.detectedFen}")
                appendLine("  Turn:       ${d.sideToMove}")
                appendLine("  Confidence: ${d.confidence}")
                appendLine("  Legal:      ${d.legalityPassed}")
                if (d.validationIssues.isNotEmpty()) {
                    appendLine("  Issues:     ${d.validationIssues.joinToString("; ")}")
                }
            }
            appendLine("--------------------------------------------------------------------------------")
            appendLine("RECOGNIZED MOVES (${recognizedMoves.size}):")
            appendLine("  ${recognizedMoves.joinToString(" ")}")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("RAW JSON PAYLOAD:")
            appendLine(json.encodeToString(audit))
            appendLine("================================================================================")
        }
    }
}

