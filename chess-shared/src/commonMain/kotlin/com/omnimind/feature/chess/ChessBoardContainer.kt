package com.creativeblisslabs.omnimind.feature.chess

import com.creativeblisslabs.omnimind.core.chess.*






import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.core.ui.components.ProductHeader
import com.creativeblisslabs.omnimind.core.ui.theme.getChessFontFamily
import com.creativeblisslabs.omnimind.core.chess.*
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.chess.shared.*
import androidx.compose.ui.window.Dialog
import kotlin.math.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
@Composable
fun ChessBoardContainer(
    fen: String,
    theme: ChessTheme,
    isPlayerWhite: Boolean,
    selectedSquare: String?,
    premove: Pair<String, String>?,
    showThreatMap: Boolean,
    threatMap: Map<String, Int>,
    pieceStyle: String = "cburnett",
    visualArrows: List<Pair<String, String>> = emptyList(),
    engineEvaluation: EngineEvaluation? = null,
    moveHistory: List<String> = emptyList(),
    moveAnalysis: List<MoveAnalysis> = emptyList(),
    currentMoveIndex: Int = -1,
    isRated: Boolean = false,
    onSquareClick: (String) -> Unit,
    onPromotionSelect: (Char) -> Unit = {}
) {
    val state = remember(fen) { ChessLogic.parseFen(fen) }
    val light = Color(theme.lightColor)
    val dark = Color(theme.darkColor)
    val isWhiteInCheck = remember(fen) { ChessLogic.isKingInCheck(fen, isWhite = true) }
    val isBlackInCheck = remember(fen) { ChessLogic.isKingInCheck(fen, isWhite = false) }

    val checkedKingSquare = when {
        isWhiteInCheck -> state.pieces.entries.firstOrNull { it.value == 'K' || it.value == 'K' }?.key
        isBlackInCheck -> state.pieces.entries.firstOrNull { it.value == 'k' || it.value == 'k' }?.key
        else -> null
    }

    val validDestinations = remember(fen, selectedSquare) {
        if (selectedSquare != null) ChessLogic.getValidDestinations(fen, selectedSquare) else emptyList()
    }

    val activePlyIdx = when {
        currentMoveIndex >= 0 && currentMoveIndex < moveHistory.size -> currentMoveIndex
        currentMoveIndex == -1 -> -1
        else -> moveHistory.size - 1
    }
    val lastMoveHighlight = remember(moveHistory, moveAnalysis, activePlyIdx) {
        if (activePlyIdx >= 0 && activePlyIdx < moveHistory.size) {
            val uciMove = moveAnalysis.getOrNull(activePlyIdx)?.move ?: ""
            val sanMove = moveHistory[activePlyIdx]

            if (uciMove.length >= 4 && uciMove[0] in 'a'..'h' && uciMove[1] in '1'..'8' && uciMove[2] in 'a'..'h' && uciMove[3] in '1'..'8') {
                Pair(uciMove.substring(0, 2), uciMove.substring(2, 4))
            } else if (sanMove.length >= 4 && sanMove[0] in 'a'..'h' && sanMove[1] in '1'..'8' && sanMove[2] in 'a'..'h' && sanMove[3] in '1'..'8') {
                Pair(sanMove.substring(0, 2), sanMove.substring(2, 4))
            } else {
                val destMatch = Regex("([a-h][1-8])").find(sanMove)
                if (destMatch != null) Pair("", destMatch.value) else null
            }
        } else null
    }

    val activeMoveQuality = remember(moveAnalysis, activePlyIdx) {
        if (activePlyIdx >= 0 && activePlyIdx < moveAnalysis.size) {
            moveAnalysis[activePlyIdx].quality
        } else null
    }

    val isPuzzleMode = remember(moveHistory, fen) {
        // If it's a puzzle, we might want to show engine arrows differently
        false // Placeholder check
    }

    var draggingSquare by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    var scale by remember { mutableStateOf(1f) }
    val zoomState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3f)
    }

    val perspectiveModifier = if (theme.is3D) {
        Modifier
            .graphicsLayer {
                rotationX = 25f
                cameraDistance = 8f
                scaleX = scale
                scaleY = scale
            }
            .shadow(12.dp, RoundedCornerShape(12.dp))
    } else {
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRated) {
            // Dynamic Top-Right Engine Evaluation Overlay Chip
                
            EvaluationBar(
                evaluation = engineEvaluation,
                isPlayerWhite = isPlayerWhite,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(14.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(6.dp)
                .aspectRatio(1f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    clip = true
                }
                .then(perspectiveModifier)
                .transformable(state = zoomState)
                .clip(RoundedCornerShape(6.dp))
                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
        ) {
            val density = LocalDensity.current
            val squareSize = maxWidth / 8f
            val squareSizePx = with(density) { squareSize.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(dark)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    for (r in 0 until 8) {
                        val rankIndex = if (isPlayerWhite) 7 - r else r
                        Row(modifier = Modifier.weight(1f)) {
                            for (c in 0 until 8) {
                                val fileIndex = if (isPlayerWhite) c else 7 - c
                                val fileChar = ('a' + fileIndex)
                                val rankChar = ('1' + rankIndex)
                                val squareLabel = "$fileChar$rankChar"

                                val isLightSquare = (fileIndex + rankIndex) % 2 == 1
                                val squareColor = if (isLightSquare) light else dark

                                val isLastMoveSquare = lastMoveHighlight != null &&
                                        (lastMoveHighlight.first == squareLabel || lastMoveHighlight.second == squareLabel)

                                val highlightOverlayColor = when {
                                    selectedSquare == squareLabel -> Color(0x66FFEB3B)
                                    isLastMoveSquare -> Color(0xB3F7EC74)
                                    premove != null && (premove.first == squareLabel || premove.second == squareLabel) -> Color(0x66FF5722)
                                    else -> Color.Transparent
                                }

                                val threatLevel = threatMap[squareLabel] ?: 0
                                val threatOverlayColor = when {
                                    showThreatMap && threatLevel > 0 -> Color(0x222196F3)
                                    showThreatMap && threatLevel < 0 -> Color(0x22F44336)
                                    else -> Color.Transparent
                                }

                                val isCheckedKing = checkedKingSquare == squareLabel
                                val checkOverlayColor = if (isCheckedKing) Color(0x66F44336) else Color.Transparent

                                SquareContainer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    squareColor = squareColor,
                                    highlightOverlayColor = highlightOverlayColor,
                                    threatOverlayColor = threatOverlayColor,
                                    checkOverlayColor = checkOverlayColor,
                                    isCheckedKing = isCheckedKing,
                                    onClick = { onSquareClick(squareLabel) }
                                ) {
                                    if (lastMoveHighlight?.second == squareLabel && activeMoveQuality != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(2.dp),
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            MoveQualityBadge(quality = activeMoveQuality)
                                        }
                                    }
                                    val piece = state.pieces[squareLabel]

                                    if (piece != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(squareLabel) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggingSquare = squareLabel
                                                            onSquareClick(squareLabel)
                                                            dragOffset = Offset.Zero
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffset += dragAmount / scale
                                                        },
                                                        onDragEnd = {
                                                            val startFile = squareLabel[0] - 'a'
                                                            val startRank = squareLabel[1] - '1'

                                                            val dropFileOffset = (dragOffset.x / squareSizePx).roundToInt()
                                                            val dropRankOffset = (dragOffset.y / squareSizePx).roundToInt()

                                                            val dropFile = if (isPlayerWhite) startFile + dropFileOffset else startFile - dropFileOffset
                                                            val dropRank = if (isPlayerWhite) startRank - dropRankOffset else startRank + dropRankOffset

                                                            if (dropFile in 0..7 && dropRank in 0..7) {
                                                                val targetSquare = "${('a' + dropFile)}${dropRank + 1}"

                                                                if (ChessLogic.isPromotionMove(fen, squareLabel, targetSquare)) {
                                                                    onPromotionSelect('q')
                                                                }

                                                                if (targetSquare != squareLabel) {
                                                                    onSquareClick(targetSquare)
                                                                }
                                                            }

                                                            draggingSquare = null
                                                            dragOffset = Offset.Zero
                                                        },
                                                        onDragCancel = {
                                                            draggingSquare = null
                                                            dragOffset = Offset.Zero
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (draggingSquare != squareLabel) {
                                                val painter = getPiecePainter(piece, pieceStyle)
                                                if (painter != null) {
                                                    Image(
                                                        painter = painter,
                                                        contentDescription = piece.toString(),
                                                        modifier = Modifier
                                                            .size((squareSize.value * 0.85f).dp)
                                                            .graphicsLayer()
                                                    )
                                                } else {
                                                    Text(
                                                        text = piece.toString(),
                                                        color = if (piece.isUpperCase()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = (squareSize.value * 0.5f).sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    val isDestination = validDestinations.contains(squareLabel)
                                    if (isDestination) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (piece == null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size((squareSize.value * 0.32f).dp)
                                                        .background(Color(0x44000000), CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size((squareSize.value * 0.88f).dp)
                                                        .border(4.dp, Color(0x44000000), CircleShape)
                                                )
                                            }
                                        }
                                    }

                                    val displayRankChar = if (isPlayerWhite) (8 - r) else (1 + r)
                                    val displayFileChar = if (isPlayerWhite) ('a' + c) else ('h' - c)

                                    val lastMoveQuality = if (lastMoveHighlight?.second == squareLabel) activeMoveQuality else null

                                    if (lastMoveQuality != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(2.dp),
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            MoveQualityBadge(quality = lastMoveQuality)
                                        }
                                    }
                                    
                                    val isLightSq = (r + c) % 2 == 0
                                    val labelColor = if (isLightSq) dark.copy(alpha = 0.7f) else light.copy(alpha = 0.7f)

                                    if (c == 0) {
                                        Text(
                                            text = displayRankChar.toString(),
                                            color = labelColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(start = 3.dp, top = 2.dp)
                                        )
                                    }
                                    if (r == 7) {
                                        Text(
                                            text = displayFileChar.toString(),
                                            color = labelColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(end = 3.dp, bottom = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                draggingSquare?.let { sq ->
                    val piece = state.pieces[sq]
                    if (piece != null) {
                        val fromFile = sq[0] - 'a'
                        val fromRank = sq[1] - '1'

                        val startX = if (isPlayerWhite) (fromFile * squareSize.value).dp else ((7 - fromFile) * squareSize.value).dp
                        val startY = if (isPlayerWhite) ((7 - fromRank) * squareSize.value).dp else (fromRank * squareSize.value).dp

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        (with(density) { startX.toPx() } + dragOffset.x).roundToInt(),
                                        (with(density) { startY.toPx() } + dragOffset.y).roundToInt()
                                    )
                                }
                                .size(squareSize),
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = getPiecePainter(piece, pieceStyle)
                            if (painter != null) {
                                Image(
                                    painter = painter,
                                    contentDescription = piece.toString(),
                                    modifier = Modifier
                                        .size((squareSize.value * 1.2f).dp)
                                        .shadow(8.dp, CircleShape)
                                )
                            }
                        }
                    }
                }

                // Top Overlay Canvas for Lichess-Grade Move & Candidate Engine Arrows
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1. Draw last played move arrow if available
                    if (lastMoveHighlight != null && lastMoveHighlight.first.length == 2 && lastMoveHighlight.second.length == 2) {
                        drawMoveArrow(
                            from = lastMoveHighlight.first,
                            to = lastMoveHighlight.second,
                            isPlayerWhite = isPlayerWhite,
                            color = Color(0xFFF7EC74),
                            alpha = 0.75f
                        )
                    }

                    // 2. Draw candidate engine move arrows
                    visualArrows.forEachIndexed { index, (from, to) ->
                        val arrowAlpha = if (index == 0) 0.85f else (0.55f / (index + 0.5f))
                        val arrowColor = if (index == 0) Color(0xDD00E676) else Color(0xBB00B0FF)
                        drawMoveArrow(from, to, isPlayerWhite, color = arrowColor, alpha = arrowAlpha)
                    }
                }
            }
        }
    }
}


@Composable
fun SquareContainer(
    modifier: Modifier = Modifier,
    squareColor: Color,
    highlightOverlayColor: Color,
    threatOverlayColor: Color,
    checkOverlayColor: Color,
    isCheckedKing: Boolean,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(squareColor)
            .background(highlightOverlayColor)
            .background(threatOverlayColor)
            .background(checkOverlayColor)
            .then(
                if (isCheckedKing) Modifier.border(2.dp, Color(0xFFFF1744), RoundedCornerShape(2.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}
@Composable
fun PromotionDialog(
    isWhite: Boolean,
    pieceStyle: String,
    onSelect: (Char) -> Unit,
    onDismiss: () -> Unit
) {
    val pieces = if (isWhite) listOf('Q', 'R', 'B', 'N') else listOf('q', 'r', 'b', 'n')
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                pieces.forEach { piece ->
                    val painter = getPiecePainter(piece, pieceStyle)
                    if (painter != null) {
                        Image(
                            painter = painter,
                            contentDescription = piece.toString(),
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onSelect(piece) }
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.drawMoveArrow(from: String, to: String, isPlayerWhite: Boolean, color: Color, alpha: Float = 0.8f) {
    val fromFile = from[0] - 'a'
    val fromRank = from[1] - '1'
    val toFile = to[0] - 'a'
    val toRank = to[1] - '1'

    val squareSize = size.width / 8f

    fun getCenter(file: Int, rank: Int): Offset {
        val f = if (isPlayerWhite) file else 7 - file
        val r = if (isPlayerWhite) 7 - rank else rank
        return Offset(f * squareSize + squareSize / 2f, r * squareSize + squareSize / 2f)
    }

    val start = getCenter(fromFile, fromRank)
    val end = getCenter(toFile, toRank)

    val angle = atan2(end.y - start.y, end.x - start.x)
    val headSize = squareSize * 0.35f
    val shaftWidth = squareSize * 0.14f

    // Shorten line slightly so arrow head aligns nicely
    val lineEnd = Offset(
        end.x - (headSize * 0.4f * cos(angle)).toFloat(),
        end.y - (headSize * 0.4f * sin(angle)).toFloat()
    )

    drawLine(
        color = color,
        start = start,
        end = lineEnd,
        strokeWidth = shaftWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
        alpha = alpha
    )

    val arrowPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            (end.x - headSize * cos(angle - 0.45f)).toFloat(),
            (end.y - headSize * sin(angle - 0.45f)).toFloat()
        )
        lineTo(
            (end.x - headSize * cos(angle + 0.45f)).toFloat(),
            (end.y - headSize * sin(angle + 0.45f)).toFloat()
        )
        close()
    }
    drawPath(arrowPath, color = color, alpha = alpha)
}

@Composable
fun InteractiveMoveHistoryRow(
    moveHistory: List<String>,
    moveAnalysis: List<MoveAnalysis> = emptyList(),
    currentMoveIndex: Int = moveHistory.size,
    onSelectMove: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (moveHistory.isEmpty()) return
    val whiteScrollState = rememberScrollState()
    val blackScrollState = rememberScrollState()

    LaunchedEffect(moveHistory.size) {
        whiteScrollState.animateScrollTo(whiteScrollState.maxValue)
        blackScrollState.animateScrollTo(blackScrollState.maxValue)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── TOP ROW: WHITE MOVES ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x1AFFFFFF))
                .horizontalScroll(whiteScrollState)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("♔ White", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            moveHistory.chunked(2).forEachIndexed { moveNumIndex, pair ->
                val moveNumber = moveNumIndex + 1
                val whiteMoveIdx = moveNumIndex * 2
                val isWhiteSelected = currentMoveIndex == whiteMoveIdx
                val whiteAnalysis = moveAnalysis.getOrNull(whiteMoveIdx)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$moveNumber.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    MoveBox(
                        move = pair[0],
                        isSelected = isWhiteSelected,
                        analysis = whiteAnalysis,
                        onClick = { onSelectMove(whiteMoveIdx) }
                    )
                }
            }
        }

        // ── BOTTOM ROW: BLACK MOVES ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x1A000000))
                .horizontalScroll(blackScrollState)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(0.4f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("♚ Black", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            moveHistory.chunked(2).forEachIndexed { moveNumIndex, pair ->
                val moveNumber = moveNumIndex + 1
                val blackMoveIdx = moveNumIndex * 2 + 1
                if (pair.size > 1) {
                    val isBlackSelected = currentMoveIndex == blackMoveIdx
                    val blackAnalysis = moveAnalysis.getOrNull(blackMoveIdx)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("$moveNumber...", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        MoveBox(
                            move = pair[1],
                            isSelected = isBlackSelected,
                            analysis = blackAnalysis,
                            onClick = { onSelectMove(blackMoveIdx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SinglePlayerMoveHistoryRow(
    moveHistory: List<String>,
    moveAnalysis: List<MoveAnalysis> = emptyList(),
    currentMoveIndex: Int = moveHistory.size,
    isWhiteRow: Boolean,
    onSelectMove: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (moveHistory.isEmpty()) return
    val scrollState = rememberScrollState()

    LaunchedEffect(moveHistory.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // .clip(RoundedCornerShape(6.dp))
            // .background(if (isWhiteRow) Color(0x1AFFFFFF) else Color(0x1A000000))
            .horizontalScroll(scrollState)
            // .padding(horizontal = 6.dp, vertical = 4.dp),
        ,horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                // .clip(RoundedCornerShape(4.dp))
                // .background(if (isWhiteRow) MaterialTheme.colorScheme.onSurface.copy(0.2f) else MaterialTheme.colorScheme.onSurface.copy(0.4f))
                // .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isWhiteRow) "♔" else "♚",
                color = if (isWhiteRow) MaterialTheme.colorScheme.onSurface else Color.Gray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        moveHistory.chunked(2).forEachIndexed { moveNumIndex, pair ->
            val moveNumber = moveNumIndex + 1
            if (isWhiteRow) {
                val whiteMoveIdx = moveNumIndex * 2
                val isWhiteSelected = currentMoveIndex == whiteMoveIdx
                val whiteAnalysis = moveAnalysis.getOrNull(whiteMoveIdx)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("$moveNumber.", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    MoveBox(
                        move = pair[0],
                        isSelected = isWhiteSelected,
                        analysis = whiteAnalysis,
                        onClick = { onSelectMove(whiteMoveIdx) }
                    )
                }
            } else {
                if (pair.size > 1) {
                    val blackMoveIdx = moveNumIndex * 2 + 1
                    val isBlackSelected = currentMoveIndex == blackMoveIdx
                    val blackAnalysis = moveAnalysis.getOrNull(blackMoveIdx)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("$moveNumber...", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        MoveBox(
                            move = pair[1],
                            isSelected = isBlackSelected,
                            analysis = blackAnalysis,
                            onClick = { onSelectMove(blackMoveIdx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TwoColumnMoveHistoryTable(
    moveHistory: List<String>,
    moveAnalysis: List<MoveAnalysis> = emptyList(),
    currentMoveIndex: Int = moveHistory.size - 1,
    onSelectMove: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            // .background(Color(0xFF161618), RoundedCornerShape(8.dp))
            // .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            // .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
                // .background(Color(0xFF222225), RoundedCornerShape(4.dp))
                // .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
            Text("", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            moveHistory.chunked(2).forEachIndexed { index, movePair ->
                val moveNum = index + 1
                val whiteIdx = index * 2
                val blackIdx = whiteIdx + 1

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$moveNum.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MoveBox(
                            move = movePair[0],
                            isSelected = currentMoveIndex == whiteIdx,
                            analysis = moveAnalysis.getOrNull(whiteIdx),
                            onClick = { onSelectMove(whiteIdx) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (movePair.size > 1) {
                            MoveBox(
                                move = movePair[1],
                                isSelected = currentMoveIndex == blackIdx,
                                analysis = moveAnalysis.getOrNull(blackIdx),
                                onClick = { onSelectMove(blackIdx) }
                            )
                        } else {
                            Text("-", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveBox(
    move: String,
    isSelected: Boolean,
    analysis: MoveAnalysis?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFFE8883A).copy(alpha = 0.3f) else Color.Transparent)
            .border(
                if (isSelected) 1.dp else 0.dp,
                if (isSelected) Color(0xFFE8883A) else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(move, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (analysis != null) {
                val (color, icon) = getQualityTheme(analysis.quality)
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = analysis.quality.name,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun CapturedPiecesRow(
    fen: String,
    forWhitePlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val capturedPieces = remember(fen, forWhitePlayer) {
        val list = mutableListOf<Char>()
        val boardPart = fen.split(" ").firstOrNull() ?: ""
        if (forWhitePlayer) {
            val initial = mapOf('p' to 8, 'n' to 2, 'b' to 2, 'r' to 2, 'q' to 1)
            val current = mutableMapOf('p' to 0, 'n' to 0, 'b' to 0, 'r' to 0, 'q' to 0)
            boardPart.forEach { ch ->
                if (ch in current.keys) current[ch] = (current[ch] ?: 0) + 1
            }
            initial.forEach { (type, initCount) ->
                val remaining = current[type] ?: 0
                repeat((initCount - remaining).coerceAtLeast(0)) { list.add(type) }
            }
        } else {
            val initial = mapOf('P' to 8, 'N' to 2, 'B' to 2, 'R' to 2, 'Q' to 1)
            val current = mutableMapOf('P' to 0, 'N' to 0, 'B' to 0, 'R' to 0, 'Q' to 0)
            boardPart.forEach { ch ->
                if (ch in current.keys) current[ch] = (current[ch] ?: 0) + 1
            }
            initial.forEach { (type, initCount) ->
                val remaining = current[type] ?: 0
                repeat((initCount - remaining).coerceAtLeast(0)) { list.add(type) }
            }
        }
        list
    }

    val groupedCaptured = remember(capturedPieces) {
        capturedPieces.groupingBy { it }.eachCount()
    }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        groupedCaptured.forEach { (piece, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                val painter = internalPiecePainter(piece)
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (count > 1) {
                    Text("x$count", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun internalPiecePainter(piece: Char, style: String = "cburnett"): androidx.compose.ui.graphics.painter.Painter? {
    return getPiecePainter(piece, style)
}
@Composable
expect fun rememberBoardThumbnailPainter(themeName: String): Painter?