package com.creativeblisslabs.omnimind.feature.chess

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
import com.creativeblisslabs.omnimind.core.chess.EngineEvaluation
import com.creativeblisslabs.omnimind.core.chess.MultiPVLine
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
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
    onTapMove: ((String) -> Unit)? = null
) {
    // lastEval pattern: keep previous eval to avoid flickering between position changes
    var lastEval by remember { mutableStateOf<EngineEvaluation?>(null) }
    if (evaluation != null) lastEval = evaluation

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
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
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
                    }
                }
            }
        }

        // Depth + engine name footer row
        if (evalToShow != null) {
            Text(
                text = "Stockfish 17 · depth ${evalToShow.depth}",
                fontSize = 9.sp,
                color = textColor.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 1.dp)
            )
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
