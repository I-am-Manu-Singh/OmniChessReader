package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.creativeblisslabs.omnimind.core.media.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creativeblisslabs.omnimind.core.chess.GameMode
import com.creativeblisslabs.omnimind.core.chess.MoveQuality
import com.creativeblisslabs.omnimind.chess.shared.GameReviewSummary

// ── Lichess accuracy color thresholds (matches _accuracyColor in game_summary_table.dart) ──
private fun accuracyColor(accuracy: Int): Color = when {
    accuracy >= 85 -> Color(0xFF4CAF50)
    accuracy >= 70 -> Color(0xFFFF9800)
    accuracy >= 55 -> Color(0xFFFF5722)
    else           -> Color(0xFFEF5350)
}

data class SavedGameRecord(
    val whitePlayer: String,
    val blackPlayer: String,
    val date: String,
    val result: String,
    val pgn: String
)

// ════════════════════════════════════════════════════════════
// ChessAnalysisScreen — unified board + analysis + review
// ════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessAnalysisScreen(
    vm: ChessViewModel,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val moveHistory by vm.moveHistory.collectAsStateWithLifecycle()
    val currentMoveIndex by vm.currentMoveIndex.collectAsStateWithLifecycle()
    val currentFen by vm.fen.collectAsStateWithLifecycle()

    val mediaActions = rememberMediaActions(
        onImagePicked = { vm.importBoardImage(it) },
        onFilePicked = { vm.importPgnFile(it) },
        onAudioPicked = {},
        onClipboardImage = { vm.importBoardImage(it) }
    )
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()

    var isBoardFlipped by remember { mutableStateOf(false) }
    var practiceBannerMessage by remember { mutableStateOf<String?>(null) }
    var showPgnImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(currentFen) {
        vm.analyzePosition()
        vm.fetchOpeningExplorer(currentFen)
    }

    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val moveAnalysis by vm.moveAnalysis.collectAsStateWithLifecycle()
    val userProfile by vm.currentUserRating.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val localOpening by vm.localOpeningInfo.collectAsStateWithLifecycle()
    val whiteTimer by vm.whiteTimer.collectAsStateWithLifecycle()
    val blackTimer by vm.blackTimer.collectAsStateWithLifecycle()
    val analysisResult by vm.gameAnalysisResult.collectAsStateWithLifecycle()

    // Tabs: Analysis | Review | Games | Explore
    var selectedTab by remember { mutableStateOf("Analysis") }
    var isAnalysisEnabled by remember { mutableStateOf(true) }
    val scheme = MaterialTheme.colorScheme

    val userName = userProfile.username.ifBlank { "Player" }
    val userRatingVal = userProfile.rapidRating?.takeIf { it > 0 }
        ?: userProfile.blitzRating?.takeIf { it > 0 }
        ?: userProfile.bulletRating?.takeIf { it > 0 }
        ?: 1200
    val botLvl = vm.botLevel.collectAsStateWithLifecycle().value

    val whiteLabel = if (isPlayerWhite) "$userName ($userRatingVal)" else "Stockfish Lv $botLvl"
    val blackLabel = if (!isPlayerWhite) "$userName ($userRatingVal)" else "Stockfish Lv $botLvl"
    val openingName = localOpening?.name ?: "Open Game"

    fun formatMs(ms: Long): String {
        if (ms <= 0L) return "10:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "$min:${sec.toString().padStart(2, '0')}"
    }
    val whiteTimeStr = formatMs(whiteTimer.remainingTimeMs)
    val blackTimeStr = formatMs(blackTimer.remainingTimeMs)

    val onMistake = {
        val idx = moveAnalysis.indexOfFirst {
            it.quality == MoveQuality.BLUNDER || it.quality == MoveQuality.MISTAKE
        }
        if (idx != -1) {
            vm.practiceMistake(idx)
            practiceBannerMessage = "Practicing Move ${idx + 1}: Find the best move!"
        } else {
            practiceBannerMessage = "No major mistakes found in this game!"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { vm.exitGameSession(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        vm.runGameAnalysis()
                        selectedTab = "Review"
                    }) {
                        Icon(Icons.Filled.Star, null, tint = Color(0xFF81B64C), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Review", color = Color(0xFF81B64C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.surfaceContainerHigh,
                    titleContentColor = scheme.onSurface,
                    navigationIconContentColor = scheme.onSurface
                )
            )
        },
        containerColor = scheme.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val isLandscape = maxWidth > 600.dp

            if (isLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Board + engine lines + controls
                    Column(
                        modifier = Modifier.weight(1.1f).fillMaxHeight()
                            .background(scheme.surfaceContainerHighest),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnalysisPlayerBar(
                            name = if (isPlayerWhite) "Stockfish Lv $botLvl" else userName,
                            pieceSymbol = "♟", isWhite = false,
                            timerText = if (isPlayerWhite) blackTimeStr else whiteTimeStr,
                            scheme = scheme
                        )
                        if (isAnalysisEnabled) {
                            LichessEngineLines(evaluation = evaluation, currentFen = currentFen, numLines = 2)
                        }
                        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            ChessBoardContainer(
                                fen = currentFen, theme = theme, pieceStyle = pieceStyle,
                                isPlayerWhite = !isBoardFlipped, selectedSquare = null, premove = null,
                                showThreatMap = false, threatMap = emptyMap(), visualArrows = visualArrows,
                                moveHistory = moveHistory, moveAnalysis = moveAnalysis,
                                currentMoveIndex = currentMoveIndex, onSquareClick = { _ -> }
                            )
                        }
                        AnalysisPlayerBar(
                            name = if (isPlayerWhite) userName else "Stockfish Lv $botLvl",
                            pieceSymbol = "♔", isWhite = true,
                            timerText = if (isPlayerWhite) whiteTimeStr else blackTimeStr,
                            scheme = scheme
                        )
                        AnalysisBoardControls(
                            onFirst = { vm.goToMove(-1) }, onPrev = { vm.stepBackward() },
                            onNext = { vm.stepForward() }, onLast = { vm.goToMove(moveHistory.size - 1) },
                            onFlip = { isBoardFlipped = !isBoardFlipped }, onMistake = onMistake, scheme = scheme
                        )
                    }
                    // Right: Unified analysis panel
                    Column(
                        modifier = Modifier.weight(0.9f).fillMaxHeight().background(scheme.background)
                    ) {
                        AnalysisPanelContent(
                            selectedTab = selectedTab, onTabSelected = { selectedTab = it },
                            isAnalysisEnabled = isAnalysisEnabled, onAnalysisToggle = { isAnalysisEnabled = it },
                            evaluation = evaluation, currentFen = currentFen, openingName = openingName,
                            whiteLabel = whiteLabel, blackLabel = blackLabel, moveHistory = moveHistory,
                            moveAnalysis = moveAnalysis, currentMoveIndex = currentMoveIndex,
                            analysisResult = analysisResult, vm = vm,
                            practiceBannerMessage = practiceBannerMessage,
                            onDismissPracticeBanner = { practiceBannerMessage = null },
                            onShowReview = { vm.runGameAnalysis(); selectedTab = "Review" },
                            onAddGame = { showPgnImportDialog = true },
                            onSharePgn = { vm.sharePGN() },
                            scheme = scheme
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnalysisPlayerBar(
                        name = if (isPlayerWhite) "Stockfish Lv $botLvl" else userName,
                        pieceSymbol = "♟", isWhite = false,
                        timerText = if (isPlayerWhite) blackTimeStr else whiteTimeStr, scheme = scheme
                    )
                    if (isAnalysisEnabled) {
                        LichessEngineLines(evaluation = evaluation, currentFen = currentFen, numLines = 2)
                    }
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        ChessBoardContainer(
                            fen = currentFen, theme = theme, pieceStyle = pieceStyle,
                            isPlayerWhite = !isBoardFlipped, selectedSquare = null, premove = null,
                            showThreatMap = false, threatMap = emptyMap(), visualArrows = visualArrows,
                            moveHistory = moveHistory, moveAnalysis = moveAnalysis,
                            currentMoveIndex = currentMoveIndex, onSquareClick = { _ -> }
                        )
                    }
                    AnalysisPlayerBar(
                        name = if (isPlayerWhite) userName else "Stockfish Lv $botLvl",
                        pieceSymbol = "♔", isWhite = true,
                        timerText = if (isPlayerWhite) whiteTimeStr else blackTimeStr, scheme = scheme
                    )
                    AnalysisBoardControls(
                        onFirst = { vm.goToMove(-1) }, onPrev = { vm.stepBackward() },
                        onNext = { vm.stepForward() }, onLast = { vm.goToMove(moveHistory.size - 1) },
                        onFlip = { isBoardFlipped = !isBoardFlipped }, onMistake = onMistake, scheme = scheme
                    )
                    Column(modifier = Modifier.fillMaxWidth().weight(1f).background(scheme.background)) {
                        AnalysisPanelContent(
                            selectedTab = selectedTab, onTabSelected = { selectedTab = it },
                            isAnalysisEnabled = isAnalysisEnabled, onAnalysisToggle = { isAnalysisEnabled = it },
                            evaluation = evaluation, currentFen = currentFen, openingName = openingName,
                            whiteLabel = whiteLabel, blackLabel = blackLabel, moveHistory = moveHistory,
                            moveAnalysis = moveAnalysis, currentMoveIndex = currentMoveIndex,
                            analysisResult = analysisResult, vm = vm,
                            practiceBannerMessage = practiceBannerMessage,
                            onDismissPracticeBanner = { practiceBannerMessage = null },
                            onShowReview = { vm.runGameAnalysis(); selectedTab = "Review" },
                            onAddGame = { showPgnImportDialog = true },
                            onSharePgn = { vm.sharePGN() },
                            scheme = scheme
                        )
                    }
                }
            }
        }
    }

    if (showPgnImportDialog) {
        ChessPgnFenImportDialog(
            onDismiss = { showPgnImportDialog = false },
            onImportToTraining = { pgnOrFen ->
                val session = com.creativeblisslabs.omnimind.chess.shared.GameSession(
                    mode = GameMode.TRAINING, label = "Custom Position", isRated = false
                )
                vm.startGame(session)
                if (pgnOrFen.contains("/")) vm.updateFen(pgnOrFen) else vm.loadPgn(pgnOrFen)
            },
            onImportToAnalysis = { pgnOrFen ->
                if (pgnOrFen.contains("/")) vm.updateFen(pgnOrFen) else vm.loadPgn(pgnOrFen)
                vm.runGameAnalysis()
            },
            onFilePickerRequested = { mediaActions.pickFile() },
            onImagePickerRequested = { mediaActions.pickImage() },
            onCameraPickerRequested = { mediaActions.pickImage() }
        )
    }
}

