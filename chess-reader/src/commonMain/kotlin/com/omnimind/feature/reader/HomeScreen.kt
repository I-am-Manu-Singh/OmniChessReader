package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.navigation.Screen
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import com.creativeblisslabs.omnimind.core.media.rememberMediaActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

val AmberWarm = Color(0xFFE8883A)

@Composable
fun HomeScreen(navController: NavController) {
    val prefs = koinInject<AppPreferences>()
    val userName by prefs.userName.collectAsState(initial = "Grandmaster")
    val books = LibraryStore.books
    val readerVm: ReaderHubViewModel = koinViewModel()
    val scope = rememberCoroutineScope()

    // Last Game / Analysis Position from preferences
    val lastGameFen by prefs.lastChessGameFen.collectAsState(initial = "")
    val lastGameTitle by prefs.lastChessGameTitle.collectAsState(initial = "Stockfish Bot Match")
    val lastGamePgn by prefs.lastChessGamePgn.collectAsState(initial = "")
    val lastGameMode by prefs.lastChessGameMode.collectAsState(initial = "VS_BOT")

    val mediaActions = rememberMediaActions(
        onImagePicked = {},
        onFilePicked = { attachment ->
            readerVm.importBook(
                attachment = attachment,
                onPasswordRequired = { _, _ -> }
            ) { importedBook, chunks ->
                ReaderSessionStore.loadBook(importedBook, chunks)
                navController.navigate(Screen.BookReader.route)
            }
        },
        onAudioPicked = {},
        onClipboardImage = {}
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 36.dp)
    ) {
        // ─── Header ──────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (userName.isNotBlank()) "Welcome back, $userName" else "Welcome to OmniChess",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Master chess theory & literature seamlessly",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, AmberWarm.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = AmberWarm,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AmberWarm.copy(alpha = 0.15f))
                            .border(1.dp, AmberWarm.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = "App Icon",
                            tint = AmberWarm,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ─── Resume Last Chess Game / Analysis Card ───────────────────────────
        item {
            val displayFen = if (lastGameFen.isNotBlank()) lastGameFen else com.creativeblisslabs.omnimind.core.chess.ChessLogic.getInitialFen()
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { navController.navigate(Screen.Chess.route) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AmberWarm.copy(alpha = 0.35f))
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(AmberWarm.copy(alpha = 0.12f), MaterialTheme.colorScheme.surfaceVariant)
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini Interactive Chess Board Preview
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, AmberWarm.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .background(Color(0xFF2B2118))
                        ) {
                            MiniChessBoardPreview(
                                fen = displayFen,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Info & Continue Action
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AmberWarm)
                                )
                                Text(
                                    text = if (lastGameFen.isNotBlank()) "ACTIVE GAME RESUME" else "QUICK MATCH READY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarm,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Text(
                                text = lastGameTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = if (lastGameFen.isNotBlank()) "Tap to continue your session" else "Play Stockfish AI or study positions",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { navController.navigate(Screen.Chess.route) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AmberWarm,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Continue",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.Chess.route) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFF4A4A58)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Analytics,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF81C784)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Analyze",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ─── Primary Action Cards: Play Arena & Book Library ───────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Chess Hub Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { navController.navigate(Screen.Chess.route) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AmberWarm.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(AmberWarm.copy(alpha = 0.1f), Color.Transparent)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(AmberWarm.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SportsEsports, null, tint = AmberWarm, modifier = Modifier.size(22.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Chess Arena",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.25.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Bots, Puzzles, Online & Analysis",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Book Reader Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { navController.navigate(Screen.ReaderHub.route) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFF64B5F6).copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF64B5F6).copy(alpha = 0.1f), Color.Transparent)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF64B5F6).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Color(0xFF64B5F6), modifier = Modifier.size(22.dp))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "Chess Books",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.25.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${books.size} Books in Library",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── Recent / Continue Reading Carousel ───────────────────────────────
        if (books.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Continue Reading",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "View All",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AmberWarm,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { navController.navigate(Screen.ReaderHub.route) }
                            .padding(4.dp)
                    )
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(books.take(5)) { book ->
                        Surface(
                            modifier = Modifier
                                .width(135.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    scope.launch {
                                        val chunkEntities = withContext(Dispatchers.IO) {
                                            readerVm.bookChunkDao.getChunksListForBook(book.id)
                                        }
                                        val chunkTexts = chunkEntities.map { it.text }
                                        if (chunkTexts.isNotEmpty()) {
                                            ReaderSessionStore.loadBook(book, chunkTexts)
                                        }
                                        readerVm.loadBookChunks(book.id)
                                        navController.navigate(Screen.BookReader.route)
                                    }
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF282830)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (book.coverImagePath.isNotBlank()) {
                                        coil3.compose.AsyncImage(
                                            model = book.coverImagePath,
                                            contentDescription = book.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = book.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${book.progress}% read",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { book.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(CircleShape),
                                    color = AmberWarm,
                                    trackColor = AmberWarm.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─── Quick Feature Hub ────────────────────────────────────────────────
        item {
            Text(
                text = "Interactive Chess Tools & Training",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickFeatureRow(
                    title = "Analysis Board & Engine Evaluation",
                    subtitle = "Evaluate positions with Stockfish engine and depth metrics",
                    icon = Icons.Default.Analytics,
                    iconTint = Color(0xFF81C784),
                    onClick = { 
                        // navController.navigate(Screen.Chess.route) 
                    } 
                )

                QuickFeatureRow(
                    title = "Tactics & Puzzle Rush",
                    subtitle = "Sharpen tactical pattern recognition under time pressure",
                    icon = Icons.Default.FlashOn,
                    iconTint = AmberWarm,
                    onClick = { 
                        // navController.navigate(Screen.Chess.route) 
                    }
                )

                QuickFeatureRow(
                    title = "Interactive Lessons & Endgames",
                    subtitle = "Study master strategies, king & pawn endgames, and theory",
                    icon = Icons.Default.School,
                    iconTint = Color(0xFFBA68C8),
                    onClick = { 
                        // navController.navigate(Screen.Chess.route) 
                    }
                )

                QuickFeatureRow(
                    title = "Fast Import EPUB / PDF / MOBI",
                    subtitle = "Import literature directly from your device storage",
                    icon = Icons.Default.AddCircleOutline,
                    iconTint = Color(0xFF4FC3F7),
                    onClick = { 
                        mediaActions.pickFile() 
                    }
                )

                QuickFeatureRow(
                    title = "Reading Typography & Font Settings",
                    subtitle = "Configure font sizes, line spacing, margins & themes",
                    icon = Icons.Default.Tune,
                    iconTint = Color(0xFFFFB74D),
                    onClick = { 
                        // navController.navigate(Screen.ReaderFont.route) 
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickFeatureRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f).copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun MiniChessBoardPreview(
    fen: String,
    modifier: Modifier = Modifier
) {
    val state = remember(fen) {
        com.creativeblisslabs.omnimind.core.chess.ChessLogic.parseFen(fen)
    }

    val lightColor = Color(0xFFD6B896)
    val darkColor = Color(0xFF8B5A2B)

    Column(modifier = modifier) {
        for (r in 0 until 8) {
            Row(modifier = Modifier.weight(1f)) {
                for (c in 0 until 8) {
                    val isLight = (r + c) % 2 == 0
                    val fileChar = ('a' + c)
                    val rankChar = ('1' + (7 - r))
                    val sq = "$fileChar$rankChar"
                    val piece = state.pieces[sq]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isLight) lightColor else darkColor),
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null) {
                            Text(
                                text = pieceToUnicode(piece),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (piece.isUpperCase()) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pieceToUnicode(p: Char): String = when (p) {
    'P' -> "♙"
    'N' -> "♘"
    'B' -> "♗"
    'R' -> "♖"
    'Q' -> "♕"
    'K' -> "♔"
    'p' -> "♟"
    'n' -> "♞"
    'b' -> "♝"
    'r' -> "♜"
    'q' -> "♛"
    'k' -> "♚"
    else -> ""
}
