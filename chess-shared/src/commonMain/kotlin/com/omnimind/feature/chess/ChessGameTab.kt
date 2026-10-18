package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.core.ui.components.ProductHeader
import com.creativeblisslabs.omnimind.core.ui.components.StatusPill
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.core.chess.GameMode
import com.creativeblisslabs.omnimind.core.chess.GameVariant
import com.creativeblisslabs.omnimind.chess.shared.GameSession
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow

import com.creativeblisslabs.omnimind.core.media.MediaActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameTabContent(
    vm: ChessViewModel,
    mediaActions: MediaActions,
    onWaitOpponent: () -> Unit = {},
    onShowThemeDialog: () -> Unit = {}
) {
    // Reset board when arriving in Game tab if not in active game
    LaunchedEffect(Unit) {
        val isGameActive = vm.isGameActive.value
        if (!isGameActive) {
            vm.resetGame()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
    var customMinutes by remember { mutableStateOf("5") }
    var customIncrement by remember { mutableStateOf("3") }
    var lobbyMode by remember { mutableStateOf(GameMode.VS_BOT) }
    val globalBotLevel by vm.botLevel.collectAsStateWithLifecycle()
    var lobbyBotLevel by remember(globalBotLevel) { mutableStateOf(globalBotLevel) }
    var lobbyPlayerWhite by remember { mutableStateOf(true) }
    var lobbyTimeFormat by remember { mutableStateOf("5+3") }
    var lobbyIsRated by remember { mutableStateOf(false) }
    var lobbyVariant by remember { mutableStateOf(GameVariant.CLASSIC) }
    var showAdvancedEngine by remember { mutableStateOf(false) }
    
    var isBoardEditorMode by remember { mutableStateOf(false) }
    var selectedEditorPiece by remember { mutableStateOf<String?>("P") }
    var selectedEditorPieceColorWhite by remember { mutableStateOf(true) }
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var showLoadPositionScreen by remember { mutableStateOf(false) }
    var showOpeningExplorerSheet by remember { mutableStateOf(false) }
    var showTimeControlDialog by remember { mutableStateOf(false) }
    var showFriendChallengeModal by remember { mutableStateOf(false) }
    var showJoinFriendModal by remember { mutableStateOf(false) }
    var joinCodeInput by remember { mutableStateOf("") }

    var editorActiveTab by remember { mutableStateOf(0) }
    var activePieceStyle by remember { mutableStateOf("cburnett") }

    val currentFen by vm.fen.collectAsStateWithLifecycle()
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val premove by vm.premove.collectAsStateWithLifecycle()
    val showThreatMap by vm.showThreatMap.collectAsStateWithLifecycle()
    val threatMap by vm.threatMap.collectAsStateWithLifecycle()
    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()

    val activeGameSession by vm.activeGameSession.collectAsStateWithLifecycle()
    val analysisResult by vm.gameAnalysisResult.collectAsStateWithLifecycle()
    val analysisProgress by vm.analysisProgress.collectAsStateWithLifecycle()
    val moveHistory by vm.moveHistory.collectAsStateWithLifecycle()
    val engineSettings by vm.engineSettings.collectAsStateWithLifecycle()
    val downloadProgress by vm.engineDownloadProgress.collectAsStateWithLifecycle()
    val currentUserRating by vm.currentUserRating.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    val isCustomSelected = lobbyTimeFormat.startsWith("custom_")
    val displayCustomFormat = if (isCustomSelected) {
        lobbyTimeFormat.removePrefix("custom_").replace("_", "+")
    } else {
        "Custom"
    }

    // Rich modern professional styling framework using surface gradients & depth cards
    // Rich modern professional styling framework using surface gradients & depth cards
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Game Lobby Controls & Setup
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Opponent Mode (VS Bot, VS Friend, Online, Training) & Slider
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Opponent Mode",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("VS Bot", Icons.Filled.Android, GameMode.VS_BOT),
                                    Triple("VS Friend", Icons.Filled.People, GameMode.VS_FRIEND),
                                    Triple("Online", Icons.Filled.Public, GameMode.ONLINE),
                                    Triple("Training", Icons.Filled.Psychology, GameMode.TRAINING)
                                ).forEach { (label, icon, mode) ->
                                    val isSelected = lobbyMode == mode
                                    val cardBg = if (isSelected) {
                                        Brush.linearGradient(listOf(scheme.primary, scheme.primaryContainer))
                                    } else {
                                        Brush.linearGradient(listOf(scheme.surfaceContainerHighest, scheme.surfaceContainerHighest))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(cardBg)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) scheme.primary.copy(alpha = 0.8f) else scheme.outlineVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { lobbyMode = mode },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(icon, null, tint = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) scheme.onPrimary else scheme.onSurface)
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = lobbyMode == GameMode.VS_BOT || lobbyMode == GameMode.TRAINING,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Engine Skill Rating", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                        Text("Level $lobbyBotLevel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = scheme.primary)
                                    }
                                    Slider(
                                        value = lobbyBotLevel.toFloat(),
                                        onValueChange = {
                                            val lvl = it.roundToInt().coerceIn(1, 20)
                                            lobbyBotLevel = lvl
                                            vm.setBotLevel(lvl)
                                        },
                                        valueRange = 1f..20f,
                                        steps = 19,
                                        colors = SliderDefaults.colors(thumbColor = scheme.primary, activeTrackColor = scheme.primary)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Game Variant (Classic, Chess960, King of Hill, Three-Check, Atomic, Crazyhouse)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Game Variant", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GameVariant.entries.forEach { v ->
                                    val isSelected = lobbyVariant == v
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) scheme.secondaryContainer else scheme.surfaceContainerHighest)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) scheme.secondary else scheme.outlineVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { lobbyVariant = v },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(v.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = if (isSelected) scheme.onSecondaryContainer else scheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Side Selection & Rated Match Controls
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Side Selection & Match Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.primary)

                            AnimatedVisibility(
                                visible = lobbyMode != GameMode.ONLINE,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(true to "White", false to "Black").forEach { (white, label) ->
                                        val sel = lobbyPlayerWhite == white
                                        OutlinedButton(
                                            onClick = { lobbyPlayerWhite = white },
                                            modifier = Modifier.weight(1f).height(40.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (sel) scheme.primary else scheme.surfaceContainerHighest
                                            ),
                                            border = BorderStroke(if (sel) 0.dp else 1.dp, if (sel) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text(label, color = if (sel) scheme.onPrimary else scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { lobbyPlayerWhite = (0..1).random() == 0 },
                                        modifier = Modifier.weight(0.5f).height(40.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = scheme.surfaceContainerHighest),
                                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("?", color = scheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = lobbyMode == GameMode.ONLINE,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(scheme.surfaceContainerHigh).border(1.dp, scheme.outlineVariant.copy(alpha = 0.3f)).padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(false to "Casual", true to "Rated").forEach { (isRated, label) ->
                                        val selected = lobbyIsRated == isRated
                                        Box(
                                            modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(8.dp)).background(if (selected) scheme.primary else scheme.surfaceContainerHighest).clickable { lobbyIsRated = isRated },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Column: Time Controls, Action Buttons, Friend Room & Training Suite
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Time Control Grid
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Time Control", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
                            val timeControls = listOf(
                                "1+0" to "Bullet", "2+1" to "Bullet", "3+0" to "Blitz", "3+2" to "Blitz",
                                "5+0" to "Blitz", "5+3" to "Blitz", "10+0" to "Rapid", "10+5" to "Rapid",
                                "15+10" to "Rapid", "30+0" to "Classical", "30+20" to "Classical", "Custom" to "Setup"
                            )
                            val rows = timeControls.chunked(6)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                rows.forEach { rowItems ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                        rowItems.forEach { pair ->
                                            val format = pair.first
                                            val isCustomTile = format == "Custom"
                                            val sel = if (isCustomTile) isCustomSelected else lobbyTimeFormat == format
                                            val cardTitle = if (isCustomTile && isCustomSelected) displayCustomFormat else format

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (sel) scheme.primary else scheme.surfaceContainerHighest)
                                                    .border(width = if (sel) 1.5.dp else 1.dp, color = if (sel) scheme.primary else scheme.outlineVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (isCustomTile) showTimeControlDialog = true else lobbyTimeFormat = format
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(cardTitle, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (sel) scheme.onPrimary else scheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Play with a Friend (P2P) Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Group, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Play with a Friend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { showFriendChallengeModal = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)) {
                                    Icon(Icons.Filled.Share, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Invite Link", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { showJoinFriendModal = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.Login, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Join Game", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 3. Start Match Primary Button
                    Button(
                        onClick = {
                            val session = com.creativeblisslabs.omnimind.chess.shared.GameSession(
                                timeFormat = lobbyTimeFormat, mode = lobbyMode, variant = lobbyVariant,
                                playerIsWhite = lobbyPlayerWhite, botLevel = lobbyBotLevel, label = if (lobbyIsRated) "Rated" else "Casual", isRated = lobbyIsRated
                            )
                            vm.startGame(session)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp), spotColor = scheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Start Match", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = scheme.onPrimary)
                    }

                    // 4. Training Suite Card
                    AnimatedVisibility(
                        visible = lobbyMode == GameMode.TRAINING,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Analysis & Training Suite", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.secondary)
                                Button(
                                    onClick = { showLoadPositionScreen = true },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = scheme.secondaryContainer)
                                ) {
                                    Icon(Icons.Filled.ContentPaste, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Load Position / FEN / PGN", fontSize = 12.sp, color = scheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Section Card Container: Opponent Mode ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opponent Mode",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("VS Bot", Icons.Filled.Android, GameMode.VS_BOT),
                                Triple("VS Friend", Icons.Filled.People, GameMode.VS_FRIEND),
                                Triple("Online", Icons.Filled.Public, GameMode.ONLINE),
                                Triple("Training", Icons.Filled.Psychology, GameMode.TRAINING)
                            ).forEach { (label, icon, mode) ->
                                val isSelected = lobbyMode == mode
                                val cardBg = if (isSelected) {
                                    Brush.linearGradient(listOf(scheme.primary, scheme.primaryContainer))
                                } else {
                                    Brush.linearGradient(listOf(scheme.surfaceContainerHighest, scheme.surfaceContainerHighest))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(cardBg)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) scheme.primary.copy(alpha = 0.8f) else scheme.outlineVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { lobbyMode = mode },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) scheme.onPrimary else scheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Dynamic Bot level slider if VS Bot is active
                        AnimatedVisibility(
                            visible = lobbyMode == GameMode.VS_BOT || lobbyMode == GameMode.TRAINING,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Engine Skill Rating", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                    Text("Level $lobbyBotLevel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = scheme.primary)
                                }
                                Slider(
                                    value = lobbyBotLevel.toFloat(),
                                    onValueChange = { lobbyBotLevel = it.roundToInt().coerceIn(1, 20) },
                                    valueRange = 1f..20f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = scheme.primary,
                                        activeTrackColor = scheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── Section Card Container: Game Variant ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Game Variant",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GameVariant.entries.forEach { v ->
                                val isSelected = lobbyVariant == v
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) scheme.secondaryContainer else scheme.surfaceContainerHighest)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) scheme.secondary else scheme.outlineVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { lobbyVariant = v },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = v.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) scheme.onSecondaryContainer else scheme.onSurface
                                        )
                                        Text(
                                            text = v.name.lowercase().take(8),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = if (isSelected) scheme.onSecondaryContainer.copy(alpha = 0.7f) else scheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Section Card Container: Color & Rated Controls ──
          item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp) // Slightly increased gap for row separation
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Side Selection & Match Type",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.primary
                                )
                            }

                            // 1. SIDE SELECTION ROW (Only shown if offline/local)
                            AnimatedVisibility(
                                visible = lobbyMode != GameMode.ONLINE,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(true to "White", false to "Black").forEach { (white, label) ->
                                        val sel = lobbyPlayerWhite == white
                                        OutlinedButton(
                                            onClick = { lobbyPlayerWhite = white },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (sel) scheme.primary else scheme.surfaceContainerHighest
                                            ),
                                            border = BorderStroke(
                                                width = if (sel) 0.dp else 1.dp,
                                                color = if (sel) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (sel) scheme.onPrimary else scheme.onSurface,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    // Random side button ("?")
                                    OutlinedButton(
                                        onClick = { lobbyPlayerWhite = (0..1).random() == 0 },
                                        modifier = Modifier
                                            .weight(0.5f)
                                            .height(44.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = scheme.surfaceContainerHighest),
                                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("?", color = scheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 2. MATCH TYPE ROW (Stays isolated in the Column hierarchy)
                            AnimatedVisibility(
                                visible = lobbyMode == GameMode.ONLINE,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(scheme.surfaceContainerHigh)
                                        .border(1.dp, scheme.outlineVariant.copy(alpha = 0.3f))
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Row(
                                        modifier = Modifier.weight(2f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf(false to "Casual", true to "Rated").forEach { (isRated, label) ->
                                            val selected = lobbyIsRated == isRated
                                            val backgroundColor =
                                                if (selected) scheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent
                                            val textColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    // 1. Dynamic Background Controller
                                                    .background(if (selected) scheme.primary else scheme.surfaceContainerHighest)
                                                    // 2. Dynamic Border Controller (Moved from button arguments)
                                                    .border(
                                                        width = if (selected) 0.dp else 1.dp,
                                                        color = if (selected) Color.Transparent else scheme.outlineVariant.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    // 3. User Event Handler
                                                    .clickable { lobbyIsRated = isRated },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    // Text switches color based on active choice states
                                                    color = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            
            // ── Section Card Container: Play with a Friend Card (Game Settings) ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.8f)),
                    border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Group, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Play with a Friend (P2P Zero-Lag)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onSurface
                            )
                        }
                        Text(
                            text = "Create a custom invite link or join a friend's room. No database needed!",
                            fontSize = 12.sp,
                            color = scheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showFriendChallengeModal = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Create Invite Link", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showJoinFriendModal = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Login, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Join Game", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Section Card Container: Time Control Grid ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Time Control",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary
                            )
                        }

                        val timeControls = listOf(
                            "1+0" to "Bullet",
                            "2+1" to "Bullet",
                            "3+0" to "Blitz",
                            "3+2" to "Blitz",
                            "5+0" to "Blitz",
                            "5+3" to "Blitz",
                            "10+0" to "Rapid",
                            "10+5" to "Rapid",
                            "15+10" to "Rapid",
                            "30+0" to "Classical",
                            "30+20" to "Classical",
                            "Custom" to "Setup"
                        )

                        val chunkSize = if (isLandscape) 12 else 6
                        val rows = timeControls.chunked(chunkSize)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rows.forEach { rowItems ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowItems.forEach { pair ->
                                        val format = pair.first
                                        val type = pair.second

                                        val isCustomTile = format == "Custom"
                                        val sel = if (isCustomTile) isCustomSelected else lobbyTimeFormat == format

                                        val cardTitle = if (isCustomTile && isCustomSelected) displayCustomFormat else format
                                        val cardSubtitle = if (isCustomTile && isCustomSelected) "Custom" else type

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.15f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (sel) {
                                                        Brush.linearGradient(listOf(scheme.primary, scheme.primaryContainer))
                                                    } else {
                                                        Brush.linearGradient(listOf(scheme.surfaceContainerHighest, scheme.surfaceContainerHighest))
                                                    }
                                                )
                                                .border(
                                                    width = if (sel) 1.5.dp else 1.dp,
                                                    color = if (sel) scheme.primary else scheme.outlineVariant.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    if (isCustomTile) {
                                                        showTimeControlDialog = true
                                                    } else {
                                                        lobbyTimeFormat = format
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(2.dp)
                                            ) {
                                                Text(
                                                    text = cardTitle,
                                                    color = if (sel) scheme.onPrimary else scheme.onSurface,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = cardSubtitle,
                                                    color = if (sel) scheme.onPrimary.copy(alpha = 0.8f) else scheme.onSurfaceVariant,
                                                    fontSize = 10.sp
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

            // ── Primary Action Buttons Stack ──
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val session = com.creativeblisslabs.omnimind.chess.shared.GameSession(
                                timeFormat = lobbyTimeFormat,
                                mode = lobbyMode,
                                variant = lobbyVariant,
                                playerIsWhite = lobbyPlayerWhite,
                                botLevel = lobbyBotLevel,
                                label = if (lobbyIsRated) "Rated" else "Casual",
                                isRated = lobbyIsRated
                            )
                            vm.resetGame()
                            vm.startGame(session)

                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = scheme.primary),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = scheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Start Match",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = scheme.onPrimary
                        )
                    }
                }
            }

            // ── Integrated Analysis & Training Mode Launcher ──
            item {
                AnimatedVisibility(
                    visible = lobbyMode == GameMode.TRAINING,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Analysis & Training Suite",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = scheme.secondary
                                )
                                StatusPill(text = "Training Ready", color = scheme.secondary)
                            }
                            Text(
                                text = "Analyze positions, test custom lines, and review game accuracy in full-screen mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showLoadPositionScreen = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = scheme.secondaryContainer)
                                ) {
                                    Icon(Icons.Filled.ContentPaste, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Load FEN", fontSize = 12.sp, color = scheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                }
                            }


                            AnimatedVisibility(visible = analysisResult != null) {
                                analysisResult?.let { PostGameAnalysisPanel(it, moveHistory) }
                            }

                            if (analysisProgress > 0f) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { analysisProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = scheme.primary,
                                        trackColor = scheme.surfaceContainerHighest
                                    )
                                    Text(
                                        text = "Analyzing game... ${(analysisProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.End)
                                    )
                                }
                            }

                             Button(
                        onClick = {
                            val session = GameSession(
                                timeFormat = lobbyTimeFormat,
                                mode = GameMode.TRAINING,
                                variant = lobbyVariant,
                                playerIsWhite = lobbyPlayerWhite,
                                botLevel = lobbyBotLevel,
                                label = "Analysis",
                                isRated = false
                            )
                            vm.startGame(session)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = scheme.surfaceContainerHighest,
                            contentColor = scheme.onSurface
                        ),
                        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = null,
                            tint = scheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Open Analysis Sandbox",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
        }
    }

    // ── Full-screen Game Overlay Session ──
    activeGameSession?.let { session ->
        ChessGameFullScreen(
            vm = vm,
            session = session,
            onExit = { vm.exitGameSession() }
        )
    }

    // ── Custom Time Control Dialog Setup ──
    if (showTimeControlDialog) {
        AlertDialog(
            onDismissRequest = { showTimeControlDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = scheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Custom Time Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Specify game duration and per-move increment in seconds:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes per side") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = scheme.primary,
                            unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customIncrement,
                        onValueChange = { customIncrement = it.filter { char -> char.isDigit() } },
                        label = { Text("Increment (Seconds)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = scheme.primary,
                            unfocusedBorderColor = scheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customMinutes.toIntOrNull() ?: 5
                        val inc = customIncrement.toIntOrNull() ?: 0
                        lobbyTimeFormat = "custom_${mins}_$inc"
                        showTimeControlDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                ) {
                    Text("Apply Time", color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimeControlDialog = false }
                ) {
                    Text("Cancel", color = scheme.onSurfaceVariant)
                }
            }
        )
    }

    // ── Load FEN / PGN Unified Training Suite Dialog ──
    if (showLoadPositionScreen) {
        ChessPgnFenImportDialog(
            onDismiss = { showLoadPositionScreen = false },
            onImportToTraining = { pgnOrFen ->
                val session = GameSession(
                    mode = GameMode.TRAINING,
                    label = "Custom Position",
                    isRated = false
                )
                vm.startGame(session)
                if (pgnOrFen.contains("/")) {
                    vm.updateFen(pgnOrFen)
                } else {
                    vm.loadPgn(pgnOrFen)
                }
            },
            onImportToAnalysis = { pgnOrFen ->
                if (pgnOrFen.contains("/")) {
                    vm.updateFen(pgnOrFen)
                } else {
                    vm.loadPgn(pgnOrFen)
                }
                vm.runGameAnalysis()
            },
            onFilePickerRequested = {
                mediaActions.pickFile()
            },
            onImagePickerRequested = {
                mediaActions.pickImage()
            },
            onCameraPickerRequested = {
                mediaActions.pickImage()
            }
        )
    }

    // ── Create Friend Challenge Modal ──
    if (showFriendChallengeModal) {
        val inviteInfo by vm.friendInviteInfo.collectAsStateWithLifecycle()
        var selectedMinutes by remember { mutableStateOf(5) }
        var selectedColor by remember { mutableStateOf("white") }
        var copiedNotice by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showFriendChallengeModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Group, null, tint = scheme.primary)
                    Text("Challenge a Friend", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Time Control:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "1 min", 3 to "3 min", 5 to "5 min", 10 to "10 min").forEach { (min, label) ->
                            FilterChip(
                                selected = selectedMinutes == min,
                                onClick = { selectedMinutes = min },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text("Play As:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("white" to "♔ White", "black" to "♚ Black", "random" to "🎲 Random").forEach { (colorKey, label) ->
                            FilterChip(
                                selected = selectedColor == colorKey,
                                onClick = { selectedColor = colorKey },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (inviteInfo != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = scheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Room Code",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = scheme.onSurfaceVariant
                                )
                                // ── Big prominently displayed short code ──
                                Text(
                                    text = inviteInfo!!.shortCode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 32.sp,
                                    color = scheme.primary,
                                    letterSpacing = 6.sp
                                )
                                Text(
                                    "Share this code with your friend",
                                    fontSize = 11.sp,
                                    color = scheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { vm.shareInviteLink() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                                    ) {
                                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Share Full Link", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteInfo == null) {
                            val timeFormat = "${selectedMinutes}+0"
                            vm.createFriendChallenge(
                                timeFormat = timeFormat,
                                variant = lobbyVariant.name,
                                isRated = lobbyIsRated,
                                colorChoice = selectedColor
                            )
                        }
                        showFriendChallengeModal = false
                        onWaitOpponent()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                ) {
                    Text(if (inviteInfo == null) "Create & Wait" else "Done", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFriendChallengeModal = false }) { Text("Close", color = scheme.onSurfaceVariant) }
            }
        )
    }

    // ── Join Friend Challenge Modal ──
    if (showJoinFriendModal) {
        AlertDialog(
            onDismissRequest = { showJoinFriendModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Login, null, tint = scheme.primary)
                    Text("Join Friend's Game", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Enter the 8-character room code:",
                        fontSize = 12.sp,
                        color = scheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it.uppercase().take(12) },
                        placeholder = { Text("e.g.  A1B2C3D4", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                    )
                    Text(
                        "Or paste the full invite link below:",
                        fontSize = 11.sp,
                        color = scheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = if (joinCodeInput.startsWith("http") || joinCodeInput.startsWith("omnimind")) joinCodeInput else "",
                        onValueChange = { joinCodeInput = it },
                        placeholder = { Text("https://omnimind.app/chess/invite?...", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.primary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (joinCodeInput.isNotBlank()) {
                            vm.joinFriendChallenge(joinCodeInput)
                            showJoinFriendModal = false
                            onWaitOpponent()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                ) { Text("Join & Play", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showJoinFriendModal = false }) { Text("Cancel", color = scheme.onSurfaceVariant) }
            }
        )
    }
}
}

