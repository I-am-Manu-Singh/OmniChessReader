package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelType
import com.creativeblisslabs.omnimind.core.data.ai.ModelPackInfo
import com.creativeblisslabs.omnimind.core.data.ai.ModelPackManager
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.core.ui.rememberFilePicker
import com.creativeblisslabs.omnimind.core.ui.theme.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OmniChessSettingsScreen(navController: NavController) {
    val prefs: AppPreferences = koinInject()
    val modelPackManager: ModelPackManager = koinInject()
    val scope = rememberCoroutineScope()

    // Real Preferences & Model state flows
    val geminiKey by prefs.geminiApiKey.collectAsState(initial = "")
    val aiProvider by prefs.aiProvider.collectAsState(initial = "local_first")
    val localModel by prefs.localModel.collectAsState(initial = "Gemma4")
    val engineVersion by prefs.chessEngineVersion.collectAsState(initial = "Stockfish 17.1")
    val botLevel by prefs.chessBotLevel.collectAsState(initial = 10)
    val searchTime by prefs.chessEngineSearchTime.collectAsState(initial = 1000)
    val multiPv by prefs.chessEngineMultiPv.collectAsState(initial = 3)
    val appTheme by prefs.theme.collectAsState(initial = "dark")
    val translationLang by prefs.readerLanguage.collectAsState(initial = "en")
    val allModelPacks by modelPackManager.allPacks.collectAsState(initial = emptyList())

    var tempGeminiKey by remember(geminiKey) { mutableStateOf(geminiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var activeCategory by remember { mutableStateOf(0) } // 0: All, 1: AI Models & Packs, 2: Chess Engine, 3: Translation, 4: Theme
    var saveSuccess by remember { mutableStateOf(false) }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var importingProfile by remember { mutableStateOf<LocalModelProfile?>(null) }

    // Native file picker for actual local model archive/weights import (.task, .bin, .tflite, .gguf, .tar, .so)
    val filePicker = rememberFilePicker { uri ->
        importingProfile?.let { profile ->
            scope.launch(NonCancellable) {
                modelPackManager.importLocalPack(uri, profile)
            }
        }
        importingProfile = null
    }

    LaunchedEffect(Unit) {
        modelPackManager.refresh()
    }
Scaffold(
    topBar = {
        Surface(
            // Adds a clean, modern divider line under the app bar
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "OmniChess Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.15.sp
                        )
                        Text(
                            text = "AI Models · Engines · API Keys · Appearance",
                            style = MaterialTheme.typography.bodySmall, // Swapped to bodySmall for better readability
                            fontWeight = FontWeight.Medium,
                            color = AmberWarm.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), // Neutral tint for better button balance
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    // Elevated button design with dynamic background transition when saved
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.setGeminiApiKey(tempGeminiKey.trim())
                                saveSuccess = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (saveSuccess) Color(0xFF2E7D32) else AmberWarm,
                            contentColor = if (saveSuccess) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .height(40.dp)
                    ) {
                        Icon(
                            imageVector = if (saveSuccess) Icons.Default.DoneAll else Icons.Default.Check, 
                            null, 
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (saveSuccess) "Saved" else "Save", 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent // Controlled by parent Surface wrapper
                )
            )
        }
    },
    containerColor = MaterialTheme.colorScheme.background
) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // Category Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("AI Model Packs", "Chess Engine", "Theme")
                    categories.forEachIndexed { index, cat ->
                        val isSelected = activeCategory == index
                        Surface(
                            onClick = { activeCategory = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AmberWarm.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) AmberWarm else Color.Transparent)
                        ) {
                            Text(
                                text = cat,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = if (isSelected) AmberWarm else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ─── 1. AI MODEL PACKS & DOWNLOAD/EXTRACTION DASHBOARD ──────────────
            if (activeCategory == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsCardHeader(
                            title = "Local Intelligence Models",
                            subtitle = "Download, import & extract on-device neural models",
                            icon = Icons.Default.Memory
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            updateStatusMessage ?: "Swipe cards to manage packs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isCheckingUpdates = true
                                    modelPackManager.checkForUpdates()
                                        .onSuccess { updateStatusMessage = if (it) "Updates found!" else "All models up to date" }
                                        .onFailure { updateStatusMessage = "Check failed" }
                                    isCheckingUpdates = false
                                }
                            },
                            enabled = !isCheckingUpdates
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = AmberWarm)
                            } else {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text("Check Updates", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                item {
                    val displayPacks = allModelPacks.filter { 
                        it.profile.type == LocalModelType.LLM || 
                        it.profile.type == LocalModelType.CHESS_ENGINE || 
                        it.profile.type == LocalModelType.TRANSLATION_PACK 
                    }
                    val activeProfile = runCatching { LocalModelProfile.valueOf(localModel) }.getOrNull()
                    val primaryPackIds = if (activeProfile != null) setOf(activeProfile.id) else emptySet()

                    ModelCarouselDashboard(
                        allPacks = displayPacks,
                        primaryPackIds = primaryPackIds,
                        onSelect = { selectedPack ->
                            scope.launch {
                                prefs.setLocalModel(selectedPack.profile.name)
                            }
                        },
                        onDownload = { targetPack ->
                            scope.launch {
                                modelPackManager.download(targetPack.profile)
                            }
                        },
                        onDelete = { targetPack ->
                            scope.launch {
                                modelPackManager.deletePack(targetPack.profile)
                            }
                        },
                        onImport = { targetPack ->
                            importingProfile = targetPack.profile
                            filePicker()
                        }
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("AI Provider Mode", color = AmberWarm, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modes = listOf("local_first" to "Local First", "cloud_only" to "Gemini Cloud", "offline" to "100% Offline")
                                modes.forEach { (mode, label) ->
                                    val active = aiProvider == mode
                                    Surface(
                                        onClick = { scope.launch { prefs.setAiProvider(mode) } },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) AmberWarm.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (active) AmberWarm else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                label,
                                                fontSize = 12.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                color = if (active) AmberWarm else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Text("Google Gemini API Key", color = AmberWarm, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            OutlinedTextField(
                                value = tempGeminiKey,
                                onValueChange = { tempGeminiKey = it },
                                placeholder = { Text("AIzaSy...", color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(
                                            if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle Key Visibility",
                                            tint = AmberWarm
                                        )
                                    }
                                },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberWarm,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // ─── 2. CHESS ENGINE SETTINGS ──────────────────────────────────────
            if (activeCategory == 1) {
                item {
                    SettingsCardHeader(
                        title = "Chess Engine & UCI Parameters",
                        subtitle = "Select native UCI engine, strength & analysis parameters",
                        icon = Icons.Default.SportsEsports
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Active UCI Engine", color = AmberWarm, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Engine Strength / Level", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("Level $botLevel (${botLevel * 150} ELO)", color = AmberWarm, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Slider(
                                value = botLevel.toFloat(),
                                onValueChange = { scope.launch { prefs.setChessBotLevel(it.toInt()) } },
                                valueRange = 1f..20f,
                                steps = 18,
                                colors = SliderDefaults.colors(thumbColor = AmberWarm, activeTrackColor = AmberWarm)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Analysis Search Time", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("${searchTime}ms", color = AmberWarm, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Slider(
                                value = searchTime.toFloat(),
                                onValueChange = { scope.launch { prefs.setChessEngineSearchTime(it.toInt()) } },
                                valueRange = 200f..5000f,
                                steps = 23,
                                colors = SliderDefaults.colors(thumbColor = AmberWarm, activeTrackColor = AmberWarm)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Multi-PV Analysis Lines", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("$multiPv Lines", color = AmberWarm, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Slider(
                                value = multiPv.toFloat(),
                                onValueChange = { scope.launch { prefs.setChessEngineMultiPv(it.toInt()) } },
                                valueRange = 1f..5f,
                                steps = 3,
                                colors = SliderDefaults.colors(thumbColor = AmberWarm, activeTrackColor = AmberWarm)
                            )
                        }
                    }
                }
            }


            // ─── 3. APP THEME & APPEARANCE ─────────────────────────────────────
            if (activeCategory == 2) {
                item {
                    SettingsCardHeader(
                        title = "App Theme & Appearance",
                        subtitle = "Instant light/dark mode & palette toggle",
                        icon = Icons.Default.Palette
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Theme Mode", color = AmberWarm, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val themes = listOf("system" to "System", "dark" to "Dark", "light" to "Light", "oled" to "OLED Black")
                                themes.forEach { (tMode, tLabel) ->
                                    val active = appTheme.lowercase() == tMode
                                    Surface(
                                        onClick = { scope.launch { prefs.setTheme(tMode) } },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (active) AmberWarm.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (active) AmberWarm else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                            Text(
                                                tLabel,
                                                fontSize = 12.sp,
                                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                color = if (active) AmberWarm else MaterialTheme.colorScheme.onSurface
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
    onSelect: (String) -> Unit,
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
                        else -> Icons.Default.Extension
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (pack.isDownloaded) NeonCyan.copy(0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (pack.isDownloaded) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = pack.profile.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = pack.displaySize,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                RadioButton(
                    selected = isPrimary,
                    onClick = { onSelect(pack.id) },
                    colors = RadioButtonDefaults.colors(selectedColor = AmberWarm)
                )
            }

            Text(
                text = pack.profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            }

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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Re-import", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Ready",
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
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarm, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onImport,
                        enabled = !pack.isImporting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.FileUpload, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (pack.isImporting) "Extracting..." else "Import File",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModelCarouselDashboard(
    allPacks: List<ModelPackInfo>,
    primaryPackIds: Set<String>,
    onSelect: (ModelPackInfo) -> Unit,
    onDownload: (ModelPackInfo) -> Unit,
    onDelete: (ModelPackInfo) -> Unit,
    onImport: (ModelPackInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(LocalModelType.LLM, LocalModelType.CHESS_ENGINE, LocalModelType.TRANSLATION_PACK)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        categories.forEach { type ->
            val filteredPacks = allPacks.filter { it.profile.type == type }

            if (filteredPacks.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { filteredPacks.size })

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = type.name.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall,
                        color = AmberWarm,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val currentPack = filteredPacks[page]

                        ModelPackCard(
                            pack = currentPack,
                            isPrimary = primaryPackIds.contains(currentPack.id),
                            onSelect = { onSelect(currentPack) },
                            onDownload = { onDownload(currentPack) },
                            onDelete = { onDelete(currentPack) },
                            onImport = { onImport(currentPack) }
                        )
                    }

                    if (filteredPacks.size > 1) {
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration) AmberWarm else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCardHeader(title: String, subtitle: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AmberWarm.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = AmberWarm, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
