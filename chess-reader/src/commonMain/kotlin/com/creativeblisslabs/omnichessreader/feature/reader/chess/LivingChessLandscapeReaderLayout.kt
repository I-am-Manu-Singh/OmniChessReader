package com.creativeblisslabs.omnichessreader.feature.reader.chess

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.creativeblisslabs.omnichessreader.core.media.PdfDiagramPosition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.creativeblisslabs.omnichessreader.core.data.ai.LocalModelProfile
import com.creativeblisslabs.omnichessreader.core.ui.rememberFilePicker
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.creativeblisslabs.omnichessreader.feature.chess.ChessAnalysisScreen
import com.creativeblisslabs.omnichessreader.core.chess.ChessLogic
import com.creativeblisslabs.omnichessreader.core.chess.EngineEvaluation
import com.creativeblisslabs.omnichessreader.feature.chess.ChessBoardContainer
import com.creativeblisslabs.omnichessreader.feature.chess.ChessViewModel
import com.creativeblisslabs.omnichessreader.feature.chess.ChessVerticalMoveHistoryTable
import com.creativeblisslabs.omnichessreader.feature.chess.LichessEngineLines
import com.creativeblisslabs.omnichessreader.feature.chess.EvaluationBar
import com.creativeblisslabs.omnichessreader.feature.chess.InteractiveMoveHistoryRow
import com.creativeblisslabs.omnichessreader.chess.shared.ChessTheme
import com.creativeblisslabs.omnichessreader.feature.reader.chess.LivingChessPlacement
import com.creativeblisslabs.omnichessreader.feature.reader.chess.ChessColor
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/**
 * LivingChessLandscapeReaderLayout
 * 3-Column Landscape UI matching living-chess-book:
 * 1. Top Bar: Hint banner ("Click a colored move on the page, and the board goes to that position...")
 * 2. Left Column: PDF Book Page (rendered original PDF page image or text) with interactive move and diagram click overlays.
 * 3. Middle Column: Interactive 8x8 Chess Board with navigation controls (←, →, ⇅, Turn Dot, ★ Save, Engine, Guess the move).
 *    Bottom of Middle Column: Next step progress ("The diagram on page X" with [Show it] [Solve]).
 * 4. Right Column: Study Notebook with tabs (All, This page, Positions), notes list, and "Write a note" / [Take note].
 */
@Composable
fun LivingChessLandscapeReaderLayout(
    pageNumber: Int,
    totalPages: Int = 1,
    pageContentText: String,
    pageImagePath: String? = null,
    detectedDiagrams: List<DetectedDiagram> = emptyList(),
    recognizedMoves: List<String> = emptyList(),
    currentFen: String = "",
    chapters: List<LivingChessChapter> = emptyList(),
    allBookDiagrams: Map<Int, List<PdfDiagramPosition>> = emptyMap(),
    onJumpToPage: (Int) -> Unit = {},
    onPrevPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onMoveSelected: (String) -> Unit = {},
    onDiagramSelected: (DetectedDiagram) -> Unit = {},
    onTakeNote: (String) -> Unit = {},
    onSolveDiagram: (DetectedDiagram) -> Unit = {},
    onShowNextDiagram: (Int) -> Unit = {},
    onToggleLayoutMode: () -> Unit = {},
    onExportDiagnosticLog: () -> Unit = {},
    onCloseNotebook: () -> Unit = {},
    onOpenEngineSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chessVm: ChessViewModel = koinViewModel()
    val vmFen by chessVm.fen.collectAsStateWithLifecycle()
    val engineEvaluation by chessVm.evaluation.collectAsStateWithLifecycle()
    val engineVisualArrows by chessVm.visualArrows.collectAsStateWithLifecycle()
    val moveHistory by chessVm.moveHistory.collectAsStateWithLifecycle()
    val moveAnalysis by chessVm.moveAnalysis.collectAsStateWithLifecycle()
    val currentMoveIndex by chessVm.currentMoveIndex.collectAsStateWithLifecycle()
    val userProfile by chessVm.currentUserRating.collectAsStateWithLifecycle()
    val enginePacks by chessVm.enginePacks.collectAsStateWithLifecycle()
    val sfPack = remember(enginePacks) {
        enginePacks.find { it.profile == LocalModelProfile.Stockfish17 }
    }
    val isStockfishDownloaded = sfPack?.isDownloaded == true
    val isDownloadingSF = (sfPack?.downloadProgress ?: 0) > 0 && !isStockfishDownloaded
    val sfProgressFloat = ((sfPack?.downloadProgress ?: 0).toFloat() / 100f).coerceIn(0f, 1f)
    var showEngineDownloadDialog by remember { mutableStateOf(false) }
    var showFullAnalysisDialog by remember { mutableStateOf(false) }

    val filePicker = rememberFilePicker { uri ->
        chessVm.importLocalModel(uri, LocalModelProfile.Stockfish17)
    }

    val threatMap by chessVm.threatMap.collectAsStateWithLifecycle()
    var showThreatMode by remember { mutableStateOf(false) }
    var showGraphMode by remember { mutableStateOf(false) }
    var numEngineLines by remember { mutableStateOf(3) }
    var boardFen by remember(currentFen) {
        val initial = if (currentFen.contains("/")) {
            val placement = currentFen.split(" ").firstOrNull() ?: currentFen
            val parts = currentFen.split(" ")
            if (parts.size >= 6) currentFen
            else LivingChessPlacement.fenFromDiagram(placement, ChessColor.WHITE)
        } else ChessLogic.getInitialFen()
        mutableStateOf(initial)
    }

    // Keep boardFen in sync with ViewModel's fen state when user navigates moves via [|<] [<] [>] [>|]
    LaunchedEffect(vmFen) {
        if (vmFen.isNotBlank() && vmFen != boardFen) {
            boardFen = vmFen
        }
    }

    // When recognizedMoves or initial position changes, populate ChessViewModel's moveHistory
    LaunchedEffect(recognizedMoves, currentFen) {
        if (recognizedMoves.isNotEmpty()) {
            val startFen = if (currentFen.isNotBlank() && currentFen.contains("/")) {
                val parts = currentFen.split(" ")
                if (parts.size >= 6) currentFen
                else LivingChessPlacement.fenFromDiagram(parts[0], ChessColor.WHITE)
            } else ChessLogic.getInitialFen()
            chessVm.loadMovesFromPosition(startFen, recognizedMoves)
        } else if (boardFen.isNotBlank()) {
            chessVm.updateFen(boardFen)
        }
    }
    var isBoardFlipped by remember { mutableStateOf(false) }
    var selectedNotebookTab by remember { mutableStateOf("Analysis") }
    var noteInputText by remember { mutableStateOf("") }
    var isEngineActive by remember(isStockfishDownloaded) { mutableStateOf(isStockfishDownloaded) }
    var isGuessActive by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var moveIndex by remember { mutableStateOf(0) }
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var isDiagramOverlayExpanded by remember(pageNumber) { mutableStateOf(true) }
    var isMaskSolutionsActive by remember { mutableStateOf(false) }
    var guessFeedbackText by remember { mutableStateOf<String?>(null) }
    var guessFeedbackColor by remember { mutableStateOf(Color.White) }
    val notes = remember { mutableStateListOf<String>() }

    // Page zoom & pan transformation states with boundary resistance
    var pageZoomScale by remember(pageNumber) { mutableStateOf(1f) }
    var pagePanOffset by remember(pageNumber) { mutableStateOf(Offset.Zero) }

    // Quick jump page dialog state
    var showJumpPageDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }

    // Navigation drawer state (All Positions & Chapters)
    var showNavSidebar by remember { mutableStateOf(false) }
    var navSidebarTab by remember { mutableStateOf("Positions") } // "Positions" or "Chapters"
    var positionSearchQuery by remember { mutableStateOf("") }

    // Unified display history for middle board and right notebook
    val displayHistory = remember(moveHistory, recognizedMoves) {
        if (moveHistory.isNotEmpty()) moveHistory else recognizedMoves
    }
    val displayCurrentIndex = if (moveHistory.isNotEmpty()) currentMoveIndex else moveIndex

    // Keep ChessViewModel's analysis position synchronized with the current reader board FEN
    // Debounced by 300ms so that fast clicks or navigating moves doesn't spawn back-to-back engine analysis and freeze the UI
    LaunchedEffect(boardFen, isEngineActive) {
        if (isEngineActive) {
            kotlinx.coroutines.delay(300)
            chessVm.exitGameSession() // ensure not in vs-bot blocked state
            chessVm.updateFen(boardFen)
            chessVm.analyzePosition()
        }
    }

    // Pane widths weight allocation (Pane 1: Left Book, Pane 2: Middle Board, Pane 3: Right Notebook)
    var pane1Weight by remember { mutableStateOf(1.05f) }
    var pane2Weight by remember { mutableStateOf(1.15f) }
    var pane3Weight by remember { mutableStateOf(0.95f) }

    val defaultChessTheme = remember {
        ChessTheme("chesscom green", 0xFFEEEED2, 0xFF769656, false)
    }

    val darkBg = Color(0xFF131815)
    val panelBg = Color(0xFF1B231F)
    val emeraldAccent = Color(0xFF4EBA87)
    val mintHighlight = Color(0xFF70DEAB)
    val amberHighlight = Color(0xFFFFD54F)

    val isWhiteTurn = remember(boardFen) {
        val parts = boardFen.split(" ")
        if (parts.size > 1) parts[1] == "w" else true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        

        // ── 3-COLUMN MAIN AREA ──
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ════════════════════════════════════════════════════════════════
            // COLUMN 1: ORIGINAL PDF / RENDERED BOOK PAGE WITH OVERLAYS
            // ════════════════════════════════════════════════════════════════
            Surface(
                modifier = Modifier
                    .weight(pane1Weight)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    // Header of page with page switcher, jump input, progress bar & sidebar trigger
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Sidebar Drawer Trigger (Browse Positions & Chapters)
                            IconButton(
                                onClick = { showNavSidebar = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "Positions & Chapters Sidebar",
                                    tint = emeraldAccent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = onPrevPage,
                                enabled = pageNumber > 1,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Page", tint = if (pageNumber > 1) Color.Black else Color.LightGray, modifier = Modifier.size(16.dp))
                            }

                            // Clickable page number text to open Quick Jump Dialog
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        jumpPageInput = pageNumber.toString()
                                        showJumpPageDialog = true
                                    },
                                color = Color(0xFFF0F4F2)
                            ) {
                                Text(
                                    text = "Page $pageNumber / $totalPages",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E3A2B),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            IconButton(
                                onClick = onNextPage,
                                enabled = pageNumber < totalPages,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Page", tint = if (pageNumber < totalPages) Color.Black else Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isMaskSolutionsActive = !isMaskSolutionsActive
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isMaskSolutionsActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Hide/Reveal Solutions Mask",
                                    tint = if (isMaskSolutionsActive) emeraldAccent else Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    isSaved = !isSaved
                                    onTakeNote("Bookmarked position: $boardFen")
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "Save Position",
                                    tint = if (isSaved) Color(0xFFFFB300) else Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleLayoutMode,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewAgenda,
                                    contentDescription = "Toggle Layout Mode",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    onExportDiagnosticLog()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share TXT Dump",
                                    tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Slim horizontal page reading progress bar
                    LinearProgressIndicator(
                        progress = { (pageNumber.toFloat() / totalPages.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .padding(horizontal = 4.dp),
                        color = emeraldAccent,
                        trackColor = Color(0xFFE2E8E4)
                    )

                    Spacer(Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        if (!pageImagePath.isNullOrBlank()) {
                            // High fidelity rendered original PDF page with pinch-to-zoom (1x..4x), pan with resistance, double-tap reset, and swipe to page turn when fit
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val cWidth = maxWidth.value
                                val cHeight = maxHeight.value

                                AsyncImage(
                                    model = pageImagePath,
                                    contentDescription = "Original PDF Page $pageNumber",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = pageZoomScale
                                            scaleY = pageZoomScale
                                            translationX = pagePanOffset.x
                                            translationY = pagePanOffset.y
                                        }
                                        .pointerInput(pageNumber, totalPages) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    if (pageZoomScale > 1.05f) {
                                                        pageZoomScale = 1f
                                                        pagePanOffset = Offset.Zero
                                                    } else {
                                                        pageZoomScale = 2.2f
                                                    }
                                                }
                                            )
                                        }
                                        .pointerInput(pageNumber, totalPages) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                val newScale = (pageZoomScale * zoom).coerceIn(1f, 4f)
                                                pageZoomScale = newScale

                                                if (newScale > 1f) {
                                                    val maxPanX = (cWidth * (newScale - 1f)) / 2f
                                                    val maxPanY = (cHeight * (newScale - 1f)) / 2f
                                                    val newOffX = (pagePanOffset.x + pan.x).coerceIn(-maxPanX, maxPanX)
                                                    val newOffY = (pagePanOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                                                    pagePanOffset = Offset(newOffX, newOffY)
                                                } else {
                                                    pagePanOffset = Offset.Zero
                                                    // Page turn swipe when fully fitted
                                                    if (pan.y < -35f && pageNumber < totalPages) {
                                                        onNextPage()
                                                    } else if (pan.y > 35f && pageNumber > 1) {
                                                        onPrevPage()
                                                    }
                                                }
                                            }
                                        }
                                )
                            }
                        } else {
                            // Clean fallback with detected diagrams & extracted text
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Text(
                                        text = pageContentText.ifBlank { "No text on this page." },
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // Diagram Click Targets Overlay - Minimal Retractable Side Overlay
                        if (detectedDiagrams.isNotEmpty()) {
                            val activeDiagram = detectedDiagrams.find {
                                val diagClean = it.placement.split(" ").firstOrNull() ?: it.placement
                                val boardClean = boardFen.split(" ").firstOrNull() ?: boardFen
                                diagClean == boardClean
                            } ?: detectedDiagrams.firstOrNull()

                            // Dynamic interactive bounding box highlights & solution masks over the rendered PDF page
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val viewW = maxWidth.value
                                val viewH = maxHeight.value

                                detectedDiagrams.forEach { diag ->
                                    val isCur = diag == activeDiagram
                                    if (diag.box.x1 > diag.box.x0 && diag.box.y1 > diag.box.y0) {
                                        // Compute aspect-fit scaling to exactly match AsyncImage ContentScale.Fit
                                        val basePdfW = if (diag.pageWidth > 0f) diag.pageWidth else 420f
                                        val basePdfH = if (diag.pageHeight > 0f) diag.pageHeight else 595f
                                        val fitScale = minOf(viewW / basePdfW, viewH / basePdfH)
                                        val renderedW = basePdfW * fitScale
                                        val renderedH = basePdfH * fitScale
                                        val offsetX = (viewW - renderedW) / 2f
                                        val offsetY = (viewH - renderedH) / 2f

                                        // Highlight overlay box over the diagram in the page
                                        val leftDp = (offsetX + diag.box.x0 * fitScale).dp
                                        val topDp = (offsetY + (basePdfH - diag.box.y1) * fitScale).coerceAtLeast(0f).dp
                                        val wDp = ((diag.box.x1 - diag.box.x0) * fitScale).dp
                                        val hDp = ((diag.box.y1 - diag.box.y0) * fitScale).dp

                                        Box(
                                            modifier = Modifier
                                                .offset(x = leftDp, y = topDp)
                                                .size(width = wDp, height = hDp)
                                                .border(
                                                    width = if (isCur) 2.5.dp else 1.dp,
                                                    color = if (isCur) emeraldAccent else emeraldAccent.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                                .background(
                                                    if (isCur) emeraldAccent.copy(alpha = 0.18f) else Color.Transparent
                                                )
                                                .clickable {
                                                    val resolvedFen = if (diag.placement.contains("/")) {
                                                        val placement = diag.placement.split(" ").firstOrNull() ?: diag.placement
                                                        val side = ChessColor.fromChar(diag.sideToMove?.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                                                        LivingChessPlacement.fenFromDiagram(placement, side)
                                                    } else diag.placement
                                                    boardFen = resolvedFen
                                                    chessVm.updateFen(resolvedFen)
                                                    chessVm.analyzePosition()
                                                    val expected = diag.metadata["expectedMoves"]
                                                    if (!expected.isNullOrBlank()) {
                                                        chessVm.loadMovesFromPosition(resolvedFen, expected.split(" ").filter { it.isNotBlank() })
                                                    }
                                                    onDiagramSelected(diag)
                                                }
                                        )

                                        // Solution masking / blanking cover if active or in guess mode
                                        if (isMaskSolutionsActive && isCur) {
                                            // Covers text below the diagram where moves or solutions might be printed
                                            val maskTopDp = (offsetY + (basePdfH - diag.box.y0) * fitScale).coerceAtLeast(0f).dp
                                            val maskHDp = (viewH.dp - maskTopDp).coerceAtLeast(0.dp)
                                            Surface(
                                                modifier = Modifier
                                                    .offset(x = 0.dp, y = maskTopDp)
                                                    .fillMaxWidth()
                                                    .height(maskHDp),
                                                color = Color.White.copy(alpha = 0.98f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC9C4BA))
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                                    contentAlignment = Alignment.TopCenter
                                                ) {
                                                    Text(
                                                        text = "🙈 Solutions hidden for practice. Make your move or tap eye icon to reveal.",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF556B5D)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Retractable expanded drawer / cards
                                AnimatedVisibility(
                                    visible = isDiagramOverlayExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.96f),
                                        shadowElevation = 4.dp,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .widthIn(max = 280.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Page Boards (${detectedDiagrams.size})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E3A2B)
                                                )
                                                IconButton(
                                                    onClick = { isDiagramOverlayExpanded = false },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            detectedDiagrams.forEach { diag ->
                                                val diagCleanPlacement = diag.placement.split(" ").firstOrNull() ?: diag.placement
                                                val boardCleanPlacement = boardFen.split(" ").firstOrNull() ?: boardFen
                                                val isCurrentDiagramActive = diagCleanPlacement == boardCleanPlacement
                                                val exNum = diag.metadata["exerciseNumber"]
                                                val sideIsBlack = diag.sideToMove == "b" || diag.placement.split(" ").getOrNull(1) == "b"
                                                val solutionStr = diag.metadata["solution"]

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val resolvedFen = if (diag.placement.contains("/")) {
                                                                val placement = diag.placement.split(" ").firstOrNull() ?: diag.placement
                                                                val side = ChessColor.fromChar(diag.sideToMove?.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                                                                LivingChessPlacement.fenFromDiagram(placement, side)
                                                            } else diag.placement
                                                            println("[LivingChess] Selecting diagram -> $resolvedFen")
                                                            boardFen = resolvedFen
                                                            chessVm.updateFen(resolvedFen)
                                                            chessVm.analyzePosition()
                                                            val expected = diag.metadata["expectedMoves"]
                                                            if (!expected.isNullOrBlank()) {
                                                                chessVm.loadMovesFromPosition(resolvedFen, expected.split(" ").filter { it.isNotBlank() })
                                                            }
                                                            onDiagramSelected(diag)
                                                            isDiagramOverlayExpanded = false
                                                        },
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isCurrentDiagramActive) Color(0xFFC7EBD7) else Color(0xFFF2F8F4),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        if (isCurrentDiagramActive) 1.5.dp else 1.dp,
                                                        if (isCurrentDiagramActive) emeraldAccent else Color(0xFFB4DFC8)
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isCurrentDiagramActive) Icons.Filled.CheckCircle else Icons.Default.Dashboard,
                                                            contentDescription = null,
                                                            tint = if (isCurrentDiagramActive) Color(0xFF1E5E3D) else emeraldAccent,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (exNum != null) "Ex #$exNum" else (diag.label ?: "Diagram"),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (isCurrentDiagramActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                    color = if (isCurrentDiagramActive) Color(0xFF0F3B24) else Color(0xFF1E3A2B)
                                                                )
                                                                Surface(
                                                                    shape = RoundedCornerShape(2.dp),
                                                                    color = if (sideIsBlack) Color(0xFF26322B) else Color(0xFFE0E0E0)
                                                                ) {
                                                                    Text(
                                                                        text = if (sideIsBlack) "Black to move" else "White to move",
                                                                        fontSize = 8.5.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        color = if (sideIsBlack) Color.White else Color.Black,
                                                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                            if (!solutionStr.isNullOrBlank()) {
                                                                Text(
                                                                    text = "Sol: $solutionStr",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = emeraldAccent,
                                                                    maxLines = 1
                                                                )
                                                            } else if (!diag.nearbyText.isNullOrBlank() && diag.nearbyText != diag.label) {
                                                                Text(
                                                                    text = diag.nearbyText,
                                                                    fontSize = 9.sp,
                                                                    color = Color(0xFF4A6B56),
                                                                    maxLines = 2
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Minimal Floating Pill Trigger
                                Surface(
                                    modifier = Modifier.clickable {
                                        isDiagramOverlayExpanded = !isDiagramOverlayExpanded
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (activeDiagram != null) Color(0xFF1E5E3D) else Color(0xFF26322B),
                                    shadowElevation = 3.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Dashboard,
                                            contentDescription = "Boards",
                                            tint = mintHighlight,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (activeDiagram != null) "Board Active (${detectedDiagrams.size})" else "Boards (${detectedDiagrams.size})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = if (isDiagramOverlayExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── RESIZE HANDLE 1 (Between Column 1 and Column 2) ──
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaWeight = dragAmount / 400f
                            val newW1 = (pane1Weight + deltaWeight).coerceIn(0.5f, 2.0f)
                            val newW2 = (pane2Weight - deltaWeight).coerceIn(0.5f, 2.0f)
                            pane1Weight = newW1
                            pane2Weight = newW2
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6B8073))
                        )
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // COLUMN 2: CHESS BOARD & STUDY CONTROLS
            // ════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .weight(pane2Weight)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Study Actions: [Engine] [Guess the move] (Minimal with icons only)
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
) {
    // Engine Action Button
    Button(
        onClick = {
            if (!isStockfishDownloaded) {
                showEngineDownloadDialog = true
            } else {
                isEngineActive = !isEngineActive
                if (isEngineActive) {
                    chessVm.analyzePosition()
                }
            }
        },
        modifier = Modifier
            .weight(1f)
            .height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEngineActive && isStockfishDownloaded) emeraldAccent else Color(0xFF26322B)
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Memory, // or Icons.Default.Speed
                contentDescription = "Engine",
                modifier = Modifier.size(15.dp),
                tint = Color.White
            )
            Text(
                text = if (!isStockfishDownloaded) "Engine (DL)" else if (isEngineActive && isStockfishDownloaded) "Engine (On)" else "Engine (Off)",
                fontSize = 11.sp,
                color = Color.White
            )
        }
    }

    // Guess the Move Action Button
    Button(
        onClick = {
            isGuessActive = !isGuessActive
            if (isGuessActive && recognizedMoves.isNotEmpty()) {
                chessVm.showPuzzleHint()
            }
        },
        modifier = Modifier
            .weight(1f)
            .height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isGuessActive) emeraldAccent else Color(0xFF26322B)
        ),
        shape = RoundedCornerShape(4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Psychology, // or Icons.Default.HelpOutline
                contentDescription = "Guess the move",
                modifier = Modifier.size(15.dp),
                tint = Color.White
            )
            Text(
                text = if (isGuessActive) "Guess (Active)" else "Guess Move",
                fontSize = 11.sp,
                color = Color.White
            )
        }
    }
}

                // Practice / Guess Mode Feedback Banner
                if (isGuessActive && guessFeedbackText != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF14241B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, guessFeedbackColor.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = guessFeedbackText ?: "",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = guessFeedbackColor,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { guessFeedbackText = null },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Interactive 8x8 Chess Board Container (with active diagram light filling highlight)
                val isBoardMatchingDiagram = detectedDiagrams.any { 
                    val p = it.placement.split(" ").firstOrNull() ?: it.placement
                    val b = boardFen.split(" ").firstOrNull() ?: boardFen
                    p == b
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBoardMatchingDiagram) Color(0xFF1E2D24) else panelBg,
                    border = if (isBoardMatchingDiagram) androidx.compose.foundation.BorderStroke(1.5.dp, emeraldAccent.copy(alpha = 0.8f)) else null
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ChessBoardContainer(
                            fen = boardFen,
                            theme = defaultChessTheme,
                            isPlayerWhite = !isBoardFlipped,
                            selectedSquare = selectedSquare,
                            premove = null,
                            showThreatMap = showThreatMode,
                            threatMap = if (showThreatMode) threatMap else emptyMap(),
                            engineEvaluation = if (isEngineActive) engineEvaluation else null,
                            visualArrows = if (isEngineActive) engineVisualArrows else emptyList(),
                            moveHistory = displayHistory,
                            moveAnalysis = moveAnalysis,
                            currentMoveIndex = displayCurrentIndex,
                            onSquareClick = { clickedSquare ->
                                if (selectedSquare == null) {
                                    val pieces = ChessLogic.parseFen(boardFen).pieces
                                    if (pieces[clickedSquare] != null) {
                                        selectedSquare = clickedSquare
                                    }
                                } else {
                                    val from = selectedSquare!!
                                    val dests = ChessLogic.getValidDestinations(boardFen, from)
                                    if (dests.contains(clickedSquare)) {
                                        println("[StockfishDebug] User made board move: $from -> $clickedSquare")
                                        val promoChar = if ((from[1] == '7' && clickedSquare[1] == '8') || (from[1] == '2' && clickedSquare[1] == '1')) 'q' else null
                                        val moveSan = ChessLogic.moveToSan(boardFen, from, clickedSquare, promoChar)
                                        val updated = ChessLogic.applyMove(boardFen, from, clickedSquare)
                                        if (updated.isNotBlank()) {
                                            // Handle Guess the move practice feedback
                                            if (isGuessActive) {
                                                // Check against recognized moves from the book, or fallback to engine best move
                                                val expectedMove = if (recognizedMoves.isNotEmpty()) {
                                                    recognizedMoves.getOrNull(displayCurrentIndex + 1) ?: recognizedMoves.firstOrNull()
                                                } else null

                                                val bestEngineUci = engineEvaluation?.bestLine?.firstOrNull()
                                                val bestEngineSan = if (!bestEngineUci.isNullOrBlank() && bestEngineUci.length >= 4) {
                                                    val eFrom = bestEngineUci.substring(0, 2)
                                                    val eTo = bestEngineUci.substring(2, 4)
                                                    val ePromo = if (bestEngineUci.length > 4) bestEngineUci[4] else null
                                                    ChessLogic.moveToSan(boardFen, eFrom, eTo, ePromo)
                                                } else null

                                                val cleanPlayed = moveSan.replace(Regex("[+#!?]+$"), "")
                                                if (expectedMove != null) {
                                                    val cleanExpected = expectedMove.replace(Regex("[+#!?]+$"), "")
                                                    if (cleanPlayed.equals(cleanExpected, ignoreCase = true)) {
                                                        guessFeedbackText = "✓ Correct! The book plays $expectedMove"
                                                        guessFeedbackColor = Color(0xFF70DEAB) // Emerald green
                                                    } else if (bestEngineSan != null && cleanPlayed.equals(bestEngineSan.replace(Regex("[+#!?]+$"), ""), ignoreCase = true)) {
                                                        guessFeedbackText = "★ Excellent! $moveSan is the engine's best move (Book plays $expectedMove)"
                                                        guessFeedbackColor = Color(0xFF70DEAB)
                                                    } else {
                                                        val correctHint = if (bestEngineSan != null) "The book plays $expectedMove (Engine: $bestEngineSan)" else "The book plays $expectedMove"
                                                        guessFeedbackText = "✗ Wrong move ($moveSan). $correctHint"
                                                        guessFeedbackColor = Color(0xFFFFB300) // Amber warning
                                                    }
                                                } else if (bestEngineSan != null) {
                                                    if (cleanPlayed.equals(bestEngineSan.replace(Regex("[+#!?]+$"), ""), ignoreCase = true)) {
                                                        guessFeedbackText = "✓ Correct! Engine top move: $bestEngineSan"
                                                        guessFeedbackColor = Color(0xFF70DEAB)
                                                    } else {
                                                        guessFeedbackText = "✗ Played $moveSan. Engine best move: $bestEngineSan"
                                                        guessFeedbackColor = Color(0xFFFFB300)
                                                    }
                                                } else {
                                                    guessFeedbackText = "Played $moveSan"
                                                    guessFeedbackColor = Color.White
                                                }
                                            }

                                            boardFen = updated
                                            // Route through ViewModel to record move, update engine position & evaluate
                                            chessVm.exitGameSession()
                                            chessVm.makeMove(from, clickedSquare)
                                            if (isEngineActive) {
                                                chessVm.analyzePosition()
                                            }
                                        }
                                    }
                                    selectedSquare = null
                                }
                            }
                        )
                    }
                }

                // Dual White/Black Move History Row for both sides
                if (displayHistory.isNotEmpty()) {
                    InteractiveMoveHistoryRow(
                        moveHistory = displayHistory,
                        moveAnalysis = moveAnalysis,
                        currentMoveIndex = displayCurrentIndex,
                        onSelectMove = { idx ->
                            moveIndex = idx
                            if (moveHistory.isNotEmpty()) {
                                chessVm.goToMove(idx)
                                boardFen = chessVm.fen.value
                            } else {
                                val start = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                var f = start
                                for (i in 0..idx) {
                                    val m = displayHistory.getOrNull(i) ?: break
                                    val trip = ChessLogic.sanToMove(f, m)
                                    if (trip != null) {
                                        f = ChessLogic.applyMove(f, trip.first, trip.second, trip.third)
                                    }
                                }
                                boardFen = f
                                chessVm.updateFen(f)
                                chessVm.analyzePosition()
                                displayHistory.getOrNull(idx)?.let { onMoveSelected(it) }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1A221E))
                    )
                }

                // Board Action Buttons Row: [|<] [<] [>] [>|] [⇅ Flip] [↺ Reset]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161F1A))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canGoBack = if (moveHistory.isNotEmpty()) currentMoveIndex >= 0 else moveIndex > 0
                    val canGoForward = if (moveHistory.isNotEmpty()) currentMoveIndex < moveHistory.size - 1 else moveIndex < displayHistory.size - 1

                    IconButton(
                        onClick = {
                            if (moveHistory.isNotEmpty()) {
                                chessVm.goToMove(-1)
                                val initial = chessVm.fenHistory.firstOrNull() ?: currentFen.ifBlank { ChessLogic.getInitialFen() }
                                boardFen = initial
                            } else {
                                moveIndex = -1
                                val initial = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                boardFen = initial
                                chessVm.updateFen(initial)
                                chessVm.analyzePosition()
                            }
                        },
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, "First Move", tint = if (canGoBack) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            if (moveHistory.isNotEmpty()) {
                                chessVm.stepBackward()
                                boardFen = chessVm.fen.value
                            } else if (moveIndex >= 0) {
                                moveIndex--
                                if (moveIndex == -1) {
                                    val initial = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                    boardFen = initial
                                    chessVm.updateFen(initial)
                                    chessVm.analyzePosition()
                                } else {
                                    // Replay moves from start up to moveIndex
                                    val start = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                    var f = start
                                    for (i in 0..moveIndex) {
                                        val m = displayHistory.getOrNull(i) ?: break
                                        val trip = ChessLogic.sanToMove(f, m)
                                        if (trip != null) {
                                            f = ChessLogic.applyMove(f, trip.first, trip.second, trip.third)
                                        }
                                    }
                                    boardFen = f
                                    chessVm.updateFen(f)
                                    chessVm.analyzePosition()
                                    displayHistory.getOrNull(moveIndex)?.let { onMoveSelected(it) }
                                }
                            }
                        },
                        enabled = canGoBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Prev Move", tint = if (canGoBack) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {
                            if (moveHistory.isNotEmpty()) {
                                chessVm.stepForward()
                                boardFen = chessVm.fen.value
                            } else if (moveIndex < displayHistory.size - 1) {
                                moveIndex++
                                val start = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                var f = start
                                for (i in 0..moveIndex) {
                                    val m = displayHistory.getOrNull(i) ?: break
                                    val trip = ChessLogic.sanToMove(f, m)
                                    if (trip != null) {
                                        f = ChessLogic.applyMove(f, trip.first, trip.second, trip.third)
                                    }
                                }
                                boardFen = f
                                chessVm.updateFen(f)
                                chessVm.analyzePosition()
                                displayHistory.getOrNull(moveIndex)?.let { onMoveSelected(it) }
                            }
                        },
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "Next Move", tint = if (canGoForward) Color.White else Color.Gray, modifier = Modifier.size(24.dp))
                    }
                    IconButton(
                        onClick = {
                            if (moveHistory.isNotEmpty()) {
                                chessVm.goToMove(moveHistory.size - 1)
                                boardFen = chessVm.fen.value
                            } else if (displayHistory.isNotEmpty()) {
                                moveIndex = displayHistory.size - 1
                                val start = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                var f = start
                                for (i in 0..moveIndex) {
                                    val m = displayHistory.getOrNull(i) ?: break
                                    val trip = ChessLogic.sanToMove(f, m)
                                    if (trip != null) {
                                        f = ChessLogic.applyMove(f, trip.first, trip.second, trip.third)
                                    }
                                }
                                boardFen = f
                                chessVm.updateFen(f)
                                chessVm.analyzePosition()
                                displayHistory.getOrNull(moveIndex)?.let { onMoveSelected(it) }
                            }
                        },
                        enabled = canGoForward,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, "Last Move", tint = if (canGoForward) Color.White else Color.Gray, modifier = Modifier.size(18.dp))
                    }

                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFF2E3E34)))

                    IconButton(
                        onClick = { isBoardFlipped = !isBoardFlipped },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, "Flip Board", tint = mintHighlight, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val initial = ChessLogic.getInitialFen()
                            boardFen = initial
                            chessVm.updateFen(initial)
                            chessVm.analyzePosition()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.RotateLeft, "Reset Position", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                    }
                }

                

                // Next Step Bar matching reference UI image
                if (detectedDiagrams.isNotEmpty()) {
                    val activeOrFirst = detectedDiagrams.find {
                        val diagClean = it.placement.split(" ").firstOrNull() ?: it.placement
                        val boardClean = boardFen.split(" ").firstOrNull() ?: boardFen
                        diagClean == boardClean
                    } ?: detectedDiagrams.first()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2822)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activeOrFirst.metadata["caption"] ?: activeOrFirst.label ?: "Diagram on Page ${activeOrFirst.page}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                                Text("${detectedDiagrams.size} boards on page", fontSize = 10.sp, color = mintHighlight)
                            }
                            if (!activeOrFirst.nearbyText.isNullOrBlank()) {
                                Text(
                                    text = activeOrFirst.nearbyText,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFC0D0C5),
                                    maxLines = 2
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val exerciseNum = activeOrFirst.metadata["exerciseNumber"]
                                val sideText = if (activeOrFirst.sideToMove == "b") "Black to move" else "White to move"
                                Text(
                                    text = if (exerciseNum != null) "Ex #$exerciseNum • $sideText" else sideText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (activeOrFirst.sideToMove == "b") Color(0xFFFFB300) else mintHighlight
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = {
                                            val resolved = if (activeOrFirst.placement.contains("/")) {
                                                val placement = activeOrFirst.placement.split(" ").firstOrNull() ?: activeOrFirst.placement
                                                val side = ChessColor.fromChar(activeOrFirst.sideToMove?.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                                                LivingChessPlacement.fenFromDiagram(placement, side)
                                            } else activeOrFirst.placement
                                            boardFen = resolved
                                            chessVm.updateFen(resolved)
                                            chessVm.analyzePosition()
                                            onDiagramSelected(activeOrFirst)
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Load Board", color = mintHighlight, fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            val resolved = if (activeOrFirst.placement.contains("/")) {
                                                val placement = activeOrFirst.placement.split(" ").firstOrNull() ?: activeOrFirst.placement
                                                val side = ChessColor.fromChar(activeOrFirst.sideToMove?.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                                                LivingChessPlacement.fenFromDiagram(placement, side)
                                            } else activeOrFirst.placement
                                            boardFen = resolved
                                            chessVm.updateFen(resolved)
                                            chessVm.analyzePosition()
                                            isBoardFlipped = activeOrFirst.sideToMove == "b"
                                            isGuessActive = true
                                            isMaskSolutionsActive = true
                                            guessFeedbackText = "🎯 Practice Mode Active: Make your move for ${if (activeOrFirst.sideToMove == "b") "Black" else "White"}"
                                            guessFeedbackColor = mintHighlight
                                            onSolveDiagram(activeOrFirst)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Practice", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── RESIZE HANDLE 2 (Between Column 2 and Column 3) ──
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaWeight = dragAmount / 400f
                            val newW2 = (pane2Weight + deltaWeight).coerceIn(0.5f, 2.0f)
                            val newW3 = (pane3Weight - deltaWeight).coerceIn(0.5f, 2.0f)
                            pane2Weight = newW2
                            pane3Weight = newW3
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6B8073))
                        )
                    }
                }
            }

            // ════════════════════════════════════════════════════════════════
            // COLUMN 3: STUDY NOTEBOOK
            // ════════════════════════════════════════════════════════════════
            Surface(
                modifier = Modifier
                    .weight(pane3Weight)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
                color = panelBg
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title & close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Analysis & Study", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    if (!isStockfishDownloaded) {
                                        showEngineDownloadDialog = true
                                    } else {
                                        // Ensure ChessViewModel has the exact board position and moves before opening full screen
                                        chessVm.updateFen(boardFen)
                                        if (displayHistory.isNotEmpty() && moveHistory.isEmpty()) {
                                            chessVm.loadMovesFromPosition(boardFen, displayHistory)
                                        }
                                        chessVm.analyzePosition()
                                        showFullAnalysisDialog = true
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Default.OpenInFull, contentDescription = "Deep Analysis Full Screen", tint = mintHighlight, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = onCloseNotebook, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Tabs: Analysis | Moves | Notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Analysis", "Moves (${displayHistory.size})", "Notes (${notes.size})").forEach { tabName ->
                            val isSelected = selectedNotebookTab.startsWith(tabName.take(4))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) Color(0xFF26322B) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent) else null,
                                modifier = Modifier.clickable { selectedNotebookTab = tabName }
                            ) {
                                Text(
                                    text = tabName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) mintHighlight else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // TAB 1: Real-time Analysis Sidebar (Stockfish lines & depth evaluation)
                    if (selectedNotebookTab.startsWith("Anal")) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Engine Lines & Threat/Graph Mode Panel
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF141312),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LichessEngineLines(
                                    evaluation = engineEvaluation,
                                    currentFen = boardFen,
                                    numLines = numEngineLines,
                                    darkBackground = true,
                                    showThreatMode = showThreatMode,
                                    showGraphMode = showGraphMode,
                                    onToggleThreat = {
                                        showThreatMode = !showThreatMode
                                        if (showThreatMode) {
                                            val threatFen = ChessLogic.getThreatFen(boardFen)
                                            if (threatFen != null) {
                                                chessVm.updateFen(threatFen)
                                                chessVm.analyzePosition()
                                            } else {
                                                // In check: cannot calculate threat
                                                showThreatMode = false
                                            }
                                        } else {
                                            chessVm.updateFen(boardFen)
                                            chessVm.analyzePosition()
                                        }
                                    },
                                    onToggleGraph = {
                                        showGraphMode = !showGraphMode
                                    },
                                    onToggleFewerLines = {
                                        numEngineLines = if (numEngineLines > 1) 1 else 3
                                    },
                                    onTapMove = { uci ->
                                        if (uci.length >= 4) {
                                            val from = uci.substring(0, 2)
                                            val to = uci.substring(2, 4)
                                            val updated = ChessLogic.applyMove(boardFen, from, to)
                                            if (updated.isNotBlank()) {
                                                boardFen = updated
                                                chessVm.makeMove(from, to)
                                            }
                                        }
                                    }
                                )
                            }

                            // Dynamic Advantage / Evaluation Graph
                            if (showGraphMode) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1A221E),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E3E34))
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text(
                                            "Evaluation Graph",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = mintHighlight
                                        )
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                                            val currentEvalScore = engineEvaluation?.score ?: 0.0
                                            val midY = size.height / 2f
                                            // Draw baseline
                                            drawLine(
                                                Color(0xFF6B8073).copy(alpha = 0.5f),
                                                androidx.compose.ui.geometry.Offset(0f, midY),
                                                androidx.compose.ui.geometry.Offset(size.width, midY),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                            val py = (midY - (currentEvalScore.coerceIn(-5.0, 5.0) / 5.0 * (midY - 8f))).toFloat()
                                            drawCircle(
                                                color = if (currentEvalScore >= 0) emeraldAccent else amberHighlight,
                                                radius = 4.dp.toPx(),
                                                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, py)
                                            )
                                        }
                                    }
                                }
                            }

                            // Move History Table inside Analysis side bar
                            Text(
                                "Move Notation & Quality",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = mintHighlight,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            ChessVerticalMoveHistoryTable(
                                moveHistory = displayHistory,
                                moveAnalysis = moveAnalysis,
                                currentMoveIndex = displayCurrentIndex,
                                currentFen = boardFen,
                                evaluation = engineEvaluation,
                                onSelectMove = { idx ->
                                    moveIndex = idx
                                    if (moveHistory.isNotEmpty()) {
                                        chessVm.goToMove(idx)
                                        boardFen = chessVm.fen.value
                                    } else {
                                        val start = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                        var f = start
                                        for (i in 0..idx) {
                                            val m = displayHistory.getOrNull(i) ?: break
                                            val trip = ChessLogic.sanToMove(f, m)
                                            if (trip != null) {
                                                f = ChessLogic.applyMove(f, trip.first, trip.second, trip.third)
                                            }
                                        }
                                        boardFen = f
                                        chessVm.updateFen(f)
                                        chessVm.analyzePosition()
                                        displayHistory.getOrNull(idx)?.let { onMoveSelected(it) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    } else if (selectedNotebookTab.startsWith("Move")) {
                        // TAB 2: Full Vertical Move History Table
                        ChessVerticalMoveHistoryTable(
                            moveHistory = displayHistory,
                            moveAnalysis = moveAnalysis,
                            currentMoveIndex = displayCurrentIndex,
                            currentFen = boardFen,
                            evaluation = engineEvaluation,
                            onSelectMove = { idx ->
                                moveIndex = idx
                                if (moveHistory.isNotEmpty()) {
                                    chessVm.goToMove(idx)
                                    boardFen = chessVm.fen.value
                                } else {
                                    val start = if (currentFen.isNotBlank() && currentFen.contains("/")) currentFen else ChessLogic.getInitialFen()
                                    var f = start
                                    for (i in 0..idx) {
                                        val m = displayHistory.getOrNull(i) ?: break
                                        val trip = ChessLogic.sanToMove(f, m)
                                        if (trip != null) {
                                            f = ChessLogic.applyMove(f, trip.first, trip.second, trip.third)
                                        }
                                    }
                                    boardFen = f
                                    chessVm.updateFen(f)
                                    chessVm.analyzePosition()
                                    displayHistory.getOrNull(idx)?.let { onMoveSelected(it) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    } else {
                        // TAB 3: Study Notes
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Write notes or save key positions. Tied directly to page $pageNumber.",
                                color = Color(0xFFA5B8AD),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )

                            // Notes list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(notes) { n ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF222C26)
                                    ) {
                                        Text(
                                            text = n,
                                            fontSize = 11.sp,
                                            color = Color(0xFFD6E4DC),
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }

                            // Note input box
                            Text("Attaches to: Page $pageNumber", fontSize = 10.sp, color = mintHighlight)
                            OutlinedTextField(
                                value = noteInputText,
                                onValueChange = { noteInputText = it },
                                placeholder = { Text("Write a note", fontSize = 11.sp, color = Color.Gray) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(65.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = emeraldAccent,
                                    unfocusedBorderColor = Color(0xFF2E3E34),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Button(
                                onClick = {
                                    if (noteInputText.isNotBlank()) {
                                        notes.add("P$pageNumber: $noteInputText")
                                        onTakeNote(noteInputText)
                                        noteInputText = ""
                                    }
                                },
                                modifier = Modifier.align(Alignment.End).height(30.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263D31)),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Take note", color = mintHighlight, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
        // ── Engine Required Dialog (Download or Import Stockfish 17) ──
        if (showEngineDownloadDialog) {
            AlertDialog(
                onDismissRequest = { showEngineDownloadDialog = false },
                containerColor = Color(0xFF1E2822),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = emeraldAccent)
                        Text("Stockfish 17 Engine Required", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "To enable real-time engine evaluation, candidate move arrows, and deep analysis, please download or import the Stockfish 17 engine pack (like in Settings).",
                            color = Color(0xFFD6E4DC),
                            fontSize = 13.sp
                        )
                        if (isDownloadingSF) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { sfProgressFloat },
                                modifier = Modifier.fillMaxWidth(),
                                color = emeraldAccent
                            )
                            Text(
                                "Downloading engine... ${sfPack?.downloadProgress ?: 0}%",
                                color = mintHighlight,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                filePicker()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = mintHighlight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Import Binary", fontSize = 12.sp)
                        }
                        if (!isDownloadingSF) {
                            Button(
                                onClick = {
                                    chessVm.downloadStockfish()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Download", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEngineDownloadDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // ── Quick Page Jump Dialog ──
        if (showJumpPageDialog) {
            AlertDialog(
                onDismissRequest = { showJumpPageDialog = false },
                containerColor = Color(0xFF1E2822),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = emeraldAccent)
                        Text("Jump to Page", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter a page number between 1 and $totalPages:",
                            color = Color(0xFFD6E4DC),
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = jumpPageInput,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) jumpPageInput = input
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = emeraldAccent,
                                unfocusedBorderColor = Color(0xFF384D41),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = jumpPageInput.toIntOrNull()
                            if (target != null && target in 1..totalPages) {
                                onJumpToPage(target)
                                showJumpPageDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent)
                    ) {
                        Text("Jump", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpPageDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        // ── Navigation Drawer / Sidebar (All Book Positions & Chapters) ──
        if (showNavSidebar) {
            Dialog(
                onDismissRequest = { showNavSidebar = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { showNavSidebar = false }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(min = 340.dp, max = 460.dp)
                            .align(Alignment.CenterStart)
                            .clickable(enabled = false) {}, // prevent click-through dismissal
                        color = Color(0xFF161F1A),
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                        shadowElevation = 16.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, emeraldAccent.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Sidebar Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = emeraldAccent, modifier = Modifier.size(20.dp))
                                    Text("Book Navigation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                IconButton(onClick = { showNavSidebar = false }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Sidebar Tabs: Positions vs Chapters
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF233028))
                                    .padding(2.dp)
                            ) {
                                val totalPositionsCount = remember(allBookDiagrams) {
                                    allBookDiagrams.values.sumOf { it.size }
                                }
                                Button(
                                    onClick = { navSidebarTab = "Positions" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (navSidebarTab == "Positions") emeraldAccent else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "Positions ($totalPositionsCount)",
                                        color = if (navSidebarTab == "Positions") Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = { navSidebarTab = "Chapters" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (navSidebarTab == "Chapters") emeraldAccent else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.weight(1f).height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "Chapters (${chapters.size})",
                                        color = if (navSidebarTab == "Chapters") Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            if (navSidebarTab == "Positions") {
                                // Search Input for positions / exercises / moves
                                OutlinedTextField(
                                    value = positionSearchQuery,
                                    onValueChange = { positionSearchQuery = it },
                                    placeholder = { Text("Search exercise #, page, moves...", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    },
                                    trailingIcon = {
                                        if (positionSearchQuery.isNotBlank()) {
                                            IconButton(onClick = { positionSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = emeraldAccent,
                                        unfocusedBorderColor = Color(0xFF384D41),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )

                                Spacer(Modifier.height(8.dp))

                                // Flatten all diagram positions across all pages
                                val flatPositions = remember(allBookDiagrams, positionSearchQuery) {
                                    val list = mutableListOf<PdfDiagramPosition>()
                                    allBookDiagrams.keys.sorted().forEach { pNum ->
                                        allBookDiagrams[pNum]?.forEach { diag ->
                                            if (positionSearchQuery.isBlank()) {
                                                list.add(diag)
                                            } else {
                                                val q = positionSearchQuery.trim()
                                                val matchesPage = diag.pageNumber.toString() == q || "page $q" in diag.label.lowercase()
                                                val matchesEx = diag.metadata["exerciseNumber"]?.contains(q, ignoreCase = true) == true
                                                val matchesSol = diag.metadata["solution"]?.contains(q, ignoreCase = true) == true
                                                val matchesLabel = diag.label.contains(q, ignoreCase = true)
                                                val matchesNear = diag.nearbyText?.contains(q, ignoreCase = true) == true
                                                if (matchesPage || matchesEx || matchesSol || matchesLabel || matchesNear) {
                                                    list.add(diag)
                                                }
                                            }
                                        }
                                    }
                                    list
                                }

                                if (flatPositions.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (allBookDiagrams.isEmpty()) "Scanning & parsing book diagrams..." else "No positions matching \"$positionSearchQuery\"",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(flatPositions) { diag ->
                                            val exNum = diag.metadata["exerciseNumber"]
                                            val isCurPage = diag.pageNumber == pageNumber
                                            val isBlackTurn = diag.sideToMove == "b" || diag.placement.split(" ").getOrNull(1) == "b"
                                            val solutionText = diag.metadata["solution"]

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onJumpToPage(diag.pageNumber)
                                                        val resolved = if (diag.placement.contains("/")) {
                                                            val parts = diag.placement.split(" ")
                                                            val side = ChessColor.fromChar(diag.sideToMove.firstOrNull() ?: 'w') ?: ChessColor.WHITE
                                                            LivingChessPlacement.fenFromDiagram(parts[0], side)
                                                        } else diag.placement
                                                        boardFen = resolved
                                                        chessVm.updateFen(resolved)
                                                        chessVm.analyzePosition()
                                                        val expected = diag.metadata["expectedMoves"]
                                                        if (!expected.isNullOrBlank()) {
                                                            chessVm.loadMovesFromPosition(resolved, expected.split(" ").filter { it.isNotBlank() })
                                                        }
                                                        onDiagramSelected(
                                                            DetectedDiagram(
                                                                id = "pdf_p${diag.pageNumber}_${diag.boxX0}",
                                                                page = diag.pageNumber,
                                                                box = PdfBox(diag.boxX0, diag.boxY0, diag.boxX1, diag.boxY1),
                                                                placement = diag.placement,
                                                                sideToMove = diag.sideToMove,
                                                                label = diag.label,
                                                                pageWidth = diag.pageWidth,
                                                                pageHeight = diag.pageHeight,
                                                                nearbyText = diag.nearbyText,
                                                                metadata = diag.metadata
                                                            )
                                                        )
                                                        showNavSidebar = false
                                                    },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isCurPage) Color(0xFF1E382A) else Color(0xFF1B2620),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    if (isCurPage) 1.5.dp else 1.dp,
                                                    if (isCurPage) emeraldAccent else Color(0xFF2C3E33)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (exNum != null) "Exercise #$exNum" else diag.label,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = Color.White
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isCurPage) emeraldAccent else Color(0xFF293B30)
                                                        ) {
                                                            Text(
                                                                text = "Page ${diag.pageNumber}",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCurPage) Color.Black else mintHighlight,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(2.dp),
                                                            color = if (isBlackTurn) Color(0xFF0F1512) else Color(0xFFE2E2E2)
                                                        ) {
                                                            Text(
                                                                text = if (isBlackTurn) "Black to move" else "White to move",
                                                                fontSize = 8.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isBlackTurn) Color.White else Color.Black,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                        if (!solutionText.isNullOrBlank()) {
                                                            Text(
                                                                text = "• $solutionText",
                                                                fontSize = 9.5.sp,
                                                                color = mintHighlight,
                                                                maxLines = 1,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Chapters & Topics Tab
                                if (chapters.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No chapters found in document outline.", color = Color.Gray, fontSize = 12.sp)
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(chapters) { ch ->
                                            val isNearPage = abs(ch.page - pageNumber) <= 1
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        onJumpToPage(ch.page)
                                                        showNavSidebar = false
                                                    },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isNearPage) Color(0xFF1E382A) else Color(0xFF1B2620),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    if (isNearPage) 1.5.dp else 1.dp,
                                                    if (isNearPage) emeraldAccent else Color(0xFF2C3E33)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                        Text(
                                                            text = ch.title,
                                                            fontSize = if (ch.level == 0) 12.sp else 11.sp,
                                                            fontWeight = if (ch.level == 0) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isNearPage) mintHighlight else Color.White,
                                                            maxLines = 2
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF293B30)
                                                    ) {
                                                        Text(
                                                            text = "p. ${ch.page}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = mintHighlight,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Full-Screen Deep Analysis Dialog ──
        if (showFullAnalysisDialog) {
            Dialog(
                onDismissRequest = { showFullAnalysisDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                ChessAnalysisScreen(
                    vm = chessVm,
                    initialTab = "Analysis",
                    onBack = { showFullAnalysisDialog = false }
                )
            }
        }
    }
}
