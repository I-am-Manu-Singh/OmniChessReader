package com.creativeblisslabs.omnimind.feature.chess

import com.creativeblisslabs.omnimind.core.chess.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.core.ui.components.ProductHeader
import com.creativeblisslabs.omnimind.core.ui.components.StatusPill
import com.creativeblisslabs.omnimind.core.chess.*
import androidx.compose.animation.core.*
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.chess.shared.*
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.abs

import androidx.compose.ui.text.style.TextOverflow
import com.creativeblisslabs.omnimind.core.chess.MultiPVLine



@Composable
fun PGNViewer(
    movePairs: List<Triple<Int, String, String>>,
    currentIndex: Int,
    onMoveClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
        val scheme = MaterialTheme.colorScheme

    // Auto-scroll to current move
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            val pairIndex = currentIndex / 2
            // Approximate scroll position
            scrollState.animateScrollTo(pairIndex * 40) 
        }
    }

    Column(
        modifier = modifier
            .background(Color(0xFF161618), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(
            "Move History",
            color = scheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            movePairs.forEach { (num, white, black) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$num.",
                        color = scheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.width(28.dp)
                    )
                    
                    MoveItem(
                        move = white,
                        isSelected = currentIndex == (num - 1) * 2,
                        onClick = { onMoveClick((num - 1) * 2) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(Modifier.width(4.dp))
                    
                    if (black.isNotEmpty()) {
                        MoveItem(
                            move = black,
                            isSelected = currentIndex == (num - 1) * 2 + 1,
                            onClick = { onMoveClick((num - 1) * 2 + 1) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveItem(
    move: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFFE8883A).copy(alpha = 0.2f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = move,
            color = if (isSelected) Color(0xFFE8883A) else scheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}


@Composable
fun ClockCard(label: String, timer: ChessTimer, modifier: Modifier = Modifier, isWhite: Boolean) {
    val durationSeconds = timer.remainingTimeMs / 1000
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeFormatted = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val containerColor = if (timer.isRunning) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                timeFormatted,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (timer.remainingTimeMs < 10000 && timer.isRunning) Color.Red else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}



@Composable
fun SocialPanel(
    friends: List<ChessFriend>,
    onAddFriend: () -> Unit,
    onPlayerClick: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // .padding(16.dp)
            ,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Players & Social", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
            IconButton(onClick = onAddFriend) {
                Icon(Icons.Filled.PersonAdd, null, tint = scheme.primary)
            }
        }

        // Search Bar (Screenshot 1)
      OutlinedTextField(
    value = searchQuery,
    onValueChange = { searchQuery = it },
    placeholder = { Text("Search players, bots...", color = scheme.onSurfaceVariant) },
    leadingIcon = { Icon(Icons.Filled.Search, null, tint = scheme.onSurfaceVariant) },
    shape = RoundedCornerShape(24.dp),
    colors = OutlinedTextFieldDefaults.colors(
        // Uses the soft surface containers (latte/dark latte)
        focusedContainerColor = scheme.surfaceVariant,
        unfocusedContainerColor = scheme.surfaceVariant,
        
        // Keeps borders transparent as requested, but links them to theme colors if needed
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        
        // Dynamically adjusts typing text color based on background
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface
    ),
    modifier = Modifier.fillMaxWidth()
)

        // Friends List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Friends", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.onBackground)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = scheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val filteredFriends = friends.filter { it.user.username.contains(searchQuery, ignoreCase = true) }
                    if (filteredFriends.isEmpty()) {
                        Text(
                            "No friends added yet. Enter a username above to search and connect.",
                            fontSize = 13.sp,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        filteredFriends.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayerClick(friend.user.username) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(if (friend.isOnline) Color(0xFF4CAF50) else scheme.onSurfaceVariant, CircleShape)
                                    )
                                    Text(friend.user.username, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, fontSize = 15.sp)
                                }
                                Text(
                                    text = "Rating: ${friend.user.blitzRating?.let { if (it > 0) it else "—" } ?: "—"}", 
                                    fontSize = 12.sp, 
                                    color = scheme.onSurfaceVariant
                                )                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardPanel(
    leaderboard: List<LichessLeaderEntry>,
    onPlayerClick: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    if (leaderboard.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Loading Lichess leaderboard…", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lichess Grandmaster Leaderboard", style = MaterialTheme.typography.titleSmall, color = scheme.primary, fontWeight = FontWeight.Bold)
            leaderboard.forEachIndexed { idx, entry ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth().clickable { onPlayerClick(entry.username) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("#${idx + 1}", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 14.sp)
                            Text(entry.username, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            entry.title?.let { t ->
                                Surface(color = scheme.primaryContainer, shape = RoundedCornerShape(4.dp)) {
                                    Text(t, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                                }
                            }
                        }
                        Text("Rating: ${entry.rating}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

fun generateChess960Fen(): String {
    val squares = Array<String?>(8) { null }
    val random = kotlin.random.Random.Default
    val b1 = random.nextInt(4) * 2
    val b2 = random.nextInt(4) * 2 + 1
    squares[b1] = "B"
    squares[b2] = "B"
    val remaining1 = (0..7).filter { squares[it] == null }
    squares[remaining1[random.nextInt(remaining1.size)]] = "Q"
    val remaining2 = (0..7).filter { squares[it] == null }
    squares[remaining2[random.nextInt(remaining2.size)]] = "N"
    val remaining3 = (0..7).filter { squares[it] == null }
    squares[remaining3[random.nextInt(remaining3.size)]] = "N"
    val remainingLast = (0..7).filter { squares[it] == null }
    squares[remainingLast[0]] = "R"
    squares[remainingLast[1]] = "K"
    squares[remainingLast[2]] = "R"
    val backRank = squares.filterNotNull().joinToString("")
    val whiteRank = backRank.uppercase()
    val blackRank = backRank.lowercase()
    return "$blackRank/pppppppp/8/8/8/8/PPPPPPPP/$whiteRank w KQkq - 0 1"
}

fun applyPieceEdit(fen: String, square: String, piece: String?, isWhite: Boolean): String {
    val col = square[0] - 'a'
    val row = 8 - (square[1] - '0')
    if (col !in 0..7 || row !in 0..7) return fen
    val parts = fen.split(" ")
    val boardPart = parts[0]
    val rows = boardPart.split("/").toMutableList()
    val rowStr = rows[row]
    val expanded = StringBuilder()
    for (char in rowStr) {
        if (char.isDigit()) {
            repeat(char - '0') { expanded.append('.') }
        } else {
            expanded.append(char)
        }
    }
    val pChar = if (piece == null || piece == "trash") {
        '.'
    } else {
        if (isWhite) piece.uppercase()[0] else piece.lowercase()[0]
    }
    if (col < expanded.length) {
        expanded.set(col, pChar)
    }
    val compressed = StringBuilder()
    var emptyCount = 0
    for (char in expanded.toString()) {
        if (char == '.') {
            emptyCount++
        } else {
            if (emptyCount > 0) {
                compressed.append(emptyCount)
                emptyCount = 0
            }
            compressed.append(char)
        }
    }
    if (emptyCount > 0) {
        compressed.append(emptyCount)
    }
    rows[row] = compressed.toString()
    val newBoardPart = rows.joinToString("/")
    return (listOf(newBoardPart) + parts.drop(1)).joinToString(" ")
}
@Composable
fun ReusableChessControlBar(
    moveHistory: List<String>,
    moveAnalysis: List<MoveAnalysis> = emptyList(),
    currentMoveIndex: Int = moveHistory.size,
    onSelectMove: (Int) -> Unit = {},
    onFirstMove: () -> Unit = {},
    onPrevMove: () -> Unit = {},
    onNextMove: () -> Unit = {},
    onLastMove: () -> Unit = {},
    onFlipBoard: () -> Unit = {},
    onResetBoard: () -> Unit = {},
    hasRedo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val canGoBack = currentMoveIndex >= 0
    val canGoForward = (currentMoveIndex < moveHistory.size - 1) || hasRedo
    val scheme = MaterialTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (moveHistory.isNotEmpty()) {
            InteractiveMoveHistoryRow(
                moveHistory = moveHistory,
                moveAnalysis = moveAnalysis,
                currentMoveIndex = currentMoveIndex,
                onSelectMove = onSelectMove
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activeColor = scheme.onSurface
            val disabledColor = scheme.onSurfaceVariant.copy(alpha = 0.38f)

            // First Move Button - Fixed with standard Icon mapping
            IconButton(onClick = onFirstMove, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious, 
                    contentDescription = "First Move",
                    tint = if (canGoBack) activeColor else disabledColor
                )
            }
            // Previous Move Button - Fixed with standard Icon mapping
            IconButton(onClick = onPrevMove, enabled = canGoBack) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft, 
                    contentDescription = "Previous Move",
                    tint = if (canGoBack) activeColor else disabledColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Next Move Button - Fixed with standard Icon mapping
            IconButton(onClick = onNextMove, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight, 
                    contentDescription = "Next Move",
                    tint = if (canGoForward) activeColor else disabledColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Last Move Button - Fixed with standard Icon mapping
            IconButton(onClick = onLastMove, enabled = canGoForward) {
                Icon(
                    imageVector = Icons.Default.SkipNext, 
                    contentDescription = "Last Move",
                    tint = if (canGoForward) activeColor else disabledColor
                )
            }

            VerticalDivider(
                modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), 
                color = scheme.outline
            )

            IconButton(onClick = onFlipBoard) {
                Icon(Icons.Filled.Refresh, "Flip", tint = activeColor, modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onResetBoard) {
                Icon(Icons.Filled.RotateLeft, "Reset", tint = activeColor, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessPgnFenImportDialog(
    onDismiss: () -> Unit,
    onImportToTraining: (pgnOrFenText: String) -> Unit,
    onImportToAnalysis: (pgnOrFenText: String) -> Unit,
    onFilePickerRequested: () -> Unit,
    onImagePickerRequested: () -> Unit = {},
    onCameraPickerRequested: () -> Unit = {}
) {
    val sampleOperaGame = "[Event \"Opera Game\"]\n[White \"Paul Morphy\"]\n[Black \"Duke of Brunswick\"]\n[Result \"1-0\"]\n1. e4 e5 2. Nf3 d6 3. d4 Bg4 4. dxe5 Bxf3 5. Qxf3 dxe5 6. Bc4 Nf6 7. Qb3 Qe7 8. Nc3 c6 9. Bg5 b5 10. Nxb5 cxb5 11. Bxb5+ Nbd7 12. O-O-O Rd8 13. Rxd7 Rxd7 14. Rd1 Qe6 15. Bxd7+ Nxd7 16. Qb8+ Nxb8 17. Rd8# 1-0"
    val sampleImmortalGame = "[Event \"Immortal Game\"]\n[White \"Adolf Anderssen\"]\n[Black \"Lionel Kieseritzky\"]\n[Result \"1-0\"]\n1. e4 e5 2. f4 exf4 3. Bc4 Qh4+ 4. Kf1 b5 5. Bxb5 Nf6 6. Nf3 Qh6 7. d3 Nh5 8. Nh4 Qg5 9. Nf5 c6 10. g4 Nf6 11. Rg1 cxb5 12. h4 Qg6 13. h5 Qg5 14. Qf3 Ng8 15. Bxf4 Qf6 16. Nc3 Bc5 17. Nd5 Qxb2 18. Bd6 Bxg1 19. e5 Qxa1+ 20. Ke2 Na6 21. Nxg7+ Kd8 22. Qf6+ Nxf6 23. Be7# 1-0"
    val sampleEvergreenGame = "[Event \"Evergreen Game\"]\n[White \"Adolf Anderssen\"]\n[Black \"Jean Dufresne\"]\n[Result \"1-0\"]\n1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 4. b4 Bxb4 5. c3 Ba5 6. d4 exd4 7. O-O d3 8. Qb3 Qf6 9. e5 Qg6 10. Re1 Nge7 11. Ba3 b5 12. Qxb5 Rb8 13. Qa4 Bb6 14. Nbd2 Bb7 15. Ne4 Qf5 16. Bxd3 Qh5 17. Nf6+ gxf6 18. exf6 Rg8 19. Rad1 Qxf3 20. Rxe7+ Nxe7 21. Qxd7+ Kxd7 22. Bf5+ Ke8 23. Bd7+ Kf8 24. Bxe7# 1-0"

    var inputPgnText by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = scheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = scheme.primary)
                    Text(
                        text = "PGN & Board AI Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onCameraPickerRequested()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Camera", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onImagePickerRequested()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Photo / FEN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        onFilePickerRequested()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.secondaryContainer,
                        contentColor = scheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import PGN / FEN File", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))

                Text("Sample Famous Master Games:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Opera Game" to sampleOperaGame,
                        "Immortal Game" to sampleImmortalGame,
                        "Evergreen Game" to sampleEvergreenGame
                    ).forEach { (label, pgn) ->
                        FilterChip(
                            selected = inputPgnText == pgn,
                            onClick = { inputPgnText = pgn },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = inputPgnText,
                    onValueChange = { inputPgnText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    placeholder = {
                        Text(
                            "Paste FEN string or game moves PGN...",
                            color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = scheme.primary,
                        unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.4f)
                    )
                )

                var sideToMove by remember { mutableStateOf<String?>(null) } // null = use original in FEN/PGN, "w" = White, "b" = Black

                // Turn to play selection for loaded FEN / custom position
                if (inputPgnText.contains("/") || inputPgnText.isBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Active Turn to Move:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { sideToMove = if (sideToMove == "w") null else "w" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (sideToMove == "w") scheme.primary.copy(alpha = 0.25f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (sideToMove == "w") scheme.primary else scheme.outlineVariant)
                            ) {
                                Text("⚪ White to Move", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            }

                            OutlinedButton(
                                onClick = { sideToMove = if (sideToMove == "b") null else "b" },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (sideToMove == "b") scheme.primary.copy(alpha = 0.25f) else Color.Transparent
                                ),
                                border = BorderStroke(1.dp, if (sideToMove == "b") scheme.primary else scheme.outlineVariant)
                            ) {
                                Text("⚫ Black to Move", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            }
                        }
                    }
                }

                fun processOutputFen(rawText: String): String {
                    val trimmed = rawText.trim()
                    if (sideToMove == null || !trimmed.contains("/")) return trimmed
                    val parts = trimmed.split("\\s+".toRegex()).toMutableList()
                    if (parts.size >= 2) {
                        parts[1] = sideToMove!!
                        return parts.joinToString(" ")
                    } else if (parts.size == 1) {
                        return "${parts[0]} ${sideToMove!!} - - 0 1"
                    }
                    return trimmed
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = scheme.onSurface, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (inputPgnText.isNotBlank()) {
                                onImportToTraining(processOutputFen(inputPgnText))
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.secondary)
                    ) {
                        Text("Training Mode", color = scheme.onSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (inputPgnText.isNotBlank()) {
                                onImportToAnalysis(processOutputFen(inputPgnText))
                            }
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                    ) {
                        Text("Deep Analysis", color = scheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}