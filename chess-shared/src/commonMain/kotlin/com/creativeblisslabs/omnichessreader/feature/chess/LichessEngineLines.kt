package com.creativeblisslabs.omnichessreader.feature.chess

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.omnichessreader.core.chess.EngineEvaluation
import com.creativeblisslabs.omnichessreader.core.chess.MultiPVLine
import com.creativeblisslabs.omnichessreader.core.chess.ChessLogic
import kotlin.math.abs
import kotlin.math.roundToInt

// Lichess engine_lines.dart constants
private const val ENGINE_LINE_FONT_SIZE = 11f
private val ENGINE_LINE_HEIGHT = 24.dp

/**
 * Lichess-faithful engine lines panel mirroring engine_lines.dart:
 *  - White-winning score badge: light bg + dark text (like EngineGauge.valueColor)
 *  - Black-winning score badge: dark bg + white text (like EngineGauge.backgroundColor)
 *  - Proper SAN with move numbers: "1. e4 e5 2. Nf3 Nc6 ..."
 *  - lastEval anti-flicker pattern
 *  - Empty placeholder rows while engine is calculating
 *  - Tapping a line calls onTapMove with the first UCI move
 *  - Depth/engine name footer
 */
@Composable
fun LichessEngineLines(
    evaluation: EngineEvaluation?,
    currentFen: String,
    numLines: Int = 3,
    darkBackground: Boolean = false,
    showThreatMode: Boolean = false,
    showGraphMode: Boolean = false,
    onToggleThreat: (() -> Unit)? = null,
    onToggleGraph: (() -> Unit)? = null,
    onToggleFewerLines: (() -> Unit)? = null,
    onTapMove: ((String) -> Unit)? = null
) {
    // lastEval pattern: keep previous eval to avoid flickering between position changes,
    // but reset when current position FEN changes so stale moves/eval aren't shown
    var lastEval by remember { mutableStateOf<EngineEvaluation?>(null) }
    var previousFen by remember { mutableStateOf(currentFen) }

    LaunchedEffect(currentFen) {
        val currentPlacement = currentFen.split(" ").firstOrNull() ?: currentFen
        val prevPlacement = previousFen.split(" ").firstOrNull() ?: previousFen
        if (currentPlacement != prevPlacement) {
            println("[StockfishDebug] Position changed in LichessEngineLines from $previousFen to $currentFen - clearing previous lines")
            lastEval = null
            previousFen = currentFen
        }
    }

    if (evaluation != null) {
        lastEval = evaluation
    }

    LaunchedEffect(evaluation) {
        println("[StockfishDebug] LichessEngineLines evaluation update -> score=${evaluation?.score}, depth=${evaluation?.depth}, lines=${evaluation?.multiPV?.size}")
    }

    val evalToShow = evaluation ?: lastEval

    val bgColor = if (darkBackground) Color(0xFF141312) else MaterialTheme.colorScheme.surfaceContainerLow
    val textColor = if (darkBackground) Color(0xFFCCCCCC) else MaterialTheme.colorScheme.onSurfaceVariant

    // White-winning: near-white bg + dark text (mirrors EngineGauge.valueColor which is near-white in dark mode)
    val whiteBadgeBg = Color(0xFFE8E8E8)
    val whiteBadgeText = Color(0xFF111111)
    // Black-winning: dark bg + white text (mirrors EngineGauge.backgroundColor)
    val blackBadgeBg = Color(0xFF2A2A2A)
    val blackBadgeText = Color(0xFFEEEEEE)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // ── TOP EVALUATION SUMMARY TITLE (matching reference screenshots) ──
        val isWhiteTurn = remember(currentFen) {
            val parts = currentFen.split(" ")
            if (parts.size > 1) parts[1] == "w" else true
        }

        val topScore = evalToShow?.score ?: 0.0
        val topMate = evalToShow?.mate
        val firstPvUci = evalToShow?.bestLine?.firstOrNull()
        val firstPvSan = if (!firstPvUci.isNullOrBlank()) {
            ChessLogic.convertUciLineToSan(currentFen, listOf(firstPvUci)).ifBlank { firstPvUci }
        } else null

        val summaryTitle = when {
            showThreatMode -> {
                if (firstPvSan != null) {
                    val moveNum = currentFen.split(" ").getOrNull(5)?.toIntOrNull() ?: 1
                    val prefix = if (!isWhiteTurn) "$moveNum. " else "$moveNum... "
                    "If the other side could move now, it would play $prefix$firstPvSan"
                } else {
                    "The other side has nothing to play."
                }
            }
            topMate != null -> {
                if (topMate > 0) "White mates in $topMate" else "Black mates in ${abs(topMate)}"
            }
            topScore >= 3.0 -> "White is winning"
            topScore >= 1.0 -> "White is clearly better"
            topScore >= 0.3 -> "White is slightly better"
            topScore <= -3.0 -> "Black is winning"
            topScore <= -1.0 -> "Black is clearly better"
            topScore <= -0.3 -> "Black is slightly better"
            else -> "The position is level"
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = summaryTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            val formattedTopScore = when {
                topMate != null -> if (topMate > 0) "+M$topMate" else "-M${abs(topMate)}"
                topScore > 0 -> "+${(topScore * 10).roundToInt() / 10.0}"
                topScore < 0 -> "${(topScore * 10).roundToInt() / 10.0}"
                else -> "0.0"
            }
            Text(
                text = formattedTopScore,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (topScore >= 0.0) Color(0xFF70DEAB) else Color(0xFFFFB300)
            )
        }

        val pvLines = evalToShow?.multiPV?.take(numLines) ?: emptyList()
        val linesToShow: List<MultiPVLine?> = if (pvLines.isNotEmpty()) {
            pvLines + List((numLines - pvLines.size).coerceAtLeast(0)) { null }
        } else {
            List(numLines) { null }
        }

        linesToShow.forEachIndexed { lineIdx, pv ->
            AnimatedContent(
                targetState = pv,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "engine_line_$lineIdx"
            ) { pvData ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ENGINE_LINE_HEIGHT)
                        .then(
                            if (pvData != null && onTapMove != null) {
                                Modifier.clickable {
                                    pvData.bestLine.firstOrNull()?.let { onTapMove(it) }
                                }
                            } else Modifier
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pvData == null) {
                        // Empty placeholder row while engine is thinking
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(18.dp)
                                .background(
                                    if (darkBackground) Color(0xFF262421) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        if (lineIdx == 0 && lastEval == null) {
                            Text(
                                text = "Stockfish is calculating...",
                                fontSize = ENGINE_LINE_FONT_SIZE.sp,
                                color = textColor.copy(alpha = 0.5f),
                                maxLines = 1
                            )
                        }
                    } else {
                        // Score badge: colored by who is winning
                        val (scoreText, isWhiteWinning) = pvData.evalScoreText()
                        val badgeBg = if (isWhiteWinning) whiteBadgeBg else blackBadgeBg
                        val badgeTextColor = if (isWhiteWinning) whiteBadgeText else blackBadgeText

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeBg,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = scoreText,
                                fontSize = ENGINE_LINE_FONT_SIZE.sp,
                                fontWeight = FontWeight.W600,
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        // SAN move line with move numbers (mirrors pvData.sanMoves(fromPosition) in Dart)
                        val sanLine = remember(pvData.bestLine, currentFen) {
                            ChessLogic.convertUciLineToSan(currentFen, pvData.bestLine)
                                .ifBlank { pvData.bestLine.joinToString(" ") }
                        }

                        Text(
                            text = sanLine,
                            fontSize = ENGINE_LINE_FONT_SIZE.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "Ask",
                            fontSize = 10.sp,
                            color = Color(0xFF6B8073),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // ── FOOTER CONTROLS MATCHING SCREENSHOT: [Threat] [Graph] [Fewer lines / More lines] Depth XX ⚙ ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Threat Pill Button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (showThreatMode) Color(0xFF3B5646) else Color(0xFF1F2421),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showThreatMode) Color(0xFF70DEAB) else Color(0xFF2E3E34)
                    ),
                    modifier = Modifier.clickable { onToggleThreat?.invoke() }
                ) {
                    Text(
                        text = "Threat",
                        fontSize = 10.sp,
                        fontWeight = if (showThreatMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (showThreatMode) Color.White else Color(0xFFA5B8AD),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Graph Pill Button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (showGraphMode) Color(0xFF3B5646) else Color(0xFF1F2421),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showGraphMode) Color(0xFF70DEAB) else Color(0xFF2E3E34)
                    ),
                    modifier = Modifier.clickable { onToggleGraph?.invoke() }
                ) {
                    Text(
                        text = "Graph",
                        fontSize = 10.sp,
                        fontWeight = if (showGraphMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (showGraphMode) Color.White else Color(0xFFA5B8AD),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Lines toggle Button
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1F2421),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3E34)),
                    modifier = Modifier.clickable { onToggleFewerLines?.invoke() }
                ) {
                    Text(
                        text = if (numLines > 1) "Fewer lines" else "3 lines",
                        fontSize = 10.sp,
                        color = Color(0xFFA5B8AD),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Depth & engine icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Depth ${evalToShow?.depth ?: 22}",
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/** Returns Pair(displayString, isWhiteWinning) for the score badge. */
private fun MultiPVLine.evalScoreText(): Pair<String, Boolean> = when {
    mate != null -> {
        val m = mate!!
        val isWhite = m > 0
        Pair(if (isWhite) "+M${abs(m)}" else "-M${abs(m)}", isWhite)
    }
    else -> {
        val isWhite = score >= 0.0
        val rounded = (score * 10.0).roundToInt() / 10.0
        Pair(if (isWhite) "+$rounded" else "$rounded", isWhite)
    }
}
