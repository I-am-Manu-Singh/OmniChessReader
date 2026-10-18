package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creativeblisslabs.omnimind.chess.shared.GameAnalysisResult
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.chess.shared.GameSession
import com.creativeblisslabs.omnimind.core.chess.EngineEvaluation
import com.creativeblisslabs.omnimind.core.chess.GameMode
import com.creativeblisslabs.omnimind.core.chess.MoveQuality
import com.creativeblisslabs.omnimind.core.chess.MultiPVLine
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelType
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessGameFullScreen(
    vm: ChessViewModel,
    session: GameSession,
    onExit: () -> Unit
) {
    // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
    val currentFen by vm.fen.collectAsStateWithLifecycle()
    val moveHistory by vm.moveHistory.collectAsStateWithLifecycle()
    val whiteTimer by vm.whiteTimer.collectAsStateWithLifecycle()
    val blackTimer by vm.blackTimer.collectAsStateWithLifecycle()
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val threatMap by vm.threatMap.collectAsStateWithLifecycle()
    val showThreatMap by vm.showThreatMap.collectAsStateWithLifecycle()
    val premove by vm.premove.collectAsStateWithLifecycle()
    val isBotThinking by vm.isBotThinking.collectAsStateWithLifecycle()
    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val showEngineAnalysis by vm.showEngineAnalysis.collectAsStateWithLifecycle()
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val currentMoveIndex by vm.currentMoveIndex.collectAsStateWithLifecycle()
    val gameOverReason by vm.gameOverReason.collectAsStateWithLifecycle()
    val isGameActive by vm.isGameActive.collectAsStateWithLifecycle()
    val isLearningMode by vm.isLearningMode.collectAsStateWithLifecycle()
    val moveAnalysis by vm.moveAnalysis.collectAsStateWithLifecycle()
    val userProfile by vm.currentUserRating.collectAsStateWithLifecycle()
    val enginePacks by vm.enginePacks.collectAsStateWithLifecycle()
  var showWaitingDialog by remember { mutableStateOf(false) }
    val sfPack = remember(enginePacks) {
        enginePacks.find { it.profile == LocalModelProfile.Stockfish17 }
    }
        val activeTimeFormat by vm.activeTimeFormat.collectAsStateWithLifecycle()

        var showAbortedDialog by remember { mutableStateOf(false) }
    val isStockfishDownloaded = sfPack?.isDownloaded == true
    val isDownloadingSF = (sfPack?.downloadProgress ?: 0) > 0 && !isStockfishDownloaded
    val sfProgressFloat = ((sfPack?.downloadProgress ?: 0).toFloat() / 100f).coerceIn(0f, 1f)

    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var pendingPromotionTo by remember { mutableStateOf<String?>(null) }
    var showResignDialog by remember { mutableStateOf(false) }
    var showDrawDialog by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }
    var showAnalysisScreen by remember { mutableStateOf(false) }

    val scheme = MaterialTheme.colorScheme
    val bgColor = scheme.background
    val panelColor = scheme.surfaceContainerHigh
    val accentOrange = scheme.primary

    fun formatMs(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return if (min >= 10) "$min:${sec.toString().padStart(2, '0')}"
        else "$min:${sec.toString().padStart(2, '0')}"
    }

    val playerWhiteTime = formatMs(whiteTimer.remainingTimeMs)
    val playerBlackTime = formatMs(blackTimer.remainingTimeMs)

    val friendOpponentName by vm.friendOpponentName.collectAsStateWithLifecycle()
    val opponentName = when (session.mode) {
        GameMode.VS_BOT -> "Stockfish (Lv ${session.botLevel})"
        GameMode.VS_FRIEND -> friendOpponentName?.ifBlank { "Opponent" } ?: "Opponent"
        else -> "Opponent"
    }
    val rawName = userProfile.username.ifBlank { "You" }
    val playerName = if (rawName == "You") "You" else "$rawName (You)"

    val topName = when (session.mode) {
        GameMode.PUZZLE, GameMode.PUZZLE_STREAK, GameMode.PUZZLE_STORM -> {
            val puzzle = vm.puzzles.collectAsStateWithLifecycle().value.getOrNull(vm.currentPuzzleIndex.collectAsStateWithLifecycle().value)
            if (puzzle != null) "Puzzle Rating: ${puzzle.rating}" else "Chess Puzzle"
        }
        else -> if (isPlayerWhite) opponentName else playerName
    }
    val bottomName = if (isPlayerWhite) playerName else opponentName
    val topTime = if (isPlayerWhite) playerBlackTime else playerWhiteTime
    val bottomTime = if (isPlayerWhite) playerWhiteTime else playerBlackTime

    val isPuzzleMode = session.mode == GameMode.PUZZLE || session.mode == GameMode.PUZZLE_STREAK || session.mode == GameMode.PUZZLE_STORM

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
   
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── LEFT COLUMN: Top/Bottom Player Headers + Centered Board ──
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Header (Back, Match Info, Thinking)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(panelColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val isWaitingForFriend = vm.isWaitingForFriend.value
                                if (isWaitingForFriend) {
                                    vm.resetGame()
                                    onExit()
                                } else if (isGameActive && moveHistory.isEmpty()) {
                                    showAbortedDialog = true
                                } else if (isGameActive && moveHistory.isNotEmpty() && gameOverReason == null) {
                                    showResignDialog = true
                                } else {
                                    onExit()
                                }
                            }) {
                                Icon(Icons.Default.ArrowBackIosNew, null, tint = scheme.onSurface)
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isPuzzleMode) "Puzzle Mode" else "${session.timeFormat} • ${session.label}",
                                color = scheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isBotThinking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = accentOrange)
                            }
                        }

                        // Top Player Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(panelColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.size(26.dp).background(Color(0xFF555558), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(topName.firstOrNull()?.toString() ?: "B", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column {
                                    Text(topName, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    CapturedPiecesRow(fen = currentFen, forWhitePlayer = !isPlayerWhite)
                                }
                            }
                            if (!isPuzzleMode) {
                                val isTopTurn = if (isPlayerWhite) blackTimer.isRunning else whiteTimer.isRunning
                                Box(
                                    modifier = Modifier
                                        .background(if (isTopTurn) scheme.surfaceContainerHigh else scheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(topTime, color = if (isTopTurn) scheme.onSurface else Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        // Centered Board
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            val boardFen = if (gameOverReason != null) ChessLogic.getGameEndKingsFen(gameOverReason!!) else currentFen
                            ChessBoardContainer(
                                fen = boardFen,
                                theme = theme,
                                pieceStyle = pieceStyle,
                                isPlayerWhite = isPlayerWhite,
                                selectedSquare = selectedSquare,
                                premove = premove,
                                showThreatMap = showThreatMap,
                                threatMap = threatMap,
                                visualArrows = visualArrows,
                                engineEvaluation = evaluation,
                                moveHistory = moveHistory,
                                moveAnalysis = moveAnalysis,
                                isRated = session.isRated,
                                onSquareClick = { clicked ->
                                    val cur = selectedSquare
                                    val fenState = ChessLogic.parseFen(currentFen)
                                    val curPiece = fenState.pieces[cur]
                                    val clickedPiece = fenState.pieces[clicked]

                                    if (cur == null) {
                                        if (clickedPiece != null && ((fenState.isWhiteTurn && clickedPiece.isUpperCase()) || (!fenState.isWhiteTurn && !clickedPiece.isUpperCase()))) {
                                            selectedSquare = clicked
                                        }
                                    } else {
                                        if (cur == clicked) {
                                            selectedSquare = null
                                        } else if (clickedPiece != null && ((curPiece?.isUpperCase() == true && clickedPiece.isUpperCase()) || (curPiece?.isUpperCase() == false && !clickedPiece.isUpperCase()))) {
                                            selectedSquare = clicked
                                        } else if (ChessLogic.isValidMove(currentFen, cur, clicked, vm.gameVariant.value)) {
                                            if (ChessLogic.isPromotionMove(currentFen, cur, clicked)) {
                                                pendingPromotionTo = clicked
                                            } else {
                                                vm.makeMove(cur, clicked)
                                                selectedSquare = null
                                            }
                                        } else {
                                            selectedSquare = null
                                        }
                                    }
                                },
                                onPromotionSelect = { promotionPiece ->
                                    val from = selectedSquare
                                    val to = pendingPromotionTo
                                    if (from != null && to != null) vm.makeMove(from, to, promotionPiece)
                                    selectedSquare = null
                                    pendingPromotionTo = null
                                }
                            )

                            if (pendingPromotionTo != null) {
                                PromotionDialog(
                                    isWhite = ChessLogic.parseFen(currentFen).isWhiteTurn,
                                    pieceStyle = pieceStyle,
                                    onSelect = { piece ->
                                        val from = selectedSquare
                                        val to = pendingPromotionTo
                                        if (from != null && to != null) vm.makeMove(from, to, piece)
                                        selectedSquare = null
                                        pendingPromotionTo = null
                                    },
                                    onDismiss = {
                                        selectedSquare = null
                                        pendingPromotionTo = null
                                    }
                                )
                            }
                        }

                        // Bottom Player Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(panelColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.size(26.dp).background(Color(0xFF555558), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(bottomName.firstOrNull()?.toString() ?: "Y", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Column {
                                    Text(bottomName, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    CapturedPiecesRow(fen = currentFen, forWhitePlayer = isPlayerWhite)
                                }
                            }
                            if (!isPuzzleMode) {
                                val isBottomTurn = if (isPlayerWhite) whiteTimer.isRunning else blackTimer.isRunning
                                Box(
                                    modifier = Modifier
                                        .background(if (isBottomTurn) scheme.surfaceContainerHigh else scheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(bottomTime, color = if (isBottomTurn) scheme.onSurface else Color(0xFF888888), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }

                    // ── RIGHT COLUMN: Opening Book, Move History Table & Controls ──
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(panelColor)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val localOpeningInfo by vm.localOpeningInfo.collectAsStateWithLifecycle()
                            localOpeningInfo?.let { opening ->
                                Surface(
                                    color = scheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = if (opening.eco.isNotBlank()) "[${opening.eco}] ${opening.name}" else opening.name,
                                        color = accentOrange,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Vertical Move Table
                            Box(modifier = Modifier.weight(1f)) {
                                ChessVerticalMoveHistoryTable(
                                    moveHistory = moveHistory,
                                    moveAnalysis = moveAnalysis,
                                    currentMoveIndex = currentMoveIndex,
                                    onSelectMove = { idx -> vm.goToMove(idx) }
                                )
                            }
                        }

                        // Bottom Controls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allowUndo = session.mode == GameMode.VS_BOT || session.mode == GameMode.TRAINING
                            val canUndo = moveHistory.isNotEmpty() && allowUndo && isGameActive
                            val canStepBack = currentMoveIndex >= 0
                            val canStepForward = currentMoveIndex < moveHistory.size - 1

                            IconButton(onClick = { showMenuDropdown = true }) {
                                Icon(Icons.Filled.Menu, null, tint = scheme.onSurface)
                            }
                            IconButton(onClick = { vm.stepBackward() }, enabled = canStepBack) {
                                Icon(Icons.Filled.ChevronLeft, null, tint = if (canStepBack) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                            IconButton(onClick = { vm.stepForward() }, enabled = canStepForward) {
                                Icon(Icons.Filled.ChevronRight, null, tint = if (canStepForward) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                            IconButton(onClick = { vm.undoLastMove() }, enabled = canUndo) {
                                Icon(Icons.AutoMirrored.Filled.Undo, null, tint = if (canUndo) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                            IconButton(onClick = { showResignDialog = true }, enabled = gameOverReason == null && isGameActive) {
                                Icon(Icons.Filled.Flag, null, tint = if (gameOverReason == null && isGameActive) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            } else {
            Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.SpaceBetween
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp) // Added top padding here
    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(panelColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val isWaitingForFriend = vm.isWaitingForFriend.value
                                if (isWaitingForFriend) {
                                    vm.resetGame()
                                    onExit()
                                } else if (isGameActive && moveHistory.isEmpty()) {
                                    showAbortedDialog = true
                                } else if (isGameActive && moveHistory.isNotEmpty() && gameOverReason == null) {
                                    showResignDialog = true
                                } else {
                                    onExit()
                                }
                            }) {
                                Icon(Icons.Default.ArrowBackIosNew, null, tint = scheme.onSurface)
                            }
                            Spacer(Modifier.width(8.dp))
                            if (isPuzzleMode) {
                                val puzzle = vm.puzzles.collectAsStateWithLifecycle().value.getOrNull(vm.currentPuzzleIndex.collectAsStateWithLifecycle().value)
                                Text(
                                    text = puzzle?.instruction ?: "Chess Puzzles",
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = "${session.timeFormat} • ${session.label}",
                                    color = scheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (isBotThinking) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFFF9800), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = scheme.onSurface
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("thinking...", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(panelColor)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(Color(0xFF555558), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(topName.firstOrNull()?.toString() ?: "B", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column {
                                    Text(topName, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    CapturedPiecesRow(fen = currentFen, forWhitePlayer = !isPlayerWhite)
                                }
                            }
                            if (!isPuzzleMode) {
                                val isTopTurn = if (isPlayerWhite) blackTimer.isRunning else whiteTimer.isRunning
                                Box(
                                    modifier = Modifier
                                        .background(if (isTopTurn) scheme.surfaceContainerHigh else scheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = topTime,
                                        color = if (isTopTurn) scheme.onSurface else Color(0xFF888888),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                        val localOpeningInfo by vm.localOpeningInfo.collectAsStateWithLifecycle()
                        localOpeningInfo?.let { opening ->
                            Surface(
                                color = scheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (opening.eco.isNotBlank()) "[${opening.eco}] ${opening.name}" else opening.name,
                                    color = accentOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (moveHistory.isNotEmpty()) {
                            SinglePlayerMoveHistoryRow(
                                moveHistory = moveHistory,
                                moveAnalysis = moveAnalysis,
                                currentMoveIndex = currentMoveIndex,
                                isWhiteRow = !isPlayerWhite,
                                onSelectMove = { idx -> vm.goToMove(idx) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val boardFen = if (gameOverReason != null) {
                            ChessLogic.getGameEndKingsFen(gameOverReason!!)
                        } else {
                            currentFen
                        }
                        ChessBoardContainer(
                            fen = boardFen,
                            theme = theme,
                            pieceStyle = pieceStyle,
                            isPlayerWhite = isPlayerWhite,
                            selectedSquare = selectedSquare,
                            premove = premove,
                            showThreatMap = showThreatMap,
                            threatMap = threatMap,
                            visualArrows = visualArrows,
                            engineEvaluation = evaluation,
                            moveHistory = moveHistory,
                            moveAnalysis = moveAnalysis,
                            isRated = session.isRated,
                            onSquareClick = { clicked ->
                                val cur = selectedSquare
                                val fenState = ChessLogic.parseFen(currentFen)
                                val curPiece = fenState.pieces[cur]
                                val clickedPiece = fenState.pieces[clicked]

                                if (cur == null) {
                                    if (clickedPiece != null) {
                                        if ((fenState.isWhiteTurn && clickedPiece.isUpperCase()) || (!fenState.isWhiteTurn && !clickedPiece.isUpperCase())) {
                                            selectedSquare = clicked
                                        }
                                    }
                                } else {
                                    if (cur == clicked) {
                                        selectedSquare = null
                                    } else if (clickedPiece != null && ((curPiece?.isUpperCase() == true && clickedPiece.isUpperCase()) || (curPiece?.isUpperCase() == false && !clickedPiece.isUpperCase()))) {
                                        selectedSquare = clicked
                                    } else if (ChessLogic.isValidMove(currentFen, cur, clicked, vm.gameVariant.value)) {
                                        if (ChessLogic.isPromotionMove(currentFen, cur, clicked)) {
                                            pendingPromotionTo = clicked
                                        } else {
                                            vm.makeMove(cur, clicked)
                                            selectedSquare = null
                                        }
                                    } else {
                                        selectedSquare = null
                                    }
                                }
                            },
                            onPromotionSelect = { promotionPiece ->
                                val from = selectedSquare
                                val to = pendingPromotionTo
                                if (from != null && to != null) {
                                    vm.makeMove(from, to, promotionPiece)
                                }
                                selectedSquare = null
                                pendingPromotionTo = null
                            }
                        )

                        if (pendingPromotionTo != null) {
                            PromotionDialog(
                                isWhite = ChessLogic.parseFen(currentFen).isWhiteTurn,
                                pieceStyle = pieceStyle,
                                onSelect = { piece ->
                                    val from = selectedSquare
                                    val to = pendingPromotionTo
                                    if (from != null && to != null) {
                                        vm.makeMove(from, to, piece)
                                    }
                                    selectedSquare = null
                                    pendingPromotionTo = null
                                },
                                onDismiss = {
                                    selectedSquare = null
                                    pendingPromotionTo = null
                                }
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(panelColor)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(Color(0xFF555558), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(bottomName.firstOrNull()?.toString() ?: "Y", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Column {
                                    Text(bottomName, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                    CapturedPiecesRow(fen = currentFen, forWhitePlayer = isPlayerWhite)
                                }
                            }
                            if (isPuzzleMode) {
                                if (session.mode == GameMode.PUZZLE_STORM) {
                                    val timeLeft by vm.puzzleStormTimeLeft.collectAsStateWithLifecycle()
                                    val score by vm.puzzleStormScore.collectAsStateWithLifecycle()
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Score: $score", color = accentOrange, fontWeight = FontWeight.Bold)
                                        Text("Time: $timeLeft", color = scheme.onSurface)
                                    }
                                } else if (session.mode == GameMode.PUZZLE_STREAK) {
                                    val streak by vm.puzzleStreak.collectAsStateWithLifecycle()
                                    Text("Streak: $streak", color = accentOrange, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val isBottomTurn = if (isPlayerWhite) whiteTimer.isRunning else blackTimer.isRunning
                                Box(
                                    modifier = Modifier
                                        .background(if (isBottomTurn) scheme.surfaceContainerHigh else scheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = bottomTime,
                                        color = if (isBottomTurn) scheme.onSurface else Color(0xFF888888),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }

                        if (moveHistory.isNotEmpty()) {
                            SinglePlayerMoveHistoryRow(
                                moveHistory = moveHistory,
                                moveAnalysis = moveAnalysis,
                                currentMoveIndex = currentMoveIndex,
                                isWhiteRow = isPlayerWhite,
                                onSelectMove = { idx -> vm.goToMove(idx) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surfaceContainerHigh)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isLearningMode) {
                                Button(
                                    onClick = {
                                        vm.exitLearningMode()
                                        showAnalysisScreen = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Back to Analysis", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val allowUndo = session.mode == GameMode.VS_BOT || session.mode == GameMode.TRAINING
                                val canUndo = moveHistory.isNotEmpty() && allowUndo && isGameActive
                                val canStepBack = currentMoveIndex >= 0
                                val canStepForward = currentMoveIndex < moveHistory.size - 1

                                IconButton(onClick = { showMenuDropdown = true }) {
                                    Icon(Icons.Filled.Menu, null, tint = scheme.onSurface)
                                }
                                IconButton(onClick = { vm.stepBackward() }, enabled = canStepBack) {
                                    Icon(Icons.Filled.ChevronLeft, null, tint = if (canStepBack) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                IconButton(onClick = { vm.stepForward() }, enabled = canStepForward) {
                                    Icon(Icons.Filled.ChevronRight, null, tint = if (canStepForward) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                IconButton(onClick = { vm.undoLastMove() }, enabled = canUndo) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, null, tint = if (canUndo) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                IconButton(onClick = { showResignDialog = true }, enabled = gameOverReason == null && isGameActive) {
                                    Icon(Icons.Filled.Flag, null, tint = if (gameOverReason == null && isGameActive) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
            if (showMenuDropdown) {
                AlertDialog(
                    onDismissRequest = { showMenuDropdown = false },
                    containerColor = scheme.surfaceContainerHigh,
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.SportsEsports, null, tint = accentOrange)
                            Text("Game Options & Menu", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val hasMovesPlayed = moveHistory.isNotEmpty()

                            if (!isGameActive) {
                                Button(
                                    onClick = {
                                        showMenuDropdown = false
                                        vm.startRematch()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                                ) {
                                    Icon(Icons.Filled.Replay, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Rematch", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (hasMovesPlayed && isGameActive) {
                                OutlinedButton(
                                    onClick = {
                                        showMenuDropdown = false
                                        showDrawDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Handshake, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Offer Draw", color = scheme.onSurface)
                                }
                            }

                            if (isGameActive && hasMovesPlayed) {
                                OutlinedButton(
                                    onClick = {
                                        showMenuDropdown = false
                                        showResignDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Icon(Icons.Filled.Flag, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Resign Game", color = Color.Red)
                                }
                            } else if (isGameActive && !hasMovesPlayed) {
                                OutlinedButton(
                                    onClick = {
                                        showMenuDropdown = false
                                        showAbortedDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEB5757))
                                ) {
                                    Icon(Icons.Filled.Close, null, tint = Color(0xFFEB5757), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Abort Game", color = Color(0xFFEB5757))
                                }
                            }

                            if (hasMovesPlayed) {
                                OutlinedButton(
                                    onClick = {
                                        showMenuDropdown = false
                                        vm.runGameAnalysis()
                                        showAnalysisScreen = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Analytics, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Run Game Analysis", color = scheme.onSurface)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    showMenuDropdown = false
                                    vm.flipBoard()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.SwapVert, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Flip Board", color = scheme.onSurface)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showMenuDropdown = false }) {
                            Text("Close", color = scheme.onSurfaceVariant)
                        }
                    }
                )
            }

            if (!showAnalysisScreen) {
                gameOverReason?.let { reason ->
                    val analysisResult by vm.gameAnalysisResult.collectAsStateWithLifecycle()
                    AlertDialog(
                        onDismissRequest = { vm.dismissGameOverDialog() },
                        containerColor = scheme.surfaceContainerHigh,
                        title = {
                            Column {
                                Text("Game Over", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                analysisResult?.let { result ->
                                    val delta = result.ratingDelta
                                    val deltaText = if (delta >= 0) "+$delta" else "$delta"
                                    val deltaColor = if (delta >= 0) Color.Green else Color.Red
                                    Text(
                                        text = "Rating: $deltaText",
                                        color = deltaColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        },
                        text = {
                            Column {
                                Text(reason, color = scheme.onSurfaceVariant)
                                analysisResult?.let { result ->
                                    Spacer(Modifier.height(8.dp))
                                    Text("Accuracy: ${result.accuracy.toInt()}%", color = accentOrange, fontSize = 14.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                vm.runGameAnalysis()
                                vm.dismissGameOverDialog()
                                showAnalysisScreen = true
                            }) { Text("Review Game & Analysis") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                vm.dismissGameOverDialog()
                                onExit()
                            }) { Text("Exit to Lobby", color = scheme.onSurfaceVariant) }
                        }
                    )
                }
            }

            if (showResignDialog) {
                AlertDialog(
                    onDismissRequest = { showResignDialog = false },
                    containerColor = scheme.surfaceContainerHigh,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Flag, null, tint = Color.Red)
                            Text("Resign Match?", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Text(
                            "Are you sure you want to resign? Your opponent will be awarded the victory.",
                            color = scheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showResignDialog = false
                                vm.resignGame()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Resign Game", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResignDialog = false }) {
                            Text("Cancel", color = scheme.onSurfaceVariant)
                        }
                    }
                )
            }

            val isWaitingForFriend by vm.isWaitingForFriend.collectAsStateWithLifecycle()
            val inviteInfo by vm.friendInviteInfo.collectAsStateWithLifecycle()

            if (isWaitingForFriend || showWaitingDialog) {
                Dialog(onDismissRequest = { showWaitingDialog = false }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.surfaceContainer,
                        tonalElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = accentOrange,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )

                            Text(
                                "Waiting for friend to join...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = scheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.LocalFireDepartment,
                                    null,
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    activeTimeFormat,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = scheme.onSurface
                                )
                            }

                            if (inviteInfo != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = scheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "Room Code",
                                            fontSize = 11.sp,
                                            color = scheme.onSurfaceVariant
                                        )
                                        // ── Big short code ──
                                        Text(
                                            text = inviteInfo!!.shortCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 36.sp,
                                            color = accentOrange,
                                            letterSpacing = 6.sp
                                        )
                                        Text(
                                            "Friend enters this code to join",
                                            fontSize = 10.sp,
                                            color = scheme.onSurfaceVariant
                                        )

                                        Button(
                                            onClick = {
                                                vm.shareInviteLink()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Share, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Share Full Link", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            TextButton(
                                onClick = {
                                    showWaitingDialog = false
                                    vm.resetGame()
                                    onExit()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray)
                            ) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            if (showAbortedDialog) {
                AlertDialog(
                    onDismissRequest = { showAbortedDialog = false },
                    containerColor = scheme.surfaceContainerHigh,
                    title = {
                        Text(
                            text = "Abort Game?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = scheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            text = "No moves have been made yet. Aborting will cancel this game room without rating penalty.",
                            color = scheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                vm.resetGame()
                                showAbortedDialog = false
                                onExit()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEB5757)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Abort & Exit", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAbortedDialog = false }) {
                            Text("Cancel", color = scheme.onSurfaceVariant)
                        }
                    }
                )
            }
            if (showDrawDialog) {
                AlertDialog(
                    onDismissRequest = { showDrawDialog = false },
                    containerColor = scheme.surfaceContainerHigh,
                    title = { Text("Offer Draw?", color = scheme.onSurface, fontWeight = FontWeight.Bold) },
                    text = { Text("Offer a draw to your opponent?", color = scheme.onSurfaceVariant) },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDrawDialog = false
                                vm.offerDraw()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                        ) { Text("Offer Draw") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDrawDialog = false }) { Text("Cancel", color = scheme.onSurfaceVariant) }
                    }
                )

            if (!isStockfishDownloaded && (session.mode == GameMode.VS_BOT || showEngineAnalysis)) {
                AlertDialog(
                    onDismissRequest = { },
                    containerColor = scheme.surfaceContainerHigh,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Download, null, tint = accentOrange)
                            Text("Stockfish 17 Engine Required", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "To play against Stockfish AI or access engine evaluation, you need to download Stockfish 17.",
                                color = scheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            if (isDownloadingSF) {
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { sfProgressFloat },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = accentOrange
                                )
                                Text("Downloading... ${sfPack?.downloadProgress ?: 0}%", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        if (!isDownloadingSF) {
                            Button(
                                onClick = { vm.downloadStockfish() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                            ) {
                                Text("Download Stockfish 17", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onExit() }) {
                            Text("Exit to Lobby", color = scheme.onSurfaceVariant)
                        }
                    }
                )
            }

            if (showAnalysisScreen) {
                Dialog(
                    onDismissRequest = { showAnalysisScreen = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
                ) {
                    ChessAnalysisScreen(
                        vm = vm,
                        onBack = { showAnalysisScreen = false }
                    )
                }
            }
        }
    }
}
}
@Composable
fun MultiPVPanel(
    evaluation: EngineEvaluation?,
    modifier: Modifier = Modifier
) {
        val scheme = MaterialTheme.colorScheme

    if (evaluation == null) return

    Column(
        modifier = modifier
            .background(scheme.surfaceContainerLow, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            "Engine Analysis (Depth ${evaluation.depth})",
            color = scheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        val allLines = if (evaluation.multiPV.isNotEmpty()) evaluation.multiPV
        else listOf(MultiPVLine(evaluation.score, evaluation.bestLine, evaluation.mate))

        allLines.take(3).forEachIndexed { index, pv ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val scoreText = when {
                    pv.mate != null -> if (pv.mate!! > 0) "+M${pv.mate}" else "-M${abs(pv.mate!!)}"
                    pv.score >= 0 -> "+${(kotlin.math.round(pv.score * 10) / 10.0)}"
                    else -> "${(kotlin.math.round(pv.score * 10) / 10.0)}"
                }

                Text(
                    text = scoreText,
                    color = if (index == 0) scheme.primary else scheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.width(52.dp)
                )

                val sanLine = com.creativeblisslabs.omnimind.core.chess.ChessLogic.convertUciLineToSan(evaluation.bestLine.firstOrNull() ?: "", pv.bestLine)
                Text(
                    text = if (sanLine.isNotBlank()) sanLine else pv.bestLine.joinToString(" "),
                    color = scheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EvaluationBar(
    evaluation: EngineEvaluation?,
    isPlayerWhite: Boolean,
    modifier: Modifier = Modifier
) {
    val scoreVal = evaluation?.score ?: 0.0
    val mateVal = evaluation?.mate
    val scheme = MaterialTheme.colorScheme

    // 1. Calculate evaluation bar percentages (0.05 to 0.95 range for smooth visibility)
    val rawWhitePercentage = when {
        mateVal != null -> if (mateVal > 0) 0.95f else 0.05f
        else -> {
            val normalizedScore = scoreVal.coerceIn(-10.0, 10.0)
            ((normalizedScore + 10.0) / 20.0).toFloat().coerceIn(0.05f, 0.95f)
        }
    }
    val displayPercentage = if (isPlayerWhite) rawWhitePercentage else (1.0f - rawWhitePercentage)
    val animatedPercentage by animateFloatAsState(
        targetValue = displayPercentage,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "EvalBarAnimation"
    )

    // 2. Generate the formatted text string
    val evalText = when {
        mateVal != null -> if (mateVal > 0) "M$mateVal" else "-M${abs(mateVal)}"
        scoreVal > 0 -> "+${(scoreVal * 10).roundToInt() / 10.0}"
        scoreVal < 0 -> "${(scoreVal * 10).roundToInt() / 10.0}"
        else -> "0.0"
    }

    // Dynamic color assignments based on your theme palette
    val whitePieceColor = scheme.inverseOnSurface // Always soft milk/ivory cream
    val blackPieceColor = scheme.inverseSurface   // Always deep espresso dark chocolate
    val barBorderColor  = scheme.outline

    // Outer vertical bar container (Defaults to the Black position backdrop)
    Box(
        modifier = modifier
            .width(22.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(blackPieceColor) 
            .border(1.dp, barBorderColor, RoundedCornerShape(6.dp))
    ) {
        // White evaluation portion sliding on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedPercentage)
                .align(if (isPlayerWhite) Alignment.BottomCenter else Alignment.TopCenter)
                .background(whitePieceColor)
        )

        // Center baseline divider (Using your Primary caramel accent instead of hardcoded orange)
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            thickness = 1.5.dp,
            color = scheme.primary
        )

        // Determine if text sits inside White or Black territory to ensure perfect contrast
        val isTextInWhiteTerritory = if (isPlayerWhite) animatedPercentage > 0.5f else animatedPercentage < 0.5f

        // High-contrast score label layout
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            contentAlignment = if (animatedPercentage > 0.5f) {
                if (isPlayerWhite) Alignment.BottomCenter else Alignment.TopCenter
            } else {
                if (isPlayerWhite) Alignment.TopCenter else Alignment.BottomCenter
            }
        ) {
            Text(
                text = evalText,
                fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                // If text sits on White, use Dark Chocolate text. If on Black, use Cream text.
                color = if (isTextInWhiteTerritory) scheme.onSurface else scheme.inverseOnSurface,
                maxLines = 1
            )
        }
    }
}
@Composable
fun PostGameAnalysisPanel(result: GameAnalysisResult, moveHistory: List<String>) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Game Review",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary
                        )
                        val clampedAcc = (result.accuracy * 10).roundToInt() / 10.0
                        Text(
                            "Accuracy: ${clampedAcc}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = scheme.onSurface
                        )
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                        CircularProgressIndicator(
                            progress = { (result.accuracy / 100f).toFloat() },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 6.dp,
                            color = getAccuracyColor(result.accuracy),
                            trackColor = scheme.onSurface.copy(alpha = 0.1f)
                        )
                        Text(
                            "${result.accuracy.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RatingEstimateItem("Player", result.estimatedElo, Icons.Default.Person, scheme.primary)
                    VerticalDivider(modifier = Modifier.height(40.dp), color = scheme.onSurface.copy(alpha = 0.1f))
                    RatingEstimateItem("Opponent", (result.estimatedElo * 0.95).toInt(), Icons.Default.SmartToy, Color.Gray)
                }
            }
        }

        Text("Evaluation Graph", style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
        GlassCard(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            Box(modifier = Modifier.padding(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val points = result.evaluationGraph
                    if (points.isNotEmpty()) {
                        val dx = size.width / (points.size - 1).coerceAtLeast(1)
                        val midY = size.height / 2

                        val fillPath = Path().apply {
                            moveTo(0f, midY)
                            points.forEachIndexed { idx, value ->
                                val py = midY - (value.coerceIn(-10.0, 10.0) / 10.0 * midY).toFloat()
                                lineTo(idx * dx, py)
                            }
                            lineTo(size.width, midY)
                            close()
                        }
                        drawPath(fillPath, brush = Brush.verticalGradient(listOf(Color(0x4400FFCC), Color.Transparent)), alpha = 0.3f)

                        val path = Path()
                        points.forEachIndexed { idx, value ->
                            val py = midY - (value.coerceIn(-10.0, 10.0) / 10.0 * midY).toFloat()
                            val px = idx * dx
                            if (idx == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        drawPath(path, color = Color(0xFF00FFCC), style = Stroke(width = 3f))

                        drawLine(scheme.onSurface.copy(alpha = 0.2f), Offset(0f, midY), Offset(size.width, midY), strokeWidth = 1.dp.toPx())
                    }
                }
            }
        }

        Text("Move Classifications", style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)

        val qualities = listOf(
            MoveQuality.BRILLIANT, MoveQuality.GREAT, MoveQuality.BEST,
            MoveQuality.EXCELLENT, MoveQuality.GOOD, MoveQuality.BOOK,
            MoveQuality.INACCURACY, MoveQuality.MISTAKE, MoveQuality.BLUNDER, MoveQuality.FORCED
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            qualities.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { quality ->
                        ClassificationItem(
                            quality = quality,
                            count = result.counts[quality] ?: 0,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingEstimateItem(label: String, elo: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
       val scheme = MaterialTheme.colorScheme

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Column {
            Text(label, fontSize = 11.sp, color = scheme.onSurfaceVariant)
            Text("$elo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = scheme.onSurface)
        }
    }
}

@Composable
fun ClassificationItem(quality: MoveQuality, count: Int, modifier: Modifier = Modifier) {
    val (color, icon) = getQualityTheme(quality)
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        color = Color(0x1A888888),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(quality.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                Text("$count", fontSize = 11.sp, color = scheme.onSurfaceVariant)
            }
        }
    }
}

fun getAccuracyColor(accuracy: Double): Color = when {
    accuracy >= 90 -> Color(0xFF00E676)
    accuracy >= 70 -> Color(0xFFD4AF37)
    accuracy >= 50 -> Color(0xFFFF9100)
    else -> Color(0xFFFF1744)
}

fun getQualityTheme(quality: MoveQuality): Pair<Color, androidx.compose.ui.graphics.vector.ImageVector> = when (quality) {
    MoveQuality.BRILLIANT -> Color(0xFF00C8FF) to Icons.Default.AutoAwesome
    MoveQuality.GREAT -> Color(0xFF00E676) to Icons.Default.Star
    MoveQuality.BEST -> Color(0xFF9CCC65) to Icons.Default.CheckCircle
    MoveQuality.EXCELLENT -> Color(0xFF81C784) to Icons.Default.Check
    MoveQuality.GOOD -> Color(0xFFAED581) to Icons.Default.ThumbUp
    MoveQuality.BOOK -> Color(0xFFD4AF37) to Icons.AutoMirrored.Filled.MenuBook
    MoveQuality.INACCURACY -> Color(0xFFFFD600) to Icons.Default.QuestionMark
    MoveQuality.MISTAKE -> Color(0xFFFF9100) to Icons.Default.PriorityHigh
    MoveQuality.BLUNDER -> Color(0xFFFF1744) to Icons.Default.Close
    MoveQuality.FORCED -> Color(0xFF90A4AE) to Icons.Default.Lock
}
