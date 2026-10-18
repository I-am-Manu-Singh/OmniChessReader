package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.chess.shared.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.core.chess.GameMode

@Composable
fun PuzzlesTabContent(
    vm: ChessViewModel,
    onSwitchToTab: (String) -> Unit = {}
) {
    val puzzles by vm.puzzles.collectAsStateWithLifecycle()
    val puzzleFeedback by vm.puzzleFeedback.collectAsStateWithLifecycle()
    val currentPuzzleIndex by vm.currentPuzzleIndex.collectAsStateWithLifecycle()
    val lichessPuzzle by vm.lichessPuzzle.collectAsStateWithLifecycle()
    val chessComPuzzle by vm.chessComPuzzle.collectAsStateWithLifecycle()
    val isLichessLoading by vm.isLichessLoading.collectAsStateWithLifecycle()
    val currentFen by vm.fen.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val threatMap by vm.threatMap.collectAsStateWithLifecycle()
    val showThreatMap by vm.showThreatMap.collectAsStateWithLifecycle()
    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()
    val moveHistory by vm.moveHistory.collectAsStateWithLifecycle()
    val puzzleStormScore by vm.puzzleStormScore.collectAsStateWithLifecycle()
    val puzzleStormTimeLeft by vm.puzzleStormTimeLeft.collectAsStateWithLifecycle()
    val puzzleStreak by vm.puzzleStreak.collectAsStateWithLifecycle()
    val gameMode by vm.gameMode.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var selectedThemeTitle by remember { mutableStateOf<String?>(null) }

    var phasesExpanded by remember { mutableStateOf(false) }
    var motifsExpanded by remember { mutableStateOf(false) }
    var advancedExpanded by remember { mutableStateOf(false) }
    var matesExpanded by remember { mutableStateOf(false) }
    var specialMovesExpanded by remember { mutableStateOf(false) }
    var goalsExpanded by remember { mutableStateOf(false) }
    var lengthsExpanded by remember { mutableStateOf(false) }
    var originExpanded by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Reset board on entry so moves from Game tab don't carry over into puzzle boards
    LaunchedEffect(Unit) {
        vm.resetGame()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Tactical Modes, Themes & Daily Feeds
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Action Mode Switchers
                    listOf(
                        Triple("Puzzle Streak", "Solve progressively harder puzzles and build a win streak.", Icons.Filled.TrendingUp),
                        Triple("Puzzle Storm", "Solve as many puzzles as possible in 3 minutes.", Icons.Filled.ElectricBolt)
                    ).forEach { (title, desc, icon) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x1F888888),
                            border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    when (title) {
                                        "Puzzle Streak" -> vm.startPuzzleStreak()
                                        "Puzzle Storm" -> vm.startPuzzleStorm()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(24.dp))
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = scheme.onSurface)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Collapsible Puzzle Themes Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = if (selectedThemeTitle != null) "Theme: $selectedThemeTitle" else "Explore Puzzle Themes",
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                                fontSize = 15.sp
                            )

                            CollapsiblePuzzleCategory(
                                title = "Phases",
                                expanded = phasesExpanded,
                                onToggle = { phasesExpanded = !phasesExpanded },
                                items = listOf(
                                    Triple("Opening", "A tactic during the first phase of the game.", "320995"),
                                    Triple("Middlegame", "A tactic during the second phase of the game.", "2912087"),
                                    Triple("Endgame", "A tactic during the last phase of the game.", "3160026")
                                ),
                                primaryColor = scheme.primary,
                                onThemeClick = { themeTitle ->
                                    selectedThemeTitle = themeTitle
                                    vm.loadPuzzleByTheme(themeTitle)
                                }
                            )
                        }
                    }

                    // Daily Feeds (Lichess & Chess.com)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(220.dp)
                                .clickable {
                                    if (lichessPuzzle != null) vm.loadPuzzlePosition(lichessPuzzle!!.fen, lichessPuzzle!!.solution)
                                    else vm.fetchLichessPuzzleOfDay()
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Lichess Daily", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.onSurface)
                                Text(if (lichessPuzzle != null) "Rating: ${lichessPuzzle!!.rating}" else "-", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        if (lichessPuzzle != null) vm.loadPuzzlePosition(lichessPuzzle!!.fen, lichessPuzzle!!.solution)
                                        else vm.fetchLichessPuzzleOfDay()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Play Lichess", fontSize = 10.sp) }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(220.dp)
                                .clickable {
                                    chessComPuzzle?.let { vm.loadPuzzlePosition(it.fen) } ?: vm.fetchChessComDailyPuzzle()
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Chess.com Daily", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.onSurface)
                                Text(chessComPuzzle?.rating?.let { "Rating: $it" } ?: "Rating: —", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        chessComPuzzle?.let { vm.loadPuzzlePosition(it.fen) } ?: vm.fetchChessComDailyPuzzle()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(30.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Play Chess.com", fontSize = 10.sp) }
                            }
                        }
                    }
                }

                // Right Column: Active Puzzle Board & Interactive Control Bar
                Column(
                    modifier = Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val turnSide = if (ChessLogic.parseFen(currentFen).isWhiteTurn) "White to play" else "Black to play"
                            val activePuzzle = puzzles.getOrNull(currentPuzzleIndex)
                            val ratingValue = activePuzzle?.rating ?: lichessPuzzle?.rating ?: 0

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Chess Puzzles — Rating: $ratingValue",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.primary
                                )
                                Surface(
                                    color = if (turnSide.contains("White")) scheme.onSurface.copy(alpha = 0.15f) else scheme.onSurface.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(0.5.dp, Color.Gray)
                                ) {
                                    Text(
                                        text = turnSide,
                                        color = scheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = activePuzzle?.instruction ?: "Find the best move. ($turnSide)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = scheme.onSurfaceVariant
                            )

                            ChessBoardContainer(
                                fen = currentFen,
                                theme = theme,
                                isPlayerWhite = isPlayerWhite,
                                selectedSquare = selectedSquare,
                                premove = null,
                                showThreatMap = showThreatMap,
                                threatMap = threatMap,
                                pieceStyle = pieceStyle,
                                visualArrows = visualArrows,
                                engineEvaluation = evaluation,
                                moveHistory = moveHistory,
                                onSquareClick = { clicked ->
                                    val currentSelected = selectedSquare
                                    if (currentSelected == null) {
                                        if (ChessLogic.parseFen(currentFen).pieces[clicked] != null) {
                                            selectedSquare = clicked
                                        }
                                    } else {
                                        if (currentSelected != clicked) {
                                            vm.submitPuzzleMove(currentSelected, clicked)
                                        }
                                        selectedSquare = null
                                    }
                                }
                            )

                            ReusableChessControlBar(
                                moveHistory = moveHistory,
                                currentMoveIndex = moveHistory.size - 1,
                                onSelectMove = { index -> vm.goToMove(index) },
                                onFirstMove = { vm.goToMove(0) },
                                onPrevMove = { vm.undoLastMove() },
                                onNextMove = { vm.redoNextMove() },
                                onLastMove = { vm.goToMove((moveHistory.size - 1).coerceAtLeast(0)) },
                                onFlipBoard = { vm.setPlayerColor(!isPlayerWhite) },
                                onResetBoard = { vm.resetGame() }
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Action Mode Switchers
                listOf(
                    Triple("Puzzle Streak", "Solve progressively harder puzzles and build a win streak.", Icons.Filled.TrendingUp),
                    Triple("Puzzle Storm", "Solve as many puzzles as possible in 3 minutes.", Icons.Filled.ElectricBolt)
                ).forEach { (title, desc, icon) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x1F888888),
                        border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (title) {
                                    "Puzzle Streak" -> vm.startPuzzleStreak()
                                    "Puzzle Storm" -> vm.startPuzzleStorm()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, null, tint = scheme.primary, modifier = Modifier.size(24.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Comprehensive Collapsible Puzzle Themes Categories Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (selectedThemeTitle != null) "Theme: $selectedThemeTitle" else "Explore Puzzle Themes",
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary,
                            fontSize = 15.sp
                        )

                        CollapsiblePuzzleCategory(
                            title = "Game Phases",
                            expanded = phasesExpanded,
                            onToggle = { phasesExpanded = !phasesExpanded },
                            items = listOf(
                                Triple("Opening", "A tactic during the first phase of the game.", "320,995"),
                                Triple("Middlegame", "A tactic during the second phase of the game.", "2,912,087"),
                                Triple("Endgame", "A tactic during the last phase of the game.", "3,160,026")
                            ),
                            primaryColor = scheme.primary,
                            onThemeClick = { themeTitle ->
                                selectedThemeTitle = themeTitle
                                vm.loadPuzzleByTheme(themeTitle)
                            }
                        )

                        CollapsiblePuzzleCategory(
                            title = "Endgames",
                            expanded = advancedExpanded,
                            onToggle = { advancedExpanded = !advancedExpanded },
                            items = listOf(
                                Triple("Pawn Endgame", "Endgames with only pawns and kings remaining.", "412,091"),
                                Triple("Knight Endgame", "Endgames involving knights and pawns.", "128,450"),
                                Triple("Bishop Endgame", "Endgames featuring opposite or same-colored bishops.", "165,820"),
                                Triple("Rook Endgame", "Endgames involving rooks and pawns.", "890,210"),
                                Triple("Queen Endgame", "Endgames with queens and pawns.", "215,900")
                            ),
                            primaryColor = scheme.secondary,
                            onThemeClick = { themeTitle ->
                                selectedThemeTitle = themeTitle
                                vm.loadPuzzleByTheme(themeTitle)
                            }
                        )

                        CollapsiblePuzzleCategory(
                            title = "Tactical Motifs",
                            expanded = motifsExpanded,
                            onToggle = { motifsExpanded = !motifsExpanded },
                            items = listOf(
                                Triple("Pin", "Restricting a piece because moving it exposes a higher value target.", "1,240,500"),
                                Triple("Fork", "Attacking two or more enemy pieces simultaneously.", "1,850,300"),
                                Triple("Skewer", "Attacking a valuable piece, forcing it to move and exposing a piece behind it.", "340,120"),
                                Triple("Discovered Attack", "Moving a piece to open an attack from a teammate behind it.", "980,450"),
                                Triple("Double Check", "Checking the opponent king with two pieces at once.", "120,600"),
                                Triple("Deflection", "Forcing an opponent piece away from guarding an important square.", "650,890"),
                                Triple("Attraction", "Luring an enemy piece to a square where it can be attacked.", "410,230"),
                                Triple("Intermezzo", "Inserting an unexpected move before executing an expected action.", "180,950")
                            ),
                            primaryColor = scheme.tertiary,
                            onThemeClick = { themeTitle ->
                                selectedThemeTitle = themeTitle
                                vm.loadPuzzleByTheme(themeTitle)
                            }
                        )

                        CollapsiblePuzzleCategory(
                            title = "Mating Patterns",
                            expanded = matesExpanded,
                            onToggle = { matesExpanded = !matesExpanded },
                            items = listOf(
                                Triple("Back Rank Mate", "Checkmating a king trapped behind its own pawn shield on the 1st or 8th rank.", "520,300"),
                                Triple("Anastasia Mate", "A knight and rook trap the enemy king against the edge of the board.", "95,400"),
                                Triple("Arabian Mate", "A knight and rook coordinate on the corner to deliver checkmate.", "112,800"),
                                Triple("Boden Mate", "Crisscrossing bishops deliver checkmate to an exposed king.", "48,900"),
                                Triple("Smothered Mate", "A knight checkmates a king completely surrounded by its own pieces.", "62,100"),
                                Triple("Hook Mate", "Rook, knight, and pawn lock an enemy king in checkmate.", "39,500")
                            ),
                            primaryColor = Color(0xFFEF5350),
                            onThemeClick = { themeTitle ->
                                selectedThemeTitle = themeTitle
                                vm.loadPuzzleByTheme(themeTitle)
                            }
                        )

                        CollapsiblePuzzleCategory(
                            title = "Special Rules",
                            expanded = specialMovesExpanded,
                            onToggle = { specialMovesExpanded = !specialMovesExpanded },
                            items = listOf(
                                Triple("En Passant", "Tactics featuring the special pawn capture rule.", "28,400"),
                                Triple("Castling", "Tactics involving king safety through castling.", "85,600"),
                                Triple("Promotion", "Promoting a pawn to a queen or other piece to win the game.", "310,200"),
                                Triple("Under promotion", "Promoting a pawn to a knight, rook, or bishop instead of a queen.", "42,100")
                            ),
                            primaryColor = Color(0xFFFFB74D),
                            onThemeClick = { themeTitle ->
                                selectedThemeTitle = themeTitle
                                vm.loadPuzzleByTheme(themeTitle)
                            }
                        )
                    }
                }

                Text("Chess tactics trainer", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.onSurface)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- LEFT CARD: LICHESS DAILY PUZZLE ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(260.dp)
                            .clickable {
                                if (lichessPuzzle != null) {
                                    vm.loadPuzzlePosition(lichessPuzzle!!.fen, lichessPuzzle!!.solution)
                                } else {
                                    vm.fetchLichessPuzzleOfDay()
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lichess Daily", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { vm.fetchLichessPuzzleOfDay() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh Lichess Puzzle",
                                        tint = scheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (lichessPuzzle != null) "Rating: ${lichessPuzzle!!.rating}" else "-",
                                fontSize = 11.sp,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFB58863)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLichessLoading && lichessPuzzle == null) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = scheme.onSurface)
                                } else if (lichessPuzzle != null) {
                                    ChessBoardContainer(
                                        fen = lichessPuzzle!!.fen,
                                        theme = theme,
                                        pieceStyle = pieceStyle,
                                        isPlayerWhite = true,
                                        selectedSquare = null,
                                        premove = null,
                                        showThreatMap = false,
                                        threatMap = emptyMap(),
                                        onSquareClick = {}
                                    )
                                } else {
                                    Text("♞", fontSize = 36.sp, color = scheme.onSurface)
                                }
                            }

                            if (lichessPuzzle != null) {
                                Text(
                                    text = "${lichessPuzzle!!.playerBlack} vs ${lichessPuzzle!!.playerWhite}",
                                    fontSize = 10.sp,
                                    color = scheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = lichessPuzzle!!.themes.take(2).joinToString(" · ").ifEmpty { "Tactics" },
                                    fontSize = 10.sp,
                                    color = scheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text("Live Lichess Puzzle", fontSize = 10.sp, color = Color.DarkGray, maxLines = 1)
                                Text("Online API", fontSize = 10.sp, color = Color.DarkGray, maxLines = 1)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    if (lichessPuzzle != null) vm.loadPuzzlePosition(lichessPuzzle!!.fen, lichessPuzzle!!.solution)
                                    else vm.fetchLichessPuzzleOfDay()
                                },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (lichessPuzzle != null) "Play Lichess" else "Load Lichess", fontSize = 11.sp)
                            }
                        }
                    }

                    // --- RIGHT CARD: CHESS.COM FEED PUZZLE ---
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(260.dp)
                            .clickable {
                                chessComPuzzle?.let { vm.loadPuzzlePosition(it.fen) } ?: vm.fetchChessComDailyPuzzle()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chess.com Feed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(
                                    onClick = { vm.fetchChessComDailyPuzzle() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh Chess.com Puzzle",
                                        tint = scheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = chessComPuzzle?.rating?.let { "Rating: $it" } ?: "Rating: —",
                                fontSize = 11.sp,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF769656)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (chessComPuzzle != null) {
                                    ChessBoardContainer(
                                        fen = chessComPuzzle!!.fen,
                                        theme = theme,
                                        pieceStyle = pieceStyle,
                                        isPlayerWhite = true,
                                        selectedSquare = null,
                                        premove = null,
                                        showThreatMap = false,
                                        threatMap = emptyMap(),
                                        onSquareClick = {}
                                    )
                                } else {
                                    Text("♜", fontSize = 36.sp, color = scheme.onSurface)
                                }
                            }

                            Text(
                                text = chessComPuzzle?.title ?: "Tactics & Endgames",
                                fontSize = 10.sp,
                                color = scheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Daily Tactics",
                                fontSize = 10.sp,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    chessComPuzzle?.let { vm.loadPuzzlePosition(it.fen) } ?: vm.fetchChessComDailyPuzzle()
                                },
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (chessComPuzzle != null) "Play Chess.com" else "Load Chess.com", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Active Puzzle Playing Area
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val turnSide = if (ChessLogic.parseFen(currentFen).isWhiteTurn) "White to play" else "Black to play"
                        val activePuzzle = puzzles.getOrNull(currentPuzzleIndex)
                        val ratingValue = activePuzzle?.rating ?: lichessPuzzle?.rating ?: 0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Chess Puzzles — Rating: $ratingValue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary
                            )
                            Surface(
                                color = if (turnSide.contains("White")) scheme.onSurface.copy(alpha = 0.15f) else scheme.onSurface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, Color.Gray)
                            ) {
                                Text(
                                    text = turnSide,
                                    color = scheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = activePuzzle?.instruction ?: "Find the best move. ($turnSide)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurfaceVariant
                        )

                        if (gameMode == GameMode.PUZZLE_STORM) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Time: ${puzzleStormTimeLeft}s", color = if (puzzleStormTimeLeft < 30) scheme.error else scheme.primary, fontWeight = FontWeight.Bold)
                                Text("Score: $puzzleStormScore", color = scheme.secondary, fontWeight = FontWeight.Bold)
                            }
                        } else if (gameMode == GameMode.PUZZLE_STREAK) {
                            Text("Current Streak: $puzzleStreak", color = scheme.secondary, fontWeight = FontWeight.Bold)
                        }

                        ChessBoardContainer(
                            fen = currentFen,
                            theme = theme,
                            isPlayerWhite = isPlayerWhite,
                            selectedSquare = selectedSquare,
                            premove = null,
                            showThreatMap = showThreatMap,
                            threatMap = threatMap,
                            pieceStyle = pieceStyle,
                            visualArrows = visualArrows,
                            engineEvaluation = evaluation,
                            moveHistory = moveHistory,
                            onSquareClick = { clicked ->
                                val currentSelected = selectedSquare
                                if (currentSelected == null) {
                                    if (ChessLogic.parseFen(currentFen).pieces[clicked] != null) {
                                        selectedSquare = clicked
                                    }
                                } else {
                                    if (currentSelected != clicked) {
                                        vm.submitPuzzleMove(currentSelected, clicked)
                                    }
                                    selectedSquare = null
                                }
                            }
                        )

                        if (puzzleFeedback.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (puzzleFeedback.contains("Success") || puzzleFeedback.contains("Correct")) scheme.primaryContainer else scheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    puzzleFeedback,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // --- REUSABLE CHESS CONTROL BAR INTEGRATED HERE ---
                        ReusableChessControlBar(
                            moveHistory = moveHistory,
                            currentMoveIndex = moveHistory.size - 1,
                            onSelectMove = { index ->
                                vm.goToMove(index)
                            },
                            onFirstMove = {
                                vm.goToMove(0)
                            },
                            onPrevMove = {
                                vm.undoLastMove()
                            },
                            onNextMove = {
                                vm.redoNextMove()
                            },
                            onLastMove = {
                                vm.goToMove((moveHistory.size - 1).coerceAtLeast(0))
                            },
                            onFlipBoard = {
                                vm.setPlayerColor(!isPlayerWhite)
                            },
                            onResetBoard = {
                                vm.resetGame()
                            }
                        )

                        // Secondary Puzzle Skipping & Hint Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val prevIdx = if (currentPuzzleIndex > 0) currentPuzzleIndex - 1 else puzzles.size - 1
                                    vm.loadPuzzle(prevIdx)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.SkipPrevious, "Previous Puzzle", tint = scheme.onSurface, modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    vm.showPuzzleHint()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.Lightbulb, "Hint", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    val nextIdx = (currentPuzzleIndex + 1) % puzzles.size
                                    vm.loadPuzzle(nextIdx)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.SkipNext, "Next Puzzle", tint = scheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CollapsiblePuzzleCategory(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    items: List<Triple<String, String, String>>,
    primaryColor: Color,
    onThemeClick: (String) -> Unit
) {
        val scheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = scheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items.forEach { (itemTitle, desc, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeClick(itemTitle) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Extension,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = itemTitle,
                                    color = scheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = desc,
                                    color = scheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = count,
                            color = scheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}