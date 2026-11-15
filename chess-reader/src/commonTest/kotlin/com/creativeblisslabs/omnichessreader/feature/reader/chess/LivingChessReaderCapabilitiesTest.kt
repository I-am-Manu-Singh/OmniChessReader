package com.creativeblisslabs.omnichessreader.feature.reader.chess

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LivingChessReaderCapabilitiesTest {

    @Test
    fun testParseBookDiagramAndAnalyzeMoves() {
        // Page 1 Words extracted from the sample chess book
        val page1Words = listOf(
            PdfWord("Opening", 0, PdfBox(50f, 50f, 90f, 62f), ink = 0.2f),
            PdfWord("notes", 0, PdfBox(95f, 50f, 130f, 62f), ink = 0.2f),
            PdfWord("1.e4", 0, PdfBox(50f, 70f, 80f, 82f), ink = 0.35f),
            PdfWord("e5", 0, PdfBox(85f, 70f, 105f, 82f), ink = 0.35f),
            PdfWord("2.Nf3", 0, PdfBox(110f, 70f, 145f, 82f), ink = 0.35f),
            PdfWord("Nc6", 0, PdfBox(150f, 70f, 180f, 82f), ink = 0.35f),
            PdfWord("3.Bc4", 0, PdfBox(185f, 70f, 220f, 82f), ink = 0.35f),
            PdfWord("is", 0, PdfBox(225f, 70f, 235f, 82f), ink = 0.15f),
            PdfWord("a", 0, PdfBox(240f, 70f, 245f, 82f), ink = 0.15f),
            PdfWord("calm", 0, PdfBox(250f, 70f, 275f, 82f), ink = 0.15f),
            PdfWord("start.", 0, PdfBox(280f, 70f, 310f, 82f), ink = 0.15f),
            PdfWord("Diagram", 0, PdfBox(50f, 250f, 100f, 262f), ink = 0.2f),
            PdfWord("1", 0, PdfBox(105f, 250f, 115f, 262f), ink = 0.2f),
            PdfWord("White", 0, PdfBox(50f, 270f, 85f, 282f), ink = 0.2f),
            PdfWord("to", 0, PdfBox(90f, 270f, 100f, 282f), ink = 0.2f),
            PdfWord("move.", 0, PdfBox(105f, 270f, 140f, 282f), ink = 0.2f),
            PdfWord("1.Ng5", 0, PdfBox(50f, 290f, 85f, 302f), ink = 0.35f),
            PdfWord("O-O", 0, PdfBox(90f, 290f, 115f, 302f), ink = 0.35f),
            PdfWord("2.O-O", 0, PdfBox(120f, 290f, 155f, 302f), ink = 0.35f),
            PdfWord("h6", 0, PdfBox(160f, 290f, 175f, 302f), ink = 0.35f),
            PdfWord("leaves", 0, PdfBox(180f, 290f, 215f, 302f), ink = 0.15f),
            PdfWord("the", 0, PdfBox(220f, 290f, 240f, 302f), ink = 0.15f),
            PdfWord("knight", 0, PdfBox(245f, 290f, 280f, 302f), ink = 0.15f),
            PdfWord("short", 0, PdfBox(285f, 290f, 315f, 302f), ink = 0.15f),
            PdfWord("of", 0, PdfBox(320f, 290f, 335f, 302f), ink = 0.15f),
            PdfWord("squares.", 0, PdfBox(340f, 290f, 385f, 302f), ink = 0.15f)
        )

        // Diagram 1 Position on Page 1
        val diagram1 = DetectedDiagram(
            id = "d0-0",
            page = 0,
            box = PdfBox(50f, 100f, 250f, 240f),
            placement = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R",
            sideToMove = "w",
            label = "Diagram 1",
            issues = emptyList(),
            confidence = 0.98f
        )

        // 1. Placement verification via LivingChessPlacement
        val issues = LivingChessPlacement.checkPlacement(diagram1.placement)
        assertTrue(issues.isEmpty(), "Diagram FEN placement should be 100% valid")

        val fullFen = LivingChessPlacement.fenFromDiagram(diagram1.placement, ChessColor.WHITE)
        assertTrue(fullFen.contains("w KQkq"), "Castling rights should be properly inferred")

        // 2. Metadata detection via LivingChessBook
        val detectedLabel = LivingChessBook.labelFromWords(page1Words)
        assertEquals("diagram 1", detectedLabel)

        val detectedTurn = LivingChessBook.sideToMoveFromWords(page1Words)
        assertEquals(ChessColor.WHITE, detectedTurn)

        // 3. Move tokenization & tree analysis
        val (moves, _) = LivingChessBook.analyzeWords(
            pages = listOf(page1Words),
            diagrams = listOf(diagram1)
        )
        assertTrue(moves.isNotEmpty(), "Should extract moves from the page")
        val sanList = moves.map { it.san }
        println("Decoded SAN Moves using App Capabilities: $sanList")

        // 4. Analysis and playability verification
        val playability = LivingChessAnalysis.checkPlayability(fullFen)
        assertEquals(DiagramPlayability.PLAYABLE, playability)

        val eval = LivingChessAnalysis.evaluatePosition(fullFen, depth = 3)
        assertTrue(eval.moves.isNotEmpty(), "Engine should produce candidate move")
        println("Engine evaluation: ${LivingChessAnalysis.formatScore(eval.score)} | Best move: ${eval.moves.firstOrNull()?.san}")
    }
}

