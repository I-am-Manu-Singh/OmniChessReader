package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelType
import com.creativeblisslabs.omnimind.core.data.ai.ModelPackInfo
import com.creativeblisslabs.omnimind.core.ui.theme.*
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile
import com.creativeblisslabs.omnimind.core.chess.EngineVersion
import com.creativeblisslabs.omnimind.core.ui.rememberFilePicker
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.chess.shared.ChessTheme
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
fun SettingsTabContent(
    vm: ChessViewModel
) {
    val scheme = MaterialTheme.colorScheme

    val appPreferences = koinInject<com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences>()
    val scope = rememberCoroutineScope()

    val magnifyDraggedPiece by appPreferences.chessMagnifyPiece.collectAsStateWithLifecycle(true)
    val draggedPieceTargetCircle by appPreferences.chessTargetCircle.collectAsStateWithLifecycle(true)
    val enablePieceAnimation by appPreferences.chessPieceAnimation.collectAsStateWithLifecycle(true)
    val enableImmersiveMode by appPreferences.chessImmersiveMode.collectAsStateWithLifecycle(false)
    val showPieceDestinations by appPreferences.chessShowDestinations.collectAsStateWithLifecycle(true)
    val showBoardHighlights by appPreferences.chessShowHighlights.collectAsStateWithLifecycle(true)
    val showMoveListWhilePlaying by appPreferences.chessShowMoveList.collectAsStateWithLifecycle(true)
    val showMaterialDifference by appPreferences.chessShowMaterialDiff.collectAsStateWithLifecycle(true)
    val clockPositionRight by appPreferences.chessClockRight.collectAsStateWithLifecycle(true)
    val enablePremoves by appPreferences.chessPremoves.collectAsStateWithLifecycle(true)
    val confirmResignation by appPreferences.chessConfirmResign.collectAsStateWithLifecycle(true)
    val enableTouchFeedback by appPreferences.chessTouchFeedback.collectAsStateWithLifecycle(true)

    var editorActiveTab by remember { mutableStateOf(0) }
    var selectedEditorPiece by remember { mutableStateOf<String?>("P") }
    var selectedEditorPieceColorWhite by remember { mutableStateOf(true) }
    val activePieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    var lobbyBotLevel by remember { mutableStateOf(10) }
    val botLevel by vm.botLevel.collectAsStateWithLifecycle()
    val gameMode by vm.gameMode.collectAsStateWithLifecycle()
    var showAdvancedEngine by remember { mutableStateOf(false) }

    val currentFen by vm.fen.collectAsStateWithLifecycle()
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()
    val engineSettings by vm.engineSettings.collectAsStateWithLifecycle()

    val enginePacks by vm.enginePacks.collectAsStateWithLifecycle()
    val allModelPacks by vm.allModelPacks.collectAsStateWithLifecycle()
    val isCheckingForUpdates by vm.isCheckingForUpdates.collectAsStateWithLifecycle()
    val updateMessage by vm.updateMessage.collectAsStateWithLifecycle()

    var activeImportProfile by remember { mutableStateOf<LocalModelProfile?>(null) }
    val filePicker = rememberFilePicker { uri ->
        activeImportProfile?.let { profile ->
            vm.importLocalModel(uri, profile)
        }
    }

    // Board settings preview ready
    LaunchedEffect(Unit) {
        // No heavy reset on tab open
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val userProfile by vm.currentUserRating.collectAsStateWithLifecycle()
                var inputUsername by remember { mutableStateOf(userProfile.username) }
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                // Left Column: Profile Sync, Display & Game Behaviour Settings
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profile Sync
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = inputUsername,
                                onValueChange = { inputUsername = it },
                                label = { Text("Username", color = scheme.onSurfaceVariant) },
                                placeholder = { Text("Connect Lichess / Chess.com Username", color = scheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = scheme.onSurface,
                                    unfocusedTextColor = scheme.onSurface,
                                    focusedBorderColor = scheme.primary,
                                    unfocusedBorderColor = scheme.outlineVariant
                                ),
                                singleLine = true
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { if (inputUsername.isNotBlank()) vm.fetchLichessProfile(inputUsername.trim()) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                                ) {
                                    Text("Fetch Ratings", color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val u = if (userProfile.username.isNotBlank()) userProfile.username else inputUsername
                                        if (u.isNotBlank()) uriHandler.openUri("https://lichess.org/@/$u")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("View Profile", color = scheme.onSurface)
                                }
                            }
                        }
                    }
                    // 2. Display Settings Card
                    Text("Display", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.primary)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Magnify dragged piece", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = magnifyDraggedPiece, onCheckedChange = { scope.launch { appPreferences.setChessMagnifyPiece(it) } })
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Piece animation", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = enablePieceAnimation, onCheckedChange = { scope.launch { appPreferences.setChessPieceAnimation(it) } })
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Immersive mode", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = enableImmersiveMode, onCheckedChange = { scope.launch { appPreferences.setChessImmersiveMode(it) } })
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Piece destinations", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = showPieceDestinations, onCheckedChange = { scope.launch { appPreferences.setChessShowDestinations(it) } })
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Board highlights", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = showBoardHighlights, onCheckedChange = { scope.launch { appPreferences.setChessShowHighlights(it) } })
                            }
                        }
                    }

                    // 3. Game Behaviour Settings Card
                    Text("Game Behaviour", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.primary)
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Premoves", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = enablePremoves, onCheckedChange = { scope.launch { appPreferences.setChessPremoves(it) } })
                            }
                            HorizontalDivider(color = Color(0x1AFFFFFF))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Confirm resignation & draw", color = scheme.onSurface, fontSize = 14.sp)
                                Switch(checked = confirmResignation, onCheckedChange = { scope.launch { appPreferences.setChessConfirmResign(it) } })
                            }
                        }
                    }
                }

                // Right Column: Board Editor & Bot Engine Config
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Board Editor & Customization", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.primary)

                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(
                                    modifier = Modifier.width(56.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)).padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    val tabs = listOf(
                                        Triple(0, Icons.Filled.Extension, "Board Pieces Editor"),
                                        Triple(1, Icons.Filled.GridView, "Board"),
                                        Triple(2, Icons.Filled.FontDownload, "Pieces Style")
                                    )
                                    tabs.forEach { (index, icon, label) ->
                                        val active = editorActiveTab == index
                                        IconButton(onClick = { editorActiveTab = index }, modifier = Modifier.size(40.dp).background(if (active) scheme.primary else Color.Transparent, RoundedCornerShape(8.dp))) {
                                            Icon(icon, contentDescription = label, tint = if (active) scheme.onPrimary else Color.Gray)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ChessBoardContainer(
                                        fen = currentFen,
                                        theme = theme,
                                        pieceStyle = pieceStyle,
                                        isPlayerWhite = isPlayerWhite,
                                        selectedSquare = null,
                                        premove = null,
                                        showThreatMap = false,
                                        threatMap = emptyMap(),
                                        visualArrows = visualArrows,
                                        engineEvaluation = evaluation,
                                        moveHistory = emptyList(),
                                        onSquareClick = { clicked ->
                                            if (editorActiveTab == 0) {
                                                val editedFen = applyPieceEdit(fen = currentFen, square = clicked, piece = selectedEditorPiece, isWhite = selectedEditorPieceColorWhite)
                                                vm.updateFen(editedFen)
                                            }
                                        }
                                    )

                                    when (editorActiveTab) {
                                        0 -> {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().background(Color(0xFF121212)),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("Black Pieces", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF141416), RoundedCornerShape(8.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                                    val handActive = selectedEditorPiece == null
                                                    IconButton(onClick = { selectedEditorPiece = null }, modifier = Modifier.background(if (handActive) scheme.primary else Color.Transparent, RoundedCornerShape(6.dp))) {
                                                        Icon(Icons.Filled.Edit, "Select", tint = scheme.onSurface)
                                                    }
                                                    listOf("p", "n", "b", "r", "q", "k").forEach { p ->
                                                        val active = selectedEditorPiece == p && !selectedEditorPieceColorWhite
                                                        IconButton(
                                                            onClick = { selectedEditorPiece = p; selectedEditorPieceColorWhite = false },
                                                            modifier = Modifier.background(if (active) Color(0xFFE8883A) else Color.Transparent, RoundedCornerShape(6.dp))
                                                        ) {
                                                            val painter = internalPiecePainter(p[0], activePieceStyle)
                                                            if (painter != null) {
                                                                androidx.compose.foundation.Image(painter, null, modifier = Modifier.size(22.dp))
                                                            } else {
                                                                Text(p.uppercase(), color = scheme.onSurface, fontSize = 12.sp)
                                                            }
                                                        }
                                                    }
                                                    val trashActive = selectedEditorPiece == "trash"
                                                    IconButton(onClick = { selectedEditorPiece = "trash" }, modifier = Modifier.background(if (trashActive) Color(0xFFEF5350) else Color.Transparent, RoundedCornerShape(6.dp))) {
                                                        Icon(Icons.Filled.Close, "Erase", tint = scheme.onSurface)
                                                    }
                                                }

                                                Text("White Pieces", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF141416), RoundedCornerShape(8.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                                    listOf("P", "N", "B", "R", "Q", "K").forEach { p ->
                                                        val active = selectedEditorPiece == p && selectedEditorPieceColorWhite
                                                        IconButton(
                                                            onClick = { selectedEditorPiece = p; selectedEditorPieceColorWhite = true },
                                                            modifier = Modifier.background(if (active) Color(0xFFE8883A) else Color.Transparent, RoundedCornerShape(6.dp))
                                                        ) {
                                                            val painter = internalPiecePainter(p[0], activePieceStyle)
                                                            if (painter != null) {
                                                                androidx.compose.foundation.Image(painter, null, modifier = Modifier.size(22.dp))
                                                            } else {
                                                                Text(p, color = scheme.onSurface, fontSize = 12.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        1 -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Board Presets", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                val boardThemes = listOf(
                                                    ChessTheme("chesscom green", 0xFFEEEED2, 0xFF769656, false),
                                                    ChessTheme("lichess brown", 0xFFF0D9B5, 0xFFB58863, false),
                                                    ChessTheme("lichess blue", 0xFF9CB2D8, 0xFF5879A8, false),
                                                    ChessTheme("onyx", 0xFF3E3E3E, 0xFF1E1E1E, false),
                                                    ChessTheme("silver", 0xFFE0E0E0, 0xFF707070, false),
                                                    ChessTheme("cyan", 0xFFBCE3E8, 0xFF358593, false),
                                                    ChessTheme("magnus", 0xFFF0D9B5, 0xFFB58863, false),
                                                    ChessTheme("newspaper light", 0xFFF0F0F0, 0xFFCCCCCC, false),
                                                    ChessTheme("newspaper dark", 0xFF424242, 0xFF212121, false),
                                                    ChessTheme("terracotta", 0xFFE89A78, 0xFFBA5536, false),
                                                    ChessTheme("vinyl", 0xFF526A8F, 0xFF2C3E55, false),
                                                    ChessTheme("purple", 0xFF9D75C2, 0xFF5B3482, false)
                                                )
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(boardThemes) { item ->
                                                        val isSel = theme.name.lowercase() == item.name.lowercase()
                                                        val thumbPainter = rememberBoardThumbnailPainter(item.name)
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier.width(64.dp).clickable { vm.selectTheme(item) }
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).border(if (isSel) 2.dp else 1.dp, if (isSel) scheme.primary else Color(0x33FFFFFF), RoundedCornerShape(8.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (thumbPainter != null) {
                                                                    Image(painter = thumbPainter, contentDescription = item.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                                }
                                                            }
                                                            Spacer(Modifier.height(2.dp))
                                                            Text(text = item.name.replaceFirstChar { it.uppercase() }, color = if (isSel) scheme.primary else Color.Gray, fontSize = 9.sp, maxLines = 1, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        2 -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Chess Piece", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                val activeStyle by vm.pieceStyle.collectAsStateWithLifecycle()
                                                val fontOptions = listOf(
                                                    Pair("alpha", "Alpha"), Pair("cardinal", "Cardinal"), Pair("cburnett", "CBurnett (Official)"), Pair("celtic", "Celtic"), Pair("companion", "Companion"), Pair("cooke", "Cooke"), Pair("dubrovny", "Dubrovny"), Pair("fresca", "Fresca"), Pair("gioco", "Gioco"), Pair("governor", "Governor"), Pair("icpieces", "ICPieces"), Pair("kosal", "Kosal"), Pair("maestro", "Maestro"), Pair("merida", "Merida (Classic)"), Pair("pirouetti", "Pirouetti"), Pair("reillycraig", "Reillycraig"), Pair("riohacha", "Riohacha"), Pair("staunty", "Staunty"), Pair("tatiana", "Tatiana")
                                                )
                                                val itemHeight = 44.dp
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxWidth().heightIn(max = itemHeight * 4 + 12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    items(fontOptions) { (fontKey, label) ->
                                                        val isSelected = activeStyle == fontKey
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().height(itemHeight).clip(RoundedCornerShape(6.dp)).background(if (isSelected) scheme.primary.copy(alpha = 0.2f) else Color(0xFF141416)).border(1.dp, if (isSelected) scheme.primary else Color(0x22FFFFFF), RoundedCornerShape(6.dp)).clickable { vm.setPieceStyle(fontKey) }.padding(horizontal = 12.dp, vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(text = label, color = scheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                            RadioButton(selected = isSelected, onClick = { vm.setPieceStyle(fontKey) }, colors = RadioButtonDefaults.colors(selectedColor = scheme.primary))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Action buttons
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { vm.resetGame() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Reset Changes", fontSize = 11.sp) }
                                        Button(onClick = { vm.updateFen(currentFen) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) { Text("Save", fontSize = 11.sp) }
                                    }
                                }
                            }
                        }
                    }

                    // Bot & Engine Settings Card (always accessible in settings)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Bot & Engine Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.primary)
                            TextButton(onClick = { showAdvancedEngine = !showAdvancedEngine }) {
                                Text(if (showAdvancedEngine) "Hide Advanced" else "Configure", fontSize = 12.sp, color = scheme.secondary)
                            }
                        }
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Bot Level", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, fontSize = 15.sp)
                                    Text("Level $botLevel", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 15.sp)
                                }
                                Slider(value = botLevel.toFloat(), onValueChange = { vm.setBotLevel(it.roundToInt()) }, valueRange = 1f..20f, steps = 18, colors = SliderDefaults.colors(thumbColor = scheme.primary, activeTrackColor = scheme.primary))

                                Text("Engine Version", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.creativeblisslabs.omnimind.core.chess.EngineVersion.entries.forEach { ver ->
                                        val sel = engineSettings.version == ver
                                        val isDownloaded = enginePacks.find { it.profile.label.contains(ver.label.split(" ")[0]) }?.isDownloaded ?: false

                                        OutlinedButton(
                                            onClick = { vm.updateEngineSettings(engineSettings.copy(version = ver)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (sel) scheme.primary else Color.Transparent,
                                                disabledContainerColor = Color.Transparent
                                            ),
                                            enabled = isDownloaded,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (sel) scheme.primary else if (isDownloaded) Color(0x33FFFFFF) else Color(0x11FFFFFF)
                                            )
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    ver.label,
                                                    color = if (sel) scheme.onPrimary else if (isDownloaded) scheme.onSurface else Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                if (!isDownloaded) {
                                                    Text("Not downloaded", color = scheme.onSurfaceVariant, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showAdvancedEngine) {
                                    Spacer(Modifier.height(2.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Search time", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                                            Text("${engineSettings.searchTimeSec}s", color = scheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = engineSettings.searchTimeSec.toFloat(),
                                            onValueChange = { vm.updateEngineSettings(engineSettings.copy(searchTimeSec = it.roundToInt())) },
                                            valueRange = 1f..20f,
                                            steps = 18
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text("Engine Binaries", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                val displayPacks = enginePacks.ifEmpty {
                                    listOf(
                                        ModelPackInfo(profile = com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile.Stockfish17)
                                    )
                                }
                                displayPacks.forEach { pack ->
                                    ModelPackCard(
                                        pack = pack,
                                        isPrimary = false,
                                        onSelect = {},
                                        onDownload = { vm.downloadPack(pack.profile) },
                                        onDelete = { vm.deletePack(pack.profile) },
                                        onImport = {
                                            activeImportProfile = pack.profile
                                            filePicker()
                                        }
                                    )
                                }

                                val aiAndTranslationPacks = allModelPacks.filter { it.profile.type != LocalModelType.CHESS_ENGINE }
                                if (aiAndTranslationPacks.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("On-Device AI & Translation Models", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.secondary)
                                    aiAndTranslationPacks.forEach { pack ->
                                        ModelPackCard(
                                            pack = pack,
                                            isPrimary = false,
                                            onSelect = {},
                                            onDownload = { vm.downloadPack(pack.profile) },
                                            onDelete = { vm.deletePack(pack.profile) },
                                            onImport = {
                                                activeImportProfile = pack.profile
                                                filePicker()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val userProfile by vm.currentUserRating.collectAsStateWithLifecycle()
                var inputUsername by remember { mutableStateOf(userProfile.username) }
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                // ── Lichess & Chess.com Profile Sync ──
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = inputUsername,
                            onValueChange = { inputUsername = it },
                            label = { Text("Username", color = scheme.onSurfaceVariant) },
                            placeholder = { Text("Connect Lichess / Chess.com Username", color = scheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = scheme.onSurface,
                                unfocusedTextColor = scheme.onSurface,
                                focusedBorderColor = scheme.primary,
                                unfocusedBorderColor = scheme.outlineVariant
                            ),
                            singleLine = true
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { if (inputUsername.isNotBlank()) vm.fetchLichessProfile(inputUsername.trim()) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                            ) {
                                Text("Fetch Ratings", color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    val u = if (userProfile.username.isNotBlank()) userProfile.username else inputUsername
                                    if (u.isNotBlank()) uriHandler.openUri("https://lichess.org/@/$u")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View Profile", color = scheme.onSurface)
                            }
                        }
                    }
                }

                // ── Board Editor Section ──
                Text(
                    text = "Board Editor",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = scheme.primary
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left vertical tabs rail expanded with Lichess Board Themes & Piece Styles
                            Column(
                                modifier = Modifier
                                    .width(56.dp)
                                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val tabs = listOf(
                                    Triple(0, Icons.Filled.Extension, "Board Pieces Editor"),
                                    Triple(1, Icons.Filled.GridView, "Board"),
                                    Triple(2, Icons.Filled.FontDownload, "Pieces Style")
                                )
                                tabs.forEach { (index, icon, label) ->
                                    val active = editorActiveTab == index
                                    IconButton(
                                        onClick = { editorActiveTab = index },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(if (active) scheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(icon, contentDescription = label, tint = if (active) scheme.onPrimary else Color.Gray)
                                    }
                                }
                            }

                            // Center & Right pane
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ChessBoardContainer(
                                    fen = currentFen,
                                    theme = theme,
                                    pieceStyle = pieceStyle,
                                    isPlayerWhite = isPlayerWhite,
                                    selectedSquare = null,
                                    premove = null,
                                    showThreatMap = false,
                                    threatMap = emptyMap(),
                                    visualArrows = visualArrows,
                                    engineEvaluation = evaluation,
                                    moveHistory = emptyList(),
                                    onSquareClick = { clicked ->
                                        if (editorActiveTab == 0) {
                                            val editedFen = applyPieceEdit(
                                                fen = currentFen,
                                                square = clicked,
                                                piece = selectedEditorPiece,
                                                isWhite = selectedEditorPieceColorWhite
                                            )
                                            vm.updateFen(editedFen)
                                        }
                                    }
                                )

                                when (editorActiveTab) {
                                    0 -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF121212)),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Black Pieces", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF141416), RoundedCornerShape(8.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                                val handActive = selectedEditorPiece == null
                                                IconButton(onClick = { selectedEditorPiece = null }, modifier = Modifier.background(if (handActive) scheme.primary else Color.Transparent, RoundedCornerShape(6.dp))) {
                                                    Icon(Icons.Filled.Edit, "Select", tint = scheme.onSurface)
                                                }
                                                listOf("p", "n", "b", "r", "q", "k").forEach { p ->
                                                    val active = selectedEditorPiece == p && !selectedEditorPieceColorWhite
                                                    IconButton(
                                                        onClick = { selectedEditorPiece = p; selectedEditorPieceColorWhite = false },
                                                        modifier = Modifier.background(if (active) Color(0xFFE8883A) else Color.Transparent, RoundedCornerShape(6.dp))
                                                    ) {
                                                        val painter = internalPiecePainter(p[0], activePieceStyle)
                                                        if (painter != null) {
                                                            androidx.compose.foundation.Image(painter, null, modifier = Modifier.size(22.dp))
                                                        } else {
                                                            Text(p.uppercase(), color = scheme.onSurface, fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                                val trashActive = selectedEditorPiece == "trash"
                                                IconButton(onClick = { selectedEditorPiece = "trash" }, modifier = Modifier.background(if (trashActive) Color(0xFFEF5350) else Color.Transparent, RoundedCornerShape(6.dp))) {
                                                    Icon(Icons.Filled.Close, "Erase", tint = scheme.onSurface)
                                                }
                                            }

                                            Text("White Pieces", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF141416), RoundedCornerShape(8.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                                listOf("P", "N", "B", "R", "Q", "K").forEach { p ->
                                                    val active = selectedEditorPiece == p && selectedEditorPieceColorWhite
                                                    IconButton(
                                                        onClick = { selectedEditorPiece = p; selectedEditorPieceColorWhite = true },
                                                        modifier = Modifier.background(if (active) Color(0xFFE8883A) else Color.Transparent, RoundedCornerShape(6.dp))
                                                    ) {
                                                        val painter = internalPiecePainter(p[0], activePieceStyle)
                                                        if (painter != null) {
                                                            androidx.compose.foundation.Image(painter, null, modifier = Modifier.size(22.dp))
                                                        } else {
                                                            Text(p, color = scheme.onSurface, fontSize = 12.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Board Presets", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                                            val boardThemes = listOf(
                                                ChessTheme("chesscom green", 0xFFEEEED2, 0xFF769656, false),
                                                ChessTheme("lichess brown", 0xFFF0D9B5, 0xFFB58863, false),
                                                ChessTheme("lichess blue", 0xFF9CB2D8, 0xFF5879A8, false),
                                                ChessTheme("onyx", 0xFF3E3E3E, 0xFF1E1E1E, false),
                                                ChessTheme("silver", 0xFFE0E0E0, 0xFF707070, false),
                                                ChessTheme("cyan", 0xFFBCE3E8, 0xFF358593, false),
                                                ChessTheme("magnus", 0xFFF0D9B5, 0xFFB58863, false),
                                                ChessTheme("newspaper light", 0xFFF0F0F0, 0xFFCCCCCC, false),
                                                ChessTheme("newspaper dark", 0xFF424242, 0xFF212121, false),
                                                ChessTheme("terracotta", 0xFFE89A78, 0xFFBA5536, false),
                                                ChessTheme("vinyl", 0xFF526A8F, 0xFF2C3E55, false),
                                                ChessTheme("purple", 0xFF9D75C2, 0xFF5B3482, false)
                                            )

                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(boardThemes) { item ->
                                                    val isSel = theme.name.lowercase() == item.name.lowercase()
                                                    val thumbPainter = rememberBoardThumbnailPainter(item.name)

                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier
                                                            .width(64.dp)
                                                            .clickable { vm.selectTheme(item) }
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .border(
                                                                    width = if (isSel) 2.dp else 1.dp,
                                                                    color = if (isSel) scheme.primary else Color(0x33FFFFFF),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (thumbPainter != null) {
                                                                Image(
                                                                    painter = thumbPainter,
                                                                    contentDescription = item.name,
                                                                    modifier = Modifier.fillMaxSize(),
                                                                    contentScale = ContentScale.Crop
                                                                )
                                                            }
                                                        }
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                            text = item.name.replaceFirstChar { it.uppercase() },
                                                            color = if (isSel) scheme.primary else Color.Gray,
                                                            fontSize = 9.sp,
                                                            maxLines = 1,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Chess Piece", color = scheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            val activeStyle by vm.pieceStyle.collectAsStateWithLifecycle()
                                            val fontOptions = listOf(
                                                Pair("alpha", "Alpha"),
                                                Pair("cardinal", "Cardinal"),
                                                Pair("cburnett", "CBurnett (Official)"),
                                                Pair("celtic", "Celtic"),
                                                Pair("companion", "Companion"),
                                                Pair("cooke", "Cooke"),
                                                Pair("dubrovny", "Dubrovny"),
                                                Pair("fresca", "Fresca"),
                                                Pair("gioco", "Gioco"),
                                                Pair("governor", "Governor"),
                                                Pair("icpieces", "ICPieces"),
                                                Pair("kosal", "Kosal"),
                                                Pair("maestro", "Maestro"),
                                                Pair("merida", "Merida (Classic)"),
                                                Pair("pirouetti", "Pirouetti"),
                                                Pair("reillycraig", "Reillycraig"),
                                                Pair("riohacha", "Riohacha"),
                                                Pair("staunty", "Staunty"),
                                                Pair("tatiana", "Tatiana")
                                            )

                                            val itemHeight = 44.dp

                                            LazyColumn(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = itemHeight * 5 + 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(fontOptions) { (fontKey, label) ->
                                                    val isSelected = activeStyle == fontKey
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(itemHeight)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (isSelected) scheme.primary.copy(alpha = 0.2f) else Color(0xFF141416))
                                                            .border(1.dp, if (isSelected) scheme.primary else Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                                                            .clickable { vm.setPieceStyle(fontKey) }
                                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            color = scheme.onSurface,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 12.sp
                                                        )

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                listOf('Q', 'k').forEach { pieceChar ->
                                                                    val painter = internalPiecePainter(pieceChar, fontKey)
                                                                    if (painter != null) {
                                                                        Image(
                                                                            painter = painter,
                                                                            contentDescription = null,
                                                                            modifier = Modifier.size(26.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            RadioButton(
                                                                selected = isSelected,
                                                                onClick = { vm.setPieceStyle(fontKey) },
                                                                colors = RadioButtonDefaults.colors(selectedColor = scheme.primary),
                                                                modifier = Modifier.padding(start = 8.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Actions
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { vm.resetGame() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                    ) { Text("Reset Changes", fontSize = 11.sp) }
                                    Button(
                                        onClick = { vm.updateFen(currentFen) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                    ) { Text("Save", fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }

                // ── Display Section ──
                Text(
                    text = "Display",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = scheme.primary
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Magnify dragged piece", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = magnifyDraggedPiece, onCheckedChange = { scope.launch { appPreferences.setChessMagnifyPiece(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Dragged piece target", color = scheme.onSurface, fontSize = 15.sp)
                                Text("Circle", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Switch(checked = draggedPieceTargetCircle, onCheckedChange = { scope.launch { appPreferences.setChessTargetCircle(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Piece animation", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = enablePieceAnimation, onCheckedChange = { scope.launch { appPreferences.setChessPieceAnimation(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Immersive mode", color = scheme.onSurface, fontSize = 15.sp)
                                Text(
                                    text = "Hide system UI while playing. Applies to game and puzzle screens.",
                                    color = scheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Switch(checked = enableImmersiveMode, onCheckedChange = { scope.launch { appPreferences.setChessImmersiveMode(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Piece destinations (valid moves and premoves)", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = showPieceDestinations, onCheckedChange = { scope.launch { appPreferences.setChessShowDestinations(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Board highlights (last move and check)", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = showBoardHighlights, onCheckedChange = { scope.launch { appPreferences.setChessShowHighlights(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Move list while playing", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = showMoveListWhilePlaying, onCheckedChange = { scope.launch { appPreferences.setChessShowMoveList(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Material difference", color = scheme.onSurface, fontSize = 15.sp)
                                Text("Material difference", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Switch(checked = showMaterialDifference, onCheckedChange = { scope.launch { appPreferences.setChessShowMaterialDiff(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Clock position", color = scheme.onSurface, fontSize = 15.sp)
                                Text("Right", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                            Switch(checked = clockPositionRight, onCheckedChange = { scope.launch { appPreferences.setChessClockRight(it) } })
                        }
                    }
                }

                // ── Game Behaviour Section ──
                Text(
                    text = "Game behaviour",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = scheme.primary
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Premoves (playing during opponent turn)", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = enablePremoves, onCheckedChange = { scope.launch { appPreferences.setChessPremoves(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Confirm resignation and draw offers", color = scheme.onSurface, fontSize = 15.sp)
                            Switch(checked = confirmResignation, onCheckedChange = { scope.launch { appPreferences.setChessConfirmResign(it) } })
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Touch feedback", color = scheme.onSurface, fontSize = 15.sp)
                                Text(
                                    text = "When enabled, the device will vibrate shortly when you move or capture a piece.",
                                    color = scheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Switch(checked = enableTouchFeedback, onCheckedChange = { scope.launch { appPreferences.setChessTouchFeedback(it) } })
                        }
                    }
                }

                // ── Bot & Engine Settings Section ──
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bot & Engine Configuration",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = scheme.primary
                            )
                            TextButton(onClick = { showAdvancedEngine = !showAdvancedEngine }) {
                                Text(if (showAdvancedEngine) "Hide Advanced" else "Configure", fontSize = 12.sp, color = scheme.secondary)
                            }
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Bot Level", fontWeight = FontWeight.SemiBold, color = scheme.onSurface, fontSize = 15.sp)
                                    Text("Level $botLevel", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 15.sp)
                                }
                                Slider(
                                    value = botLevel.toFloat(),
                                    onValueChange = { vm.setBotLevel(it.roundToInt()) },
                                    valueRange = 1f..20f,
                                    steps = 18,
                                    colors = SliderDefaults.colors(thumbColor = scheme.primary, activeTrackColor = scheme.primary)
                                )

                                Text("Engine Version", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    com.creativeblisslabs.omnimind.core.chess.EngineVersion.entries.forEach { ver ->
                                        val sel = engineSettings.version == ver
                                        val isDownloaded = enginePacks.find { it.profile.label.contains(ver.label.split(" ")[0]) }?.isDownloaded ?: false

                                        OutlinedButton(
                                            onClick = { vm.updateEngineSettings(engineSettings.copy(version = ver)) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (sel) scheme.primary else Color.Transparent,
                                                disabledContainerColor = Color.Transparent
                                            ),
                                            enabled = isDownloaded,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (sel) scheme.primary else if (isDownloaded) Color(0x33FFFFFF) else Color(0x11FFFFFF)
                                            )
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    ver.label,
                                                    color = if (sel) scheme.onPrimary else if (isDownloaded) scheme.onSurface else Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                                if (!isDownloaded) {
                                                    Text("Not downloaded", color = scheme.onSurfaceVariant, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (showAdvancedEngine) {
                                    Spacer(Modifier.height(2.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Search time", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                                            Text("${engineSettings.searchTimeSec}s", color = scheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = engineSettings.searchTimeSec.toFloat(),
                                            onValueChange = { vm.updateEngineSettings(engineSettings.copy(searchTimeSec = it.roundToInt())) },
                                            valueRange = 1f..20f,
                                            steps = 18
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Analysis lines (MultiPV)", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                                            Text("${engineSettings.multiPvLines}", color = scheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Slider(
                                            value = engineSettings.multiPvLines.toFloat(),
                                            onValueChange = { vm.updateEngineSettings(engineSettings.copy(multiPvLines = it.roundToInt())) },
                                            valueRange = 1f..5f,
                                            steps = 3
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Engine Binaries",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = scheme.primary
                                    )
                                }
                                                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val displayPacks = if (enginePacks.isNotEmpty()) enginePacks else listOf(com.creativeblisslabs.omnimind.core.data.ai.ModelPackInfo(profile = com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile.Stockfish17))
                                        displayPacks.forEach { pack ->
                                            ModelPackCard(
                                                pack = pack,
                                                isPrimary = false,
                                                onSelect = {},
                                                onDownload = { vm.downloadPack(pack.profile) },
                                                onDelete = { vm.deletePack(pack.profile) },
                                                onImport = {
                                                    activeImportProfile = pack.profile
                                                    filePicker()
                                                }
                                            )
                                        }

                                        val aiAndTranslationPacks = allModelPacks.filter { it.profile.type != LocalModelType.CHESS_ENGINE }
                                        if (aiAndTranslationPacks.isNotEmpty()) {
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = "On-Device AI & Translation Models",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = scheme.secondary
                                            )
                                            aiAndTranslationPacks.forEach { pack ->
                                                ModelPackCard(
                                                    pack = pack,
                                                    isPrimary = false,
                                                    onSelect = {},
                                                    onDownload = { vm.downloadPack(pack.profile) },
                                                    onDelete = { vm.deletePack(pack.profile) },
                                                    onImport = {
                                                        activeImportProfile = pack.profile
                                                        filePicker()
                                                    }
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
@Composable
fun ModelPackCard(
    pack: ModelPackInfo,
    isPrimary: Boolean,
    onSelect: (String) -> Unit, // Changed callback parameter to emit the dynamic ID string
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardModifier = if (isPrimary) {
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(1.dp, AmberWarm, RoundedCornerShape(16.dp))
    } else {
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
    }

    GlassCard(modifier = cardModifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icon = when (pack.profile.type) {
                        LocalModelType.LLM -> Icons.Default.ChatBubble
                        LocalModelType.CHESS_ENGINE -> Icons.Default.SportsEsports
                        LocalModelType.TRANSLATION_PACK -> Icons.Default.Translate
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (pack.isDownloaded) NeonCyan.copy(0.15f) else SpaceBorder.copy(0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (pack.isDownloaded) NeonCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = pack.profile.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        // REPLACED: Uses the smart conditional size resolution layout safely
                        Text(
                            text = pack.displaySize,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                RadioButton(
                    selected = isPrimary,
                    onClick = { onSelect(pack.id) }, // Safely routes unique id identification signature
                    colors = RadioButtonDefaults.colors(selectedColor = AmberWarm)
                )
            }

            Text(
                text = pack.profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Dynamic progress logic container
            val isActiveProgress = pack.isImporting || (pack.downloadProgress in 1..99)
            val currentProgress = if (pack.isImporting) pack.importProgress else pack.downloadProgress

            if (isActiveProgress || pack.statusMessage.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (pack.statusMessage.isNotBlank()) {
                        Text(
                            text = pack.statusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarm,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (currentProgress.coerceIn(2, 100)) / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = AmberWarm,
                        trackColor = SpaceBorder
                    )
                }
            }

            // Footer row content buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pack.isDownloaded && !pack.isImporting) {
                    AssistChip(
                        onClick = onDelete,
                        label = { Text("Remove") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                    OutlinedButton(
                        onClick = onImport,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        border = BorderStroke(1.dp, SpaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp), tint = TextMuted)
                        Spacer(Modifier.width(6.dp))
                        Text("Re-import", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Ready to use",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Button(
                        onClick = onDownload,
                        enabled = !pack.isImporting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpaceBorder)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        Text("Download", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onImport,
                        enabled = !pack.isImporting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        border = BorderStroke(1.dp, SpaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp), tint = TextMuted)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (pack.isImporting) "Importing..." else "Import File",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
            }

            if (pack.lastError.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0x33FF5555)).padding(10.dp)
                ) {
                    Text(text = pack.lastError, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF8888), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}