// ════════════════════════════════════════════════════════════
// Player info bar (theme-adaptive)
// ════════════════════════════════════════════════════════════
@Composable
private fun AnalysisPlayerBar(
    name: String, pieceSymbol: String, isWhite: Boolean,
    timerText: String, scheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).background(
                    if (isWhite) scheme.primary else scheme.surfaceVariant,
                    RoundedCornerShape(4.dp)
                ), contentAlignment = Alignment.Center
            ) {
                Text(pieceSymbol, fontSize = 14.sp,
                    color = if (isWhite) scheme.onPrimary else scheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
        }
        Surface(shape = RoundedCornerShape(4.dp), color = scheme.surfaceContainerHighest) {
            Text(timerText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
    }
}

// ════════════════════════════════════════════════════════════
// Board navigation controls (theme-adaptive)
// ════════════════════════════════════════════════════════════
@Composable
private fun AnalysisBoardControls(
    onFirst: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onLast: () -> Unit,
    onFlip: () -> Unit, onMistake: () -> Unit, scheme: ColorScheme
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("|<" to onFirst, "<" to onPrev, ">" to onNext, ">|" to onLast)
            .forEach { (label, action) ->
                Surface(
                    onClick = action, shape = RoundedCornerShape(8.dp),
                    color = scheme.surfaceContainerHigh,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = scheme.onSurface)
                    }
                }
            }
    }
}

