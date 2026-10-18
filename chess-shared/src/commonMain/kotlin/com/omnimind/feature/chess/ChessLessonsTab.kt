package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsTabContent(
    vm: ChessViewModel,
    onSwitchToTab: (String) -> Unit,
    onPuzzleThemeSelected: (String) -> Unit = {}
) {
    val openingExplorerData by vm.openingExplorerData.collectAsStateWithLifecycle()
    val localOpeningInfo by vm.localOpeningInfo.collectAsStateWithLifecycle()
    val openingCategories by vm.openingCategories.collectAsStateWithLifecycle()
    val endgamePositions by vm.endgamePositions.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val currentLessonIndex by vm.currentLessonIndex.collectAsStateWithLifecycle()
    val lessonStepIndex by vm.lessonStepIndex.collectAsStateWithLifecycle()
    val currentFen by vm.fen.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val threatMap by vm.threatMap.collectAsStateWithLifecycle()
    val showThreatMap by vm.showThreatMap.collectAsStateWithLifecycle()
    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()
    val moveHistory by vm.moveHistory.collectAsStateWithLifecycle()
    val hasRedo by vm.hasRedo.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme

    var showChaptersSheet by remember { mutableStateOf(false) }
    var selectedSquare by remember { mutableStateOf<String?>(null) }
    var lessonsTab by remember { mutableStateOf("Lessons") }
    var showSelectorMenu by remember { mutableStateOf(false) }

    // Reset board on entry so moves from Game tab don't carry over into lessons/openings boards
    LaunchedEffect(Unit) {
        vm.resetGame()
    }

    var activeMiddlegameIdx by remember { mutableIntStateOf(0) }
    var activeEndgameIdx by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val currentLesson = lessons.getOrNull(currentLessonIndex)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Navigation Rail for Lesson Sub-tabs
                NavigationRail(
                    modifier = Modifier.fillMaxHeight().clip(RoundedCornerShape(16.dp)),
                    containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.5f)
                ) {
                    val tabs = listOf(
                        "Lessons" to Icons.Filled.School,
                        "Openings" to Icons.Filled.MenuBook,
                        "Middlegames" to Icons.Filled.Psychology,
                        "Endgames" to Icons.Filled.EmojiEvents
                    )
                    tabs.forEach { (title, icon) ->
                        NavigationRailItem(
                            selected = lessonsTab == title,
                            onClick = { lessonsTab = title },
                            icon = { Icon(icon, null) },
                            label = { Text(title, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = scheme.onPrimary,
                                selectedTextColor = scheme.primary,
                                indicatorColor = scheme.primary
                            )
                        )
                    }
                }

                // Left Column: Dropdown Selector, Interactive Board & Control Bar
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Selector Dropdown
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showSelectorMenu = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (lessonsTab) {
                                                "Lessons" -> currentLesson?.title ?: "Select Lesson"
                                                "Openings" -> localOpeningInfo?.name ?: "Opening Explorer"
                                                "Middlegames" -> defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx)?.title ?: "Middlegame Topic"
                                                else -> endgamePositions.getOrNull(activeEndgameIdx)?.name ?: "Endgame Drill"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (lessonsTab) {
                                                "Lessons" -> "${currentLesson?.category ?: "Interactive"} • Chapter ${lessonStepIndex + 1}"
                                                "Openings" -> "ECO: ${localOpeningInfo?.eco ?: "A00"} • Theory & Master Games"
                                                "Middlegames" -> "${defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx)?.category ?: "Strategy"} • Masterclass"
                                                else -> "ECO: ${endgamePositions.getOrNull(activeEndgameIdx)?.eco ?: "Endgame"} • Practice Drill"
                                            },
                                            fontSize = 11.sp,
                                            color = scheme.secondary
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Select option")
                                }

                                DropdownMenu(
                                    expanded = showSelectorMenu,
                                    onDismissRequest = { showSelectorMenu = false }
                                ) {
                                    when (lessonsTab) {
                                        "Lessons" -> {
                                            lessons.forEachIndexed { index, lesson ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(text = lesson.title, fontWeight = FontWeight.Bold)
                                                            Text(text = "${lesson.category} • ${lesson.chapterCount} Chapters", fontSize = 11.sp, color = scheme.secondary)
                                                        }
                                                    },
                                                    onClick = {
                                                        vm.loadLesson(index)
                                                        showSelectorMenu = false
                                                    }
                                                )
                                            }
                                        }
                                        "Openings" -> {
                                            openingCategories.forEach { category ->
                                                Text(
                                                    text = category.name,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = scheme.primary
                                                )
                                                category.positions.forEach { pos ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(text = pos.name, fontWeight = FontWeight.Bold)
                                                                Text(text = "ECO: ${pos.eco}", fontSize = 11.sp, color = scheme.secondary)
                                                            }
                                                        },
                                                        onClick = {
                                                            vm.updateFen(pos.fen)
                                                            showSelectorMenu = false
                                                        }
                                                    )
                                                }
                                                HorizontalDivider()
                                            }
                                        }
                                        "Middlegames" -> {
                                            defaultMiddlegameTopics.forEachIndexed { idx, topic ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(text = topic.title, fontWeight = FontWeight.Bold)
                                                            Text(text = topic.category, fontSize = 11.sp, color = scheme.secondary)
                                                        }
                                                    },
                                                    onClick = {
                                                        activeMiddlegameIdx = idx
                                                        vm.updateFen(topic.fen)
                                                        showSelectorMenu = false
                                                    }
                                                )
                                            }
                                        }
                                        "Endgames" -> {
                                            endgamePositions.forEachIndexed { idx, pos ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Column {
                                                            Text(text = pos.name, fontWeight = FontWeight.Bold)
                                                            Text(text = "ECO: ${pos.eco}", fontSize = 11.sp, color = scheme.secondary)
                                                        }
                                                    },
                                                    onClick = {
                                                        activeEndgameIdx = idx
                                                        vm.updateFen(pos.fen)
                                                        showSelectorMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Interactive Board View
                            ChessBoardContainer(
                                fen = currentFen,
                                theme = theme,
                                isPlayerWhite = isPlayerWhite,
                                selectedSquare = selectedSquare,
                                premove = null,
                                showThreatMap = showThreatMap,
                                threatMap = threatMap,
                                pieceStyle = pieceStyle,
                                moveHistory = moveHistory,
                                onSquareClick = { sq ->
                                    if (selectedSquare == null) {
                                        selectedSquare = sq
                                    } else {
                                        val from = selectedSquare!!
                                        selectedSquare = null
                                        vm.makeMove(from, sq)
                                    }
                                }
                            )

                            // Reusable Move Control Bar
                            ReusableChessControlBar(                               moveHistory = moveHistory,
                                currentMoveIndex = (moveHistory.size - 1).coerceAtLeast(0),
                                onSelectMove = { index -> vm.goToMove(index) },
                                onFirstMove = { vm.goToMove(0) },
                                onPrevMove = { vm.undoLastMove() },
                                onNextMove = { vm.redoNextMove() },
                                onLastMove = { vm.goToMove((moveHistory.size - 1).coerceAtLeast(0)) },
                                onFlipBoard = { vm.setPlayerColor(!isPlayerWhite) },
                                onResetBoard = { vm.resetGame() },
                                hasRedo = hasRedo
                            )
                        }
                    }
                }

                // Right Column: Masterclass Theory & Sub-tab Specific Learning Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Theory & Strategic Explanation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.primary)
                            when (lessonsTab) {
                                "Lessons" -> {
                                    currentLesson?.let { lesson ->
                                        val totalSteps = lesson.keyMoves.size
                                        val safeStepIndex = if (totalSteps > 0) lessonStepIndex.coerceIn(0, totalSteps - 1) else 0
                                        val currentMove = lesson.keyMoves.getOrNull(safeStepIndex)
                                        val currentExplanation = lesson.explanations.getOrNull(safeStepIndex) ?: if (totalSteps == 0) "No lesson steps available." else "Finished lesson!"

                                        Surface(shape = RoundedCornerShape(8.dp), color = scheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(text = if (totalSteps > 0) "Step ${safeStepIndex + 1}/$totalSteps" else "No steps", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                                                currentMove?.let { move -> Text(text = "Move: $move", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 13.sp) }
                                                Text(text = currentExplanation, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                                "Openings" -> {
                                    val data = openingExplorerData
                                    if (data != null && data.moves.isNotEmpty()) {
                                        Text("Candidate Theory Moves:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            data.moves.take(6).forEach { move ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF1E1E22))
                                                        .clickable {
                                                            if (move.uci.length >= 4) {
                                                                vm.makeMove(move.uci.substring(0, 2), move.uci.substring(2, 4))
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(move.san, fontWeight = FontWeight.Bold, color = scheme.onSurface, fontSize = 13.sp)
                                                    Text("${move.totalGames} master games", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                                "Middlegames" -> {
                                    val topic = defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx) ?: defaultMiddlegameTopics.first()
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Strategic Plans & Goals:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                        Text(topic.explanation, fontSize = 12.sp, color = scheme.onSurfaceVariant, lineHeight = 16.sp)
                                    }
                                }
                                else -> {
                                    val pos = endgamePositions.getOrNull(activeEndgameIdx) ?: endgamePositions.first()
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Endgame Objectives:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                        Text("Practice executing precise endgame technique for ${pos.name} (ECO: ${pos.eco}).", fontSize = 12.sp, color = scheme.onSurfaceVariant)
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
        // ── SUB-TAB BAR (Lessons | Openings | Middlegames | Endgames) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(Color(0x1F888888), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Lessons", "Openings", "Middlegames", "Endgames").forEach { t ->
                val isActive = lessonsTab == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) scheme.primary else Color.Transparent)
                        .clickable { lessonsTab = t }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) scheme.onPrimary else scheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── UNIFIED GLASS CARD CONTAINER FOR ALL SUB-TABS ──
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── 1. TOP SELECTOR DROPDOWN ──
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showSelectorMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (lessonsTab) {
                                    "Lessons" -> currentLesson?.title ?: "Select Lesson"
                                    "Openings" -> localOpeningInfo?.name ?: "Opening Explorer"
                                    "Middlegames" -> defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx)?.title ?: "Middlegame Topic"
                                    else -> endgamePositions.getOrNull(activeEndgameIdx)?.name ?: "Endgame Drill"
                                },
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when (lessonsTab) {
                                    "Lessons" -> "${currentLesson?.category ?: "Interactive"} • Chapter ${lessonStepIndex + 1}"
                                    "Openings" -> "ECO: ${localOpeningInfo?.eco ?: "A00"} • Theory & Master Games"
                                    "Middlegames" -> "${defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx)?.category ?: "Strategy"} • Masterclass"
                                    else -> "ECO: ${endgamePositions.getOrNull(activeEndgameIdx)?.eco ?: "Endgame"} • Practice Drill"
                                },
                                fontSize = 11.sp,
                                color = scheme.secondary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Select option")
                    }

                    DropdownMenu(
                        expanded = showSelectorMenu,
                        onDismissRequest = { showSelectorMenu = false }
                    ) {
                        when (lessonsTab) {
                            "Lessons" -> {
                                lessons.forEachIndexed { index, lesson ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = lesson.title, fontWeight = FontWeight.Bold)
                                                Text(text = "${lesson.category} • ${lesson.chapterCount} Chapters", fontSize = 11.sp, color = scheme.secondary)
                                            }
                                        },
                                        onClick = {
                                            vm.loadLesson(index)
                                            showSelectorMenu = false
                                        }
                                    )
                                }
                            }
                            "Openings" -> {
                                openingCategories.forEach { category ->
                                    Text(
                                        text = category.name,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = scheme.primary
                                    )
                                    category.positions.forEach { pos ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(text = pos.name, fontWeight = FontWeight.Bold)
                                                    Text(text = "ECO: ${pos.eco}", fontSize = 11.sp, color = scheme.secondary)
                                                }
                                            },
                                            onClick = {
                                                vm.updateFen(pos.fen)
                                                showSelectorMenu = false
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                            "Middlegames" -> {
                                defaultMiddlegameTopics.forEachIndexed { idx, topic ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = topic.title, fontWeight = FontWeight.Bold)
                                                Text(text = topic.category, fontSize = 11.sp, color = scheme.secondary)
                                            }
                                        },
                                        onClick = {
                                            activeMiddlegameIdx = idx
                                            vm.updateFen(topic.fen)
                                            showSelectorMenu = false
                                        }
                                    )
                                }
                            }
                            "Endgames" -> {
                                endgamePositions.forEachIndexed { idx, pos ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = pos.name, fontWeight = FontWeight.Bold)
                                                Text(text = "ECO: ${pos.eco}", fontSize = 11.sp, color = scheme.secondary)
                                            }
                                        },
                                        onClick = {
                                            activeEndgameIdx = idx
                                            vm.updateFen(pos.fen)
                                            showSelectorMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 2. DESCRIPTION & ASSIST CHIPS ──
                val currentDescription = when (lessonsTab) {
                    "Lessons" -> currentLesson?.description ?: ""
                    "Openings" -> "Master the opening principles, theoretical lines, and strategic plans for key chess openings."
                    "Middlegames" -> defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx)?.description ?: ""
                    else -> "Master essential endgame techniques and practice theoretical winning/drawing positions."
                }
                if (currentDescription.isNotBlank()) {
                    Text(text = currentDescription, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when (lessonsTab) {
                                    "Lessons" -> currentLesson?.category ?: "General"
                                    "Openings" -> localOpeningInfo?.eco ?: "Theory"
                                    "Middlegames" -> defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx)?.category ?: "Strategy"
                                    else -> endgamePositions.getOrNull(activeEndgameIdx)?.eco ?: "Endgame"
                                },
                                fontSize = 10.sp
                            )
                        }
                    )
                    if (lessonsTab == "Lessons" && (currentLesson?.chapterCount ?: 0) > 0) {
                        Text(
                            text = "${currentLesson?.chapterCount} Chapters",
                            fontSize = 11.sp,
                            color = scheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable { showChaptersSheet = true }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                        )
                    } else {
                        Text(
                            text = "Interactive Masterclass",
                            fontSize = 11.sp,
                            color = scheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // ── 3. CHESSBOARD CONTAINER (Shared across all 4 sub-tabs) ──
                ChessBoardContainer(
                    fen = currentFen,
                    theme = theme,
                    pieceStyle = pieceStyle,
                    isPlayerWhite = isPlayerWhite,
                    selectedSquare = selectedSquare,
                    premove = null,
                    showThreatMap = showThreatMap,
                    threatMap = threatMap,
                    visualArrows = visualArrows,
                    engineEvaluation = evaluation,
                    moveHistory = moveHistory,
                    onSquareClick = { clicked ->
                        val cur = selectedSquare
                        if (cur == null) {
                            selectedSquare = clicked
                        } else {
                            if (cur != clicked) {
                                vm.makeMove(cur, clicked)
                            }
                            selectedSquare = null
                        }
                    }
                )
                
                // ── 4. LICHESS STUDY BOTTOM NAVIGATION BAR ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF141312)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chapters Button
                    TextButton(
                        onClick = { showChaptersSheet = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.MenuBook, "Chapters", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${currentLesson?.keyMoves?.size ?: currentLesson?.chapterCount ?: 0} Chapters",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Back Button
                    TextButton(
                        onClick = { vm.prevLessonStep() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.SkipPrevious, "Back", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Back",
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Next / View Solution Button
                    Surface(
                        color = Color(0xFF262421),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { vm.nextLessonStep() }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, "Next", tint = Color(0xFFE5A968), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Next chapter",
                                fontSize = 11.sp,
                                color = Color(0xFFE5A968),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── 5. SUB-TAB SPECIFIC MASTERCLASS CONTENT ──
                when (lessonsTab) {
                    "Lessons" -> {
                        currentLesson?.let { lesson ->
                            val totalSteps = lesson.keyMoves.size
                            val safeStepIndex = if (totalSteps > 0) lessonStepIndex.coerceIn(0, totalSteps - 1) else 0
                            val currentMove = lesson.keyMoves.getOrNull(safeStepIndex)
                            val currentExplanation = lesson.explanations.getOrNull(safeStepIndex) ?: if (totalSteps == 0) "No lesson steps available." else "Finished lesson!"

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1B1917),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (totalSteps > 0) "Chapter ${safeStepIndex + 1}/$totalSteps" else "Chapter 1",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE5A968),
                                            fontWeight = FontWeight.Bold
                                        )
                                        currentMove?.let { move ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF332B22),
                                                modifier = Modifier.clickable {
                                                    if (move.length >= 4) {
                                                        vm.makeMove(move.substring(0, 2), move.substring(2, 4))
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "Play: $move ➔",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE5A968),
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    val annotatedExplanation = remember(currentExplanation) {
                                        buildAnnotatedString {
                                            val tokens = currentExplanation.split(" ")
                                            tokens.forEach { token ->
                                                val isMoveToken = token.matches(Regex("^[a-h][1-8]|[KQRBN][a-h]?[1-8]?x?[a-h][1-8]|O-O(-O)?|\\d+\\.[a-hKQRBN].*"))
                                                if (isMoveToken) {
                                                    withStyle(SpanStyle(color = Color(0xFFE5A968), fontWeight = FontWeight.ExtraBold)) {
                                                        append("$token ")
                                                    }
                                                } else {
                                                    append("$token ")
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = annotatedExplanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    "Openings" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Jump to popular openings:", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                            val isMasters by vm.isOpeningMastersDatabase.collectAsStateWithLifecycle()
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = isMasters,
                                    onClick = { vm.toggleOpeningDatabaseSource(true) },
                                    label = { Text("Masters", fontSize = 9.sp) }
                                )
                                FilterChip(
                                    selected = !isMasters,
                                    onClick = { vm.toggleOpeningDatabaseSource(false) },
                                    label = { Text("Lichess", fontSize = 9.sp) }
                                )
                            }
                        }

                        val presets = listOf(
                            Triple("Ruy Lopez", "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3", ""),
                            Triple("Sicilian Defense", "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2", ""),
                            Triple("Queen's Gambit", "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2", ""),
                            Triple("French Defense", "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", ""),
                            Triple("Caro-Kann", "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2", ""),
                            Triple("Italian Game", "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3", "")
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(presets) { (name, fen, _) ->
                                AssistChip(onClick = { vm.updateFen(fen) }, label = { Text(name, fontSize = 10.sp) })
                            }
                        }

                        val data = openingExplorerData
                        if (data != null) {
                            val openingName = data.opening?.name ?: "Custom Position"
                            val eco = data.opening?.eco ?: ""
                            Text(text = if (eco.isNotEmpty()) "[$eco] $openingName" else openingName, fontWeight = FontWeight.Bold, color = scheme.secondary, fontSize = 14.sp)
                            if (data.moves.isNotEmpty()) {
                                Text("Candidate Theory Moves:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    data.moves.take(6).forEach { move ->
                                        val total = (move.white + move.draws + move.black).coerceAtLeast(1)
                                        val wPct = (move.white * 100 / total)
                                        val dPct = (move.draws * 100 / total)
                                        val bPct = (move.black * 100 / total)

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = scheme.surfaceContainerHigh,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (move.uci.length >= 4) {
                                                        vm.makeMove(move.uci.substring(0, 2), move.uci.substring(2, 4))
                                                    }
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(move.san, fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 14.sp)
                                                        Spacer(Modifier.width(8.dp))
                                                        Text("$total games", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                                                    }
                                                    Text("$wPct% / $dPct% / $bPct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                                                }

                                                // Lichess proportional win/draw/loss ratio bar
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(3.dp))
                                                        .background(Color(0xFF333333))
                                                ) {
                                                    if (wPct > 0) Box(modifier = Modifier.weight(wPct.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(Color(0xFF629924)))
                                                    if (dPct > 0) Box(modifier = Modifier.weight(dPct.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(Color(0xFFA0A0A0)))
                                                    if (bPct > 0) Box(modifier = Modifier.weight(bPct.toFloat().coerceAtLeast(1f)).fillMaxHeight().background(Color(0xFF36485E)))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Middlegames" -> {
                        val topic = defaultMiddlegameTopics.getOrNull(activeMiddlegameIdx) ?: defaultMiddlegameTopics.first()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Strategic Plans & Goals:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                            Text(topic.explanation, fontSize = 12.sp, color = scheme.onSurfaceVariant, lineHeight = 16.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                defaultMiddlegameTopics.take(4).forEachIndexed { idx, top ->
                                    AssistChip(
                                        onClick = {
                                            activeMiddlegameIdx = idx
                                            vm.updateFen(top.fen)
                                        },
                                        label = { Text(top.category.take(12), fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    "Endgames" -> {
                        val endgamePos = endgamePositions.getOrNull(activeEndgameIdx)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Endgame Key Technique:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                            Text(
                                text = endgamePos?.let { "Study ${it.name} (${it.eco}). Maintain active piece placement and king centralization." } ?: "Focus on king activity, opposition, and pawn promotion key squares.",
                                fontSize = 12.sp,
                                color = scheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    endgamePos?.let { pos ->
                                        scope.launch {
                                            vm.updateFen(pos.fen)
                                            vm.setGameMode(com.creativeblisslabs.omnimind.core.chess.GameMode.VS_BOT)
                                            onSwitchToTab("Game")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = scheme.primary),
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Practice against Stockfish Bot ▶", color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChaptersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChaptersSheet = false },
            containerColor = Color(0xFF1B1917),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${currentLesson?.keyMoves?.size ?: currentLesson?.chapterCount ?: 0} Chapters",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val steps = currentLesson?.keyMoves ?: emptyList()
                if (steps.isEmpty()) {
                    Text("No chapters available for this study lesson.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        steps.forEachIndexed { idx, move ->
                            val isCurrent = idx == lessonStepIndex
                            val chapterName = currentLesson?.explanations?.getOrNull(idx)?.substringBefore(".")?.let { "$it" } ?: "Chapter ${idx + 1}"
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        repeat(kotlin.math.abs(idx - lessonStepIndex)) {
                                            if (idx > lessonStepIndex) vm.nextLessonStep() else vm.prevLessonStep()
                                        }
                                        showChaptersSheet = false
                                    }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1} $chapterName",
                                    fontSize = 17.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrent) Color(0xFFE5A968) else Color.White
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
}
}
