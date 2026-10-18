package com.creativeblisslabs.omnimind.feature.chess

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.media.rememberMediaActions
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.chess.shared.*
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.window.Dialog
import com.creativeblisslabs.omnimind.core.chess.GameMode
import kotlin.math.roundToInt

import com.creativeblisslabs.omnimind.core.ui.components.GradientBackground
@Composable
expect fun getPiecePainter(piece: Char, style: String = "cburnett"): Painter?

@Composable
fun internalPiecePainterStub(piece: Char, style: String = "cburnett"): Painter? {
    return getPiecePainter(piece, style)
}
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChessScreen(navController: NavController, invite: String? = null) {
        // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)

    val vm: ChessViewModel = koinViewModel()

    LaunchedEffect(invite) {
        invite?.let {
            vm.joinFriendChallenge(it)
        }
    }

    val activeTimeFormat by vm.activeTimeFormat.collectAsStateWithLifecycle()
    val userProfile by vm.currentUserRating.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()

    // Engine & Game session
    val engineSettings by vm.engineSettings.collectAsStateWithLifecycle()

  var showFriendDialog by remember { mutableStateOf(false) }
    var friendInput by remember { mutableStateOf("") }
    val scheme = MaterialTheme.colorScheme

    val mediaActions = rememberMediaActions(
        onImagePicked = {},
        onFilePicked = { attachment ->
            vm.importPgnFile(attachment)
        },
        onAudioPicked = {},
        onClipboardImage = {}
    )
    val isGameActive by vm.isGameActive.collectAsStateWithLifecycle()
    val isWaitingForFriend by vm.isWaitingForFriend.collectAsStateWithLifecycle()
    val sharePayload by vm.sharePayload.collectAsStateWithLifecycle()
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var showWaitingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sharePayload) {
        sharePayload?.let { text ->
            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
            vm.consumeSharePayload()
        }
    }
  
    var activeTab by remember { mutableStateOf("Game") } // Game, Puzzles, Lessons, Social, Tournaments
    var showTimeControlDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf(10f) }
    var customIncrement by remember { mutableStateOf(0f) }
    var showArenaFaqSheet by remember { mutableStateOf(false) }
    var showBoardSettings by remember { mutableStateOf(false) }
    var magnifyDraggedPiece by remember { mutableStateOf(true) }
    var draggedPieceTargetCircle by remember { mutableStateOf(true) }
    var enablePieceAnimation by remember { mutableStateOf(true) }
    var enableImmersiveMode by remember { mutableStateOf(false) }
    var showPieceDestinations by remember { mutableStateOf(true) }
    var showBoardHighlights by remember { mutableStateOf(true) }
    var showMoveListWhilePlaying by remember { mutableStateOf(true) }
    var showMaterialDifference by remember { mutableStateOf(true) }
    var clockPositionRight by remember { mutableStateOf(true) }
    var enablePremoves by remember { mutableStateOf(true) }
    var confirmResignation by remember { mutableStateOf(true) }
    var enableTouchFeedback by remember { mutableStateOf(true) }
    var selectedProfileUser by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPuzzleThemesDialog by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }
    var showOpeningExplorerSheet by remember { mutableStateOf(false) }
    var showLoadPositionScreen by remember { mutableStateOf(false) }
    var fenInputText by remember { mutableStateOf("") }
    var pgnInputText by remember { mutableStateOf("") }
    var p1TimeLeft by remember { mutableStateOf(600) }
    var p2TimeLeft by remember { mutableStateOf(600) }
    var activeClockPlayer by remember { mutableStateOf(0) } // 0=stopped, 1=p1, 2=p2
    var isClockSoundMuted by remember { mutableStateOf(false) }
    var showClockSettingsSheet by remember { mutableStateOf(false) }
    var clockSettingsMinutes by remember { mutableStateOf(10f) }
    var clockSettingsIncrement by remember { mutableStateOf(0f) }
    var showEngineSettingsDialog by remember { mutableStateOf(false) }
    // Selected game mode for lobby


    LaunchedEffect(activeClockPlayer) {
        if (activeClockPlayer != 0) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (activeClockPlayer == 1) {
                    if (p1TimeLeft > 0) p1TimeLeft--
                } else if (activeClockPlayer == 2) {
                    if (p2TimeLeft > 0) p2TimeLeft--
                }
            }
        }
    }
    data class ChessTheme(
        val name: String,
        val lightColor: Long,
        val darkColor: Long,
        val is3D: Boolean = false
    )
    GradientBackground(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight

            Row(modifier = Modifier.fillMaxSize()) {
                if (isLandscape && activeTab != "Clock") {
                    NavigationRail(
                        containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        contentColor = scheme.onSurface
                    ) {
                        listOf(
                            "Game" to Icons.Filled.PlayArrow,
                            "Puzzles" to Icons.Filled.Extension,
                            "Lessons" to Icons.Filled.School,
                            "Social" to Icons.Filled.People,
                            "Clock" to Icons.Filled.Timer,
                            "Settings" to Icons.Filled.Settings
                        ).forEach { (tab, icon) ->
                            val active = activeTab == tab
                            NavigationRailItem(
                                selected = active,
                                onClick = {
                                    activeTab = tab
                                    when (tab) {
                                        "Puzzles" -> vm.setGameMode(GameMode.PUZZLE)
                                        "Lessons" -> vm.setGameMode(GameMode.THEORY)
                                        "Game" -> vm.setGameMode(GameMode.VS_BOT)
                                    }
                                },
                                icon = { Icon(icon, contentDescription = tab) },
                                label = { Text(tab, fontSize = 10.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = scheme.primary,
                                    unselectedIconColor = scheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Rating Overview Single-Row Header (Hidden on Clock screen)
                    if (activeTab != "Clock") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                val isProfileConnected = userProfile.username.isNotBlank()
                                listOf(
                                    Triple("Bullet", if (isProfileConnected) (userProfile.bulletRating?.toString() ?: "—") else "—", Icons.Filled.Bolt),
                                    Triple("Blitz", if (isProfileConnected) (userProfile.blitzRating?.toString() ?: "—") else "—", Icons.Filled.Speed),
                                    Triple("Rapid", if (isProfileConnected) (userProfile.rapidRating?.toString() ?: "—") else "—", Icons.Filled.HourglassTop),
                                    Triple("Classical", if (isProfileConnected) (userProfile.classicalRating?.toString() ?: "—") else "—", Icons.Filled.Business),
                                    Triple("Puzzles", if (isProfileConnected) (userProfile.puzzleRating?.toString() ?: "—") else "—", Icons.Filled.Extension)
                                ).forEach { (label, rating, icon) ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = scheme.onBackground.copy(alpha = 0.9f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = rating,
                                            color = scheme.onBackground,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedProfileUser =
                                            if (userProfile.username.isNotBlank()) userProfile.username else "Chess User"
                                        showProfileDialog = true
                                    }
                            ) {
                                Text(
                                    text = if (userProfile.username.isNotBlank()) userProfile.username else "Connect Profile",
                                    color = scheme.onBackground,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = scheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Tab Content Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (activeTab == "Clock") {
                            var isClockFlipped by remember { mutableStateOf(false) }
                            ChessClockPanel(
                                p1TimeLeft = if (!isClockFlipped) p1TimeLeft else p2TimeLeft,
                                p2TimeLeft = if (!isClockFlipped) p2TimeLeft else p1TimeLeft,
                                activeClockPlayer = activeClockPlayer,
                                isClockSoundMuted = isClockSoundMuted,
                                onPlayer1Tap = {
                                    activeClockPlayer = if (!isClockFlipped) {
                                        if (activeClockPlayer == 1 || activeClockPlayer == 0) 2 else 1
                                    } else {
                                        if (activeClockPlayer == 2 || activeClockPlayer == 0) 1 else 2
                                    }
                                },
                                onPlayer2Tap = {
                                    activeClockPlayer = if (!isClockFlipped) {
                                        if (activeClockPlayer == 2 || activeClockPlayer == 0) 1 else 2
                                    } else {
                                        if (activeClockPlayer == 1 || activeClockPlayer == 0) 2 else 1
                                    }
                                },
                                onPlayPause = {
                                    activeClockPlayer = if (activeClockPlayer != 0) 0 else 1
                                },
                                onReset = {
                                    activeClockPlayer = 0
                                    p1TimeLeft = (clockSettingsMinutes * 60).roundToInt()
                                    p2TimeLeft = (clockSettingsMinutes * 60).roundToInt()
                                },
                                onSettings = {
                                    showClockSettingsSheet = true
                                },
                                onMuteToggle = {
                                    isClockSoundMuted = !isClockSoundMuted
                                },
                                onFlip = {
                                    isClockFlipped = !isClockFlipped
                                }
                            )
                        } else if (activeTab == "Lessons") {
                            LessonsTabContent(
                                vm = vm,
                                onSwitchToTab = { tab -> activeTab = tab },
                                onPuzzleThemeSelected = { themeName ->
                                    vm.loadPuzzleByTheme(themeName)
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when (activeTab) {
                                    "Game" -> {
                                        GameTabContent(
                                            vm = vm,
                                            mediaActions = mediaActions,
                                            onWaitOpponent = { showWaitingDialog = true },
                                            onShowThemeDialog = { }
                                        )
                                    }

                                    "Puzzles" -> {
                                        PuzzlesTabContent(vm = vm)
                                    }

                                    "Social" -> {
                                        SocialTabContent(
                                            vm = vm,
                                            onAddFriend = { showFriendDialog = true },
                                            onPlayerClick = {
                                                selectedProfileUser = it
                                                showProfileDialog = true
                                            }
                                        )
                                    }

                                    "Tournaments" -> {
                                        TournamentsTabContent(
                                            vm = vm,
                                            onFaqClick = { showArenaFaqSheet = true }
                                        )
                                    }

                                    "Settings" -> {
                                        SettingsTabContent(vm = vm)
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Mode Navigation Bar (Anchored at bottom for portrait)
                    if (!isLandscape && activeTab != "Clock") {
                        Surface(
                            color = scheme.surfaceContainerHigh,
                            tonalElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf(
                                    "Game" to Icons.Filled.PlayArrow,
                                    "Puzzles" to Icons.Filled.Extension,
                                    "Lessons" to Icons.Filled.School,
                                    "Social" to Icons.Filled.People,
                                    "Clock" to Icons.Filled.Timer,
                                    "Settings" to Icons.Filled.Settings
                                ).forEach { (tab, icon) ->
                                    val active = activeTab == tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) scheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                            .clickable {
                                                activeTab = tab
                                                when (tab) {
                                                    "Puzzles" -> vm.setGameMode(GameMode.PUZZLE)
                                                    "Lessons" -> vm.setGameMode(GameMode.THEORY)
                                                    "Game" -> vm.setGameMode(GameMode.VS_BOT)
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                icon,
                                                null,
                                                tint = if (active) scheme.primary else scheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                text = tab,
                                                color = if (active) scheme.primary else scheme.onSurfaceVariant,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
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

    // --- 1. Player Profile Dialog with Lichess & Chess.com Dual-Tab View ---
    if (showProfileDialog) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        var selectedProfilePlatform by remember { mutableStateOf("Lichess") }
        val userProfile by vm.userProfile.collectAsStateWithLifecycle()
        val chessComProfile by vm.chessComProfile.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            val username = userProfile.username.ifBlank { selectedProfileUser.ifBlank { "magnuson-carlsium" } }
            vm.fetchDualPlatformProfile(username, "Both")
        }

        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showProfileDialog = false },
            sheetState = sheetState,
containerColor = scheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x33FFFFFF)) }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isLandscape = maxWidth > maxHeight
                val activeUser = if (selectedProfilePlatform == "Lichess") userProfile else chessComProfile

                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: User Header & Platform Switcher & Quick Actions
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .background(scheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (activeUser.username.ifBlank { "magnuson-carlsium" }).take(2).uppercase(),
                                        color = scheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        activeUser.title?.let { t ->
                                            if (t.isNotBlank()) {
                                                Text(t, color = Color(0xFFF2C94C), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.background(Color(0x33F2C94C), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text(
                                            text = activeUser.username.ifBlank { "magnuson-carlsium" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = scheme.onSurface
                                        )
                                    }
                                    Text(
                                        "Connected via $selectedProfilePlatform API",
                                        color = scheme.secondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Platform Switcher Tabs (Lichess vs. Chess.com)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(scheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("Lichess", "Chess.com").forEach { platform ->
                                    val isActive = selectedProfilePlatform == platform
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isActive) scheme.primary else Color.Transparent)
                                            .clickable { selectedProfilePlatform = platform }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(platform, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isActive) scheme.onPrimary else scheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Overview Performance Card
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Profile Stats", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Games:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.totalGames}", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Wins / Losses / Draws:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.wins} W / ${activeUser.losses} L / ${activeUser.draws} D", fontSize = 12.sp, color = scheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                    if (activeUser.playTimeHours > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Play Time:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                            Text("${activeUser.playTimeHours} hrs", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Location / Country:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text(activeUser.country.ifBlank { "INDIA" }, fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val name = activeUser.username.ifBlank { "magnuson-carlsium" }
                                    val url = if (selectedProfilePlatform == "Lichess") "https://lichess.org/@/$name" else "https://www.chess.com/member/$name"
                                    uriHandler.openUri(url)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                            ) {
                                Icon(Icons.Filled.OpenInNew, null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open $selectedProfilePlatform Web Profile", color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Right Column: Ratings Grid & Game Stats
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Mode Ratings ($selectedProfilePlatform)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.onSurface)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val bullet = activeUser.bulletRating?.toString() ?: "N/A"
                                val blitz = activeUser.blitzRating?.toString() ?: "N/A"
                                val rapid = activeUser.rapidRating?.toString() ?: "N/A"
                                val classical = activeUser.classicalRating?.toString() ?: "N/A"
                                val puzzles = activeUser.puzzleRating?.toString() ?: "N/A"

                                listOf(
                                    Triple("Bullet", bullet, Icons.Filled.Bolt),
                                    Triple("Blitz", blitz, Icons.Filled.Speed),
                                    Triple("Rapid", rapid, Icons.Filled.HourglassTop),
                                    Triple("Classical", classical, Icons.Filled.Business),
                                    Triple("Puzzles", puzzles, Icons.Filled.Extension)
                                ).forEach { (mode, rating, icon) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(icon, mode, tint = scheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(mode, fontSize = 10.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                                            Text(rating, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            // Activity & Match Breakdown
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Match Breakdown ($selectedProfilePlatform)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Blitz Matches:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.blitzGames} games", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Bullet Matches:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.bulletGames} games", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Rapid Matches:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.rapidGames} games", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait Layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(scheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (activeUser.username.ifBlank { "magnuson-carlsium" }).take(2).uppercase(),
                                    color = scheme.onPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    activeUser.title?.let { t ->
                                        if (t.isNotBlank()) {
                                            Text(t, color = Color(0xFFF2C94C), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.background(Color(0x33F2C94C), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(
                                        text = activeUser.username.ifBlank { "magnuson-carlsium" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface
                                    )
                                }
                                Text("Active Profile ($selectedProfilePlatform)", color = scheme.secondary, fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(scheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("Lichess", "Chess.com").forEach { platform ->
                                val isActive = selectedProfilePlatform == platform
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isActive) scheme.primary else Color.Transparent)
                                        .clickable { selectedProfilePlatform = platform }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(platform, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isActive) scheme.onPrimary else scheme.onSurfaceVariant)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val bullet = activeUser.bulletRating?.toString() ?: "N/A"
                            val blitz = activeUser.blitzRating?.toString() ?: "N/A"
                            val rapid = activeUser.rapidRating?.toString() ?: "N/A"
                            val classical = activeUser.classicalRating?.toString() ?: "N/A"
                            val puzzles = activeUser.puzzleRating?.toString() ?: "N/A"

                            listOf(
                                Triple("Bullet", bullet, Icons.Filled.Bolt),
                                Triple("Blitz", blitz, Icons.Filled.Speed),
                                Triple("Rapid", rapid, Icons.Filled.HourglassTop),
                                Triple("Classical", classical, Icons.Filled.Business),
                                Triple("Puzzles", puzzles, Icons.Filled.Extension)
                            ).forEach { (mode, rating, icon) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(icon, mode, tint = scheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(mode, fontSize = 10.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                                        Text(rating, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val name = activeUser.username.ifBlank { "magnuson-carlsium" }
                                val url = if (selectedProfilePlatform == "Lichess") "https://lichess.org/@/$name" else "https://www.chess.com/member/$name"
                                uriHandler.openUri(url)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                        ) {
                            Icon(Icons.Filled.OpenInNew, null, tint = scheme.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Web Profile on $selectedProfilePlatform", color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (isGameActive || isWaitingForFriend || showWaitingDialog) {
        val currentSession = com.creativeblisslabs.omnimind.chess.shared.GameSession(
            timeFormat = activeTimeFormat,
            mode = vm.gameMode.collectAsStateWithLifecycle().value,
            variant = com.creativeblisslabs.omnimind.core.chess.GameVariant.CLASSIC,
            playerIsWhite = vm.isPlayerWhite.collectAsStateWithLifecycle().value,
            label = if (vm.gameMode.collectAsStateWithLifecycle().value == GameMode.VS_FRIEND) "Friend Match" else "Casual"
        )
        ChessGameFullScreen(
            vm = vm,
            session = currentSession,
            onExit = {
                vm.resetGame()
                showWaitingDialog = false
            }
        )
    }
}