// ════════════════════════════════════════════════════════════
// UNIFIED ANALYSIS PANEL — tabs: Analysis | Review | Games | Explore
// Identical in portrait and landscape
// ════════════════════════════════════════════════════════════
@Composable
internal fun AnalysisPanelContent(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    isAnalysisEnabled: Boolean,
    onAnalysisToggle: (Boolean) -> Unit,
    evaluation: com.creativeblisslabs.omnimind.core.chess.EngineEvaluation?,
    currentFen: String,
    openingName: String,
    whiteLabel: String,
    blackLabel: String,
    moveHistory: List<String>,
    moveAnalysis: List<com.creativeblisslabs.omnimind.core.chess.MoveAnalysis>,
    currentMoveIndex: Int,
    analysisResult: com.creativeblisslabs.omnimind.chess.shared.GameAnalysisResult? = null,
    vm: ChessViewModel? = null,
    practiceBannerMessage: String?,
    onDismissPracticeBanner: () -> Unit,
    onShowReview: () -> Unit,
    onAddGame: () -> Unit = {},
    onSharePgn: () -> Unit = {},
    scheme: ColorScheme
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // ── Tab row ──
        Row(modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerHigh)) {
            listOf(
                Triple("Analysis", Icons.Filled.Search, "Analysis"),
                Triple("Review",   Icons.Filled.Star,   "Review"),
                Triple("Games",    Icons.Filled.Movie,   "Games"),
                Triple("Explore",  Icons.Filled.Explore, "Explore")
            ).forEach { (tab, icon, label) ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier.weight(1f).clickable { onTabSelected(tab) }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(icon, label, tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(label, fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) scheme.primary else scheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ── Practice banner ──
        if (practiceBannerMessage != null) {
            Surface(color = scheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, "Practice", tint = scheme.secondary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(practiceBannerMessage, fontSize = 12.sp,
                            color = scheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Text("Dismiss", fontSize = 11.sp, color = scheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onDismissPracticeBanner() })
                }
            }
        }

        // ── Tab content ──
        when (selectedTab) {

            // ════════ REVIEW TAB — fully inlined, no separate screen ════════
            "Review" -> {
                // Auto-trigger analysis when Review tab is first opened
                LaunchedEffect(Unit) {
                    vm?.runGameAnalysis()
                }

                val summary = analysisResult?.reviewSummary ?: GameReviewSummary()
                val scrollState = rememberScrollState()

                if (analysisResult == null) {
                    // Loading state
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = scheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Analysing with Stockfish...", color = scheme.onSurfaceVariant, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This may take a few seconds depending on the number of moves.",
                            color = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // ── Full inline review content ──
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).background(scheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text("⭐", fontSize = 16.sp) }
                                Spacer(Modifier.width(10.dp))
                                Text("Game Review", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = scheme.onSurface)
                            }
                            TextButton(onClick = { onTabSelected("Analysis") }) {
                                Text("← Analysis", fontSize = 11.sp, color = scheme.primary)
                            }
                        }

                        // Coach comment
                        if (summary.coachComment.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = scheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🤖", fontSize = 24.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Text(summary.coachComment, fontSize = 13.sp,
                                        color = scheme.onSurface, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Eval graph
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        ) {
                            val evals = summary.evalGraph
                            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                val w = size.width; val h = size.height
                                drawLine(Color.Gray.copy(alpha = 0.4f), Offset(0f, h / 2), Offset(w, h / 2), 1.dp.toPx())
                                if (evals.size >= 2) {
                                    val stepX = w / (evals.size - 1)
                                    val fillPath = Path()
                                    evals.forEachIndexed { i, score ->
                                        val y = h / 2 - (score.coerceIn(-10.0, 10.0).toFloat() / 10f) * (h / 2 - 4.dp.toPx())
                                        val x = i * stepX
                                        if (i == 0) fillPath.moveTo(x, y) else fillPath.lineTo(x, y)
                                    }
                                    fillPath.lineTo((evals.size - 1) * stepX, h / 2)
                                    fillPath.lineTo(0f, h / 2); fillPath.close()
                                    drawPath(fillPath, Color(0xFF90A4AE).copy(alpha = 0.3f))
                                    val strokePath = Path()
                                    evals.forEachIndexed { i, score ->
                                        val y = h / 2 - (score.coerceIn(-10.0, 10.0).toFloat() / 10f) * (h / 2 - 4.dp.toPx())
                                        val x = i * stepX
                                        if (i == 0) strokePath.moveTo(x, y) else strokePath.lineTo(x, y)
                                    }
                                    drawPath(strokePath, Color(0xFF90A4AE), style = Stroke(width = 2.dp.toPx()))
                                    evals.forEachIndexed { i, score ->
                                        if (i % 4 == 0) {
                                            val y = h / 2 - (score.coerceIn(-10.0, 10.0).toFloat() / 10f) * (h / 2 - 4.dp.toPx())
                                            val x = i * stepX
                                            val dotColor = when {
                                                score > 2.0  -> Color(0xFF26C6DA)
                                                score > 0.3  -> Color(0xFF81B64C)
                                                score > -0.5 -> Color(0xFFFF9800)
                                                else         -> Color(0xFFEF5350)
                                            }
                                            drawCircle(dotColor, radius = 3.dp.toPx(), center = Offset(x, y))
                                        }
                                    }
                                }
                            }
                        }

                        // Summary table
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                // Players header
                                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("○", fontSize = 18.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                        Text(summary.whitePlayerName.ifBlank { "White" }, fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold, color = scheme.onSurface, textAlign = TextAlign.Center)
                                    }
                                    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
                                        Text("vs", fontSize = 13.sp, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                    }
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("●", fontSize = 18.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                        Text(summary.blackPlayerName.ifBlank { "Black" }, fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold, color = scheme.onSurface, textAlign = TextAlign.Center)
                                    }
                                }

                                HorizontalDivider(color = scheme.onSurfaceVariant.copy(alpha = 0.2f), thickness = 0.5.dp)
                                Spacer(Modifier.height(6.dp))

                                // Accuracy
                                ReviewSummaryRow("${summary.whiteAccuracy.toInt()}%", "Accuracy", "${summary.blackAccuracy.toInt()}%",
                                    accuracyColor(summary.whiteAccuracy.toInt()), accuracyColor(summary.blackAccuracy.toInt()),
                                    labelIcon = true, scheme = scheme)

                                // Phase accuracy
                                summary.whiteOpeningAccuracy?.let { w -> summary.blackOpeningAccuracy?.let { b ->
                                    ReviewSummaryRow("$w%", "Opening", "$b%", accuracyColor(w), accuracyColor(b), scheme = scheme) }}
                                summary.whiteMiddlegameAccuracy?.let { w -> summary.blackMiddlegameAccuracy?.let { b ->
                                    ReviewSummaryRow("$w%", "Middlegame", "$b%", accuracyColor(w), accuracyColor(b), scheme = scheme) }}
                                summary.whiteEndgameAccuracy?.let { w -> summary.blackEndgameAccuracy?.let { b ->
                                    ReviewSummaryRow("$w%", "Endgame", "$b%", accuracyColor(w), accuracyColor(b), scheme = scheme) }}

                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = scheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 0.5.dp)
                                Spacer(Modifier.height(4.dp))

                                // Move classifications
                                ReviewSummaryRow(
                                    summary.whiteInaccuracies.toString(), "Inaccuracy", summary.blackInaccuracies.toString(),
                                    if (summary.whiteInaccuracies > 0) Color(0xFFFF9800) else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    if (summary.blackInaccuracies > 0) Color(0xFFFF9800) else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    badgeEmoji = "?!", badgeColor = Color(0xFFFF9800), scheme = scheme)
                                ReviewSummaryRow(
                                    summary.whiteMistakes.toString(), "Mistake", summary.blackMistakes.toString(),
                                    if (summary.whiteMistakes > 0) Color(0xFFFF5722) else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    if (summary.blackMistakes > 0) Color(0xFFFF5722) else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    badgeEmoji = "?", badgeColor = Color(0xFFFF5722), scheme = scheme)
                                ReviewSummaryRow(
                                    summary.whiteBlunders.toString(), "Blunder", summary.blackBlunders.toString(),
                                    if (summary.whiteBlunders > 0) Color(0xFFEF5350) else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    if (summary.blackBlunders > 0) Color(0xFFEF5350) else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    badgeEmoji = "??", badgeColor = Color(0xFFEF5350), scheme = scheme)

                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = scheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 0.5.dp)
                                Spacer(Modifier.height(4.dp))

                                ReviewSummaryRow(summary.whiteAcpl.toString(), "Avg. Cp loss", summary.blackAcpl.toString(), scheme = scheme)

                                if (summary.whiteEstimatedElo > 0 || summary.blackEstimatedElo > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    HorizontalDivider(color = scheme.onSurfaceVariant.copy(alpha = 0.15f), thickness = 0.5.dp)
                                    Spacer(Modifier.height(4.dp))
                                    ReviewSummaryRow(
                                        if (summary.whiteEstimatedElo > 0) summary.whiteEstimatedElo.toString() else "–",
                                        "Game Rating",
                                        if (summary.blackEstimatedElo > 0) summary.blackEstimatedElo.toString() else "–",
                                        scheme = scheme)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }

                        // Brilliant / Great highlights
                        if (summary.whiteBrilliants + summary.blackBrilliants + summary.whiteGreats + summary.blackGreats > 0) {
                            Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Best Moves", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                                    if (summary.whiteBrilliants > 0 || summary.blackBrilliants > 0) {
                                        ReviewSummaryRow(
                                            summary.whiteBrilliants.toString(), "Brilliant ‼",
                                            summary.blackBrilliants.toString(),
                                            if (summary.whiteBrilliants > 0) Color(0xFF26C6DA) else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            if (summary.blackBrilliants > 0) Color(0xFF26C6DA) else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            scheme = scheme)
                                    }
                                    if (summary.whiteGreats > 0 || summary.blackGreats > 0) {
                                        ReviewSummaryRow(
                                            summary.whiteGreats.toString(), "Great !",
                                            summary.blackGreats.toString(),
                                            if (summary.whiteGreats > 0) Color(0xFF5C6BC0) else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            if (summary.blackGreats > 0) Color(0xFF5C6BC0) else scheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            scheme = scheme)
                                    }
                                }
                            }
                        }

                        // "Go to Analysis" action button
                        Button(
                            onClick = { onTabSelected("Analysis") },
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Filled.Search, null, tint = scheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Back to Analysis", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimary)
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // ════════ GAMES TAB ════════
            "Games" -> {
                val gameHistoryList by (vm?.gameHistory?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(emptyList()) })

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gameHistoryList.isEmpty()) {
                        item {
                            Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Movie, "Games", tint = scheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("No Saved Games Yet", fontWeight = FontWeight.Bold, color = scheme.onSurface)
                                    Text("Played games will automatically be saved here for review.",
                                        fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        items(gameHistoryList) { game ->
                            Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth().clickable { vm?.loadGameFromHistory(game) }) {
                                Row(modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${game.whitePlayer} vs ${game.blackPlayer}",
                                            fontWeight = FontWeight.Bold, color = scheme.onSurface, fontSize = 13.sp)
                                        val dateStr = try {
                                            kotlinx.datetime.Instant.fromEpochMilliseconds(game.date)
                                                .toString().substringBefore("T")
                                        } catch (e: Exception) { "Recent Game" }
                                        Text(dateStr, fontSize = 11.sp, color = scheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(game.result, fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 13.sp)
                                        Spacer(Modifier.width(12.dp))
                                        IconButton(onClick = { vm?.deleteGameFromHistory(game.id) },
                                            modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, "Delete", tint = scheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ════════ EXPLORE TAB ════════
            "Explore" -> {
                val openingExplorerData by (vm?.openingExplorerData?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(null) })
                val isMasters by (vm?.isOpeningMastersDatabase?.collectAsStateWithLifecycle()
                    ?: remember { mutableStateOf(true) })

                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Opening Explorer", fontWeight = FontWeight.Bold, color = scheme.onSurface, fontSize = 14.sp)
                            val ecoStr = openingExplorerData?.opening?.eco ?: ""
                            val nameStr = openingExplorerData?.opening?.name ?: openingName
                            Text(if (ecoStr.isNotEmpty()) "[$ecoStr] $nameStr" else nameStr,
                                fontSize = 12.sp, color = scheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(selected = isMasters, onClick = { vm?.toggleOpeningDatabaseSource(true) },
                                label = { Text("Masters", fontSize = 10.sp) })
                            FilterChip(selected = !isMasters, onClick = { vm?.toggleOpeningDatabaseSource(false) },
                                label = { Text("Lichess", fontSize = 10.sp) })
                        }
                    }

                    val movesList = openingExplorerData?.moves ?: emptyList()
                    val topGamesList = openingExplorerData?.topGames ?: emptyList()

                    if (movesList.isEmpty() && topGamesList.isEmpty()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Searching Online Database...", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (movesList.isNotEmpty()) {
                                item {
                                    Text("Candidate Moves", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        color = scheme.primary, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
                                }
                                items(movesList) { item ->
                                    val total = (item.white + item.draws + item.black).coerceAtLeast(1)
                                    val wPct = (item.white * 100 / total)
                                    val dPct = (item.draws * 100 / total)
                                    val bPct = (item.black * 100 / total)
                                    Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceContainerHigh,
                                        modifier = Modifier.fillMaxWidth().clickable { vm?.makeMove(item.uci, item.san) }) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(item.san, fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 15.sp)
                                                    Spacer(Modifier.width(10.dp))
                                                    Text("$total games", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                                                }
                                                Text("$wPct% / $dPct% / $bPct%", fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold, color = scheme.onSurface)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth().height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)).background(scheme.surfaceContainerLowest)) {
                                                if (wPct > 0) Box(modifier = Modifier.weight(wPct.toFloat().coerceAtLeast(1f))
                                                    .fillMaxHeight().background(Color(0xFF629924)))
                                                if (dPct > 0) Box(modifier = Modifier.weight(dPct.toFloat().coerceAtLeast(1f))
                                                    .fillMaxHeight().background(scheme.onSurfaceVariant.copy(alpha = 0.5f)))
                                                if (bPct > 0) Box(modifier = Modifier.weight(bPct.toFloat().coerceAtLeast(1f))
                                                    .fillMaxHeight().background(Color(0xFF36485E)))
                                            }
                                        }
                                    }
                                }
                            }
                            if (topGamesList.isNotEmpty()) {
                                item {
                                    Text("Top Master & Database Games", fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        color = scheme.secondary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                                }
                                items(topGamesList) { game ->
                                    val whiteName = game.white.name.ifBlank { "White" }
                                    val blackName = game.black.name.ifBlank { "Black" }
                                    val whiteRating = if (game.white.rating > 0) " (${game.white.rating})" else ""
                                    val blackRating = if (game.black.rating > 0) " (${game.black.rating})" else ""
                                    val resultStr = when (game.winner) { "white" -> "1-0"; "black" -> "0-1"; else -> "½-½" }
                                    Surface(shape = RoundedCornerShape(6.dp), color = scheme.surfaceContainerLow,
                                        modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("$whiteName$whiteRating vs $blackName$blackRating",
                                                    fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface)
                                                if (game.year > 0) Text("Played in ${game.year}", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                                            }
                                            Surface(shape = RoundedCornerShape(4.dp), color = scheme.secondaryContainer,
                                                modifier = Modifier.padding(start = 8.dp)) {
                                                Text(resultStr, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                                    color = scheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ════════ ANALYSIS TAB (default) ════════
            else -> {
                // Engine toggle row
                Row(
                    modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerHigh)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isAnalysisEnabled, onCheckedChange = onAnalysisToggle,
                            colors = SwitchDefaults.colors(checkedThumbColor = scheme.primary))
                        Spacer(Modifier.width(6.dp))
                        Text("Stockfish", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                    Text(evaluation?.let { "depth ${it.depth}" } ?: "–", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                }

                // Engine lines inside panel (3 lines)
                if (isAnalysisEnabled) {
                    LichessEngineLines(evaluation = evaluation, currentFen = currentFen, numLines = 3)
                }

                // Opening name bar
                Row(modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(openingName, fontSize = 12.sp, color = scheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    Icon(Icons.Filled.Book, "Book", tint = scheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                }

                // Players & Active Turn bar
                Row(modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("$whiteLabel  vs  $blackLabel", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)

                    val isWhiteTurn = !currentFen.contains(" b ")
                    Surface(
                        onClick = {
                            val parts = currentFen.split(" ").toMutableList()
                            if (parts.size >= 2) {
                                parts[1] = if (parts[1] == "w") "b" else "w"
                                vm?.updateFen(parts.joinToString(" "))
                            }
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isWhiteTurn) Color(0xFFF0D9B5) else Color(0xFF333333)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                if (isWhiteTurn) "⚪ White to Play" else "⚫ Black to Play",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWhiteTurn) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Move history (scrollable, takes remaining space)
                ChessVerticalMoveHistoryTable(
                    moveHistory = moveHistory, moveAnalysis = moveAnalysis,
                    currentMoveIndex = currentMoveIndex, evaluation = evaluation,
                    onSelectMove = { idx -> vm?.goToMove(idx) },
                    modifier = Modifier.weight(1f)
                )

                // Bottom action bar
                Row(modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    AnalysisActionBtn(Icons.Filled.Search, "New", onClick = { vm?.resetGame() }, scheme = scheme)
                    AnalysisActionBtn(Icons.Filled.FileDownload, "Save", onClick = onSharePgn, scheme = scheme)
                    AnalysisActionBtn(Icons.Filled.Star, "Review", onClick = onShowReview,
                        tint = Color(0xFF81B64C), scheme = scheme)
                    AnalysisActionBtn(Icons.Filled.Add, "Import", onClick = onAddGame, scheme = scheme)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════
// 3-column summary row used in Review tab
// ════════════════════════════════════════════════════════════
@Composable
private fun ReviewSummaryRow(
    whiteText: String, label: String, blackText: String,
    whiteColor: Color? = null, blackColor: Color? = null,
    badgeEmoji: String? = null, badgeColor: Color? = null,
    labelIcon: Boolean = false, scheme: ColorScheme
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(whiteText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = whiteColor ?: scheme.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)

        Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (badgeEmoji != null && badgeColor != null) {
                    Box(modifier = Modifier.size(20.dp).background(badgeColor, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text(badgeEmoji, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(label, fontSize = 12.sp, color = scheme.onSurface,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                if (labelIcon) {
                    Spacer(Modifier.width(3.dp))
                    Icon(Icons.Filled.Info, null, modifier = Modifier.size(12.dp), tint = scheme.primary)
                }
            }
        }

        Text(blackText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            color = blackColor ?: scheme.onSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}

// ════════════════════════════════════════════════════════════
// Bottom action button used in Analysis tab
// ════════════════════════════════════════════════════════════
@Composable
private fun AnalysisActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    onClick: () -> Unit, tint: Color? = null, scheme: ColorScheme
) {
    Column(modifier = Modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = tint ?: scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = tint ?: scheme.onSurfaceVariant,
            fontWeight = if (tint != null) FontWeight.Bold else FontWeight.Normal)
    }
}
