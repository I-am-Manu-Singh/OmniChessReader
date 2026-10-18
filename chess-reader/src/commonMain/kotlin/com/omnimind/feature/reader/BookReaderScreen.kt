package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupProperties
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.LayoutCoordinates
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.ui.theme.*
import com.creativeblisslabs.omnimind.core.data.database.dao.BookDao
import com.creativeblisslabs.omnimind.core.ui.util.*
import com.creativeblisslabs.omnimind.core.media.*
import com.creativeblisslabs.omnimind.core.navigation.Screen
import androidx.compose.foundation.text.selection.SelectionContainer
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.text.contains
import kotlin.text.get
import kotlin.text.toFloat
import kotlin.text.toInt

data class ChapterItem(val title: String, val page: Int)

data class ParsedParagraph(
    val rawTrimmed: String,
    val isHeading: Boolean,
    val isBulletList: Boolean,
    val isChapterHeader: Boolean,
    val isImage: Boolean,
    val imageUrl: String,
    val annotatedParagraph: androidx.compose.ui.text.AnnotatedString
)

@Composable
fun BookReaderScreen(
    navController: NavController,
    viewModel: ReaderHubViewModel = koinViewModel()
) {
    // ImmersiveSystemBarsEffect(enable = true)
    val session = ReaderSessionStore.state
    var showMenu by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showTocSheet by remember { mutableStateOf(false) }
    var showBookDetailsSheet by remember { mutableStateOf(false) }
    var showBrightnessSheet by remember { mutableStateOf(false) }
    var showQuickFontSheet by remember { mutableStateOf(false) }
    var showQuickSeekSheet by remember { mutableStateOf(false) }
    var selectedTocTab by remember { mutableStateOf(0) }
    val dbBookmarks by viewModel.bookmarks.collectAsState()
    val bookmarksList = remember(dbBookmarks) {
        dbBookmarks.filter { it.noteText.isBlank() }.map { it.title }
    }
    val notesList = remember(dbBookmarks) {
        dbBookmarks.filter { it.noteText.isNotBlank() }.map { "${it.title}: ${it.noteText}" }
    }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteInputText by remember { mutableStateOf("") }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val isAutoScrolling = viewModel.isAutoScrolling
    val isScrolledMode = viewModel.isScrolledMode
    val isParagraphMode = viewModel.isParagraphMode
    val isSpeedReading = viewModel.isSpeedReading
    val isAutoMode = viewModel.isAutoMode

    val layoutResults =
        remember { mutableStateMapOf<Int, androidx.compose.ui.text.TextLayoutResult>() }
    val paragraphCoordinates = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    var readerBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

// Translation & Word Lens configuration states:
    val isTranslationActive = viewModel.enableTranslation
    val translationTarget = viewModel.translateToLanguage
    val activeTranslationService = viewModel.translationService
    val isWordLensActive = viewModel.wordLensEnabled

    SetScreenBrightnessEffect(
        brightness = viewModel.brightnessValue.toFloat() / 100f,
        isAuto = viewModel.autoBrightness
    )
    var selectedSheetTab by remember { mutableStateOf(0) }

    // Lock controls state
    var isLocked by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    // Text selection simulation
    var showSelectionTray by remember { mutableStateOf(false) }
    var selectedWord by remember { mutableStateOf<String?>(null) }
    var selectionOffset by remember { mutableStateOf(IntOffset.Zero) }
    var selectedParagraphIndex by remember { mutableStateOf(-1) }
    var selectedRangeStart by remember { mutableStateOf(0) }
    var selectedRangeEnd by remember { mutableStateOf(0) }

    var showTtsQuickConfig by remember { mutableStateOf(false) }
    var showVoiceNoteCapture by remember { mutableStateOf(false) }
    var showTranslationDialog by remember { mutableStateOf(false) }
    var translationSourceText by remember { mutableStateOf("") }

    var showAiCompanionDialog by remember { mutableStateOf(false) }
    var aiCompanionContextText by remember { mutableStateOf("") }
    var aiCustomPromptInput by remember { mutableStateOf("") }
    var aiSelectedScope by remember { mutableStateOf("Selection") }
    var aiSelectedMode by remember { mutableStateOf("Custom Prompt") }
    var aiAnalysisResult by remember { mutableStateOf<String?>(null) }
    var isAiAnalyzing by remember { mutableStateOf(false) }

    val mediaActions = rememberMediaActions(
        onImagePicked = {},
        onFilePicked = {},
        onAudioPicked = {},
        onClipboardImage = {}
    )

    val orientationController = rememberScreenOrientationController()
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.orientationSelected, viewModel.keepScreenAwake) {
        orientationController.setOrientation(viewModel.orientationSelected)
        orientationController.setKeepScreenAwake(viewModel.keepScreenAwake)
    }
    val scheme = MaterialTheme.colorScheme
    val themeBg = when (viewModel.activeColorTheme) {
        "Sepia" -> Color(0xFFF6EED9)
        "OLED", "AMOLED" -> Color(0xFF000000)
        "Light", "Paper" -> Color(0xFFFAF9F6)
        "Solarized" -> Color(0xFFFDF6E3)
        "Gruvbox" -> Color(0xFF282828)
        "Nord" -> Color(0xFF2E3440)
        "Sunset" -> Color(0xFF2D1B2D)
        "Grass" -> Color(0xFF1B281E)
        "Cherry" -> Color(0xFF2A1C20)
        "Gray" -> Color(0xFF222222)
        "E-Ink" -> Color(0xFFFFFFFF)
        else -> Color(0xFF121212)
    }
    val themeText = when (viewModel.activeColorTheme) {
        "Sepia" -> Color(0xFF2B2218)
        "Light", "Paper" -> Color(0xFF141414)
        "Solarized" -> Color(0xFF073642)
        "E-Ink" -> Color(0xFF000000)
        "OLED", "AMOLED" -> Color(0xFFF5F5F5)
        "Gruvbox" -> Color(0xFFEBDBB2)
        "Nord" -> Color(0xFFECEFF4)
        "Sunset" -> Color(0xFFF5E6F5)
        "Grass" -> Color(0xFFE2F0D8)
        "Cherry" -> Color(0xFFFCE2E6)
        "Gray" -> Color(0xFFE5E5E5)
        else -> Color(0xFFEDEDED)
    }
    val selectedFont = when {
        viewModel.fontFaceSerif.isNotBlank() && viewModel.fontFaceSerif != "System Sans" -> FontFamily.Serif
        viewModel.fontFaceMono.isNotBlank() && viewModel.fontFaceMono != "System Sans" -> FontFamily.Monospace
        viewModel.fontFaceSans.isNotBlank() -> FontFamily.SansSerif
        else -> FontFamily.Serif
    }

    // Track whether we are still loading chunks from Room DB (prevents blank flash on restart)
    var isLoadingChunks by remember(session.bookId) { mutableStateOf(session.chunks.isEmpty() && session.bookId.isNotBlank()) }

    // Check for empty state and try to reload if necessary
    LaunchedEffect(session.bookId, session.chunks.isEmpty()) {
        if (session.bookId.isNotBlank() && session.chunks.isEmpty()) {
            isLoadingChunks = true
            viewModel.loadBookChunks(session.bookId)
        }
    }

    // Update session when ViewModel chunks change
    LaunchedEffect(viewModel.chunks.size) {
        if (viewModel.chunks.isNotEmpty()) {
            val chunkTexts = viewModel.chunks.map { c -> c.text }
            ReaderSessionStore.update { copy(chunks = chunkTexts) }
            isLoadingChunks = false
        }
    }

    // Stop loading if session already has chunks
    LaunchedEffect(session.chunks.size) {
        if (session.chunks.isNotEmpty()) {
            isLoadingChunks = false
        }
    }

    val activeText = remember(session.chunks, session.chapters, viewModel.chunks.size) {
        val vmTextList = viewModel.chunks.map { it.text }.filter { it.isNotBlank() }
        val sessionChunks = session.chunks.filter { it.isNotBlank() }
        val nonBlankChapters = session.chapters.filter { it.isNotBlank() }

        // Prioritize raw chunks if present to avoid empty chapter container overrides
        when {
            sessionChunks.isNotEmpty() -> sessionChunks.joinToString("\n\n")
            vmTextList.isNotEmpty() -> vmTextList.joinToString("\n\n")
            nonBlankChapters.isNotEmpty() -> nonBlankChapters.joinToString("\n\n")
            else -> ""
        }
    }


    val paragraphs = remember(activeText) {
        val doubleNewlineSplit = activeText.split("\n\n").filter { it.isNotBlank() }
        if (doubleNewlineSplit.size > 1) {
            doubleNewlineSplit
        } else {
            activeText.split("\n").filter { it.isNotBlank() }
        }
    }

    val (chapterHeadingIndices, chapters) = remember(session.chapters, paragraphs) {
        val indices = mutableListOf<Int>()
        val items = mutableListOf<ChapterItem>()

        paragraphs.forEachIndexed { pIdx, p ->
            val rawTrimmed =
                unescapeHtml(p).replace(Regex("<[^>]*>"), "").trim()
            if (rawTrimmed.isBlank() || rawTrimmed.startsWith("### _") || rawTrimmed.startsWith("_")) return@forEachIndexed

            // Only consider clean markdown headings (#, ##) or short standalone chapter lines (< 60 chars)
            val isMarkdownHeader = rawTrimmed.startsWith("#")
            val isChapterTitle = !rawTrimmed.startsWith("- ") && !rawTrimmed.startsWith("*") &&
                    rawTrimmed.length <= 60 && (
                        Regex("^(?:CHAPTER|Chapter|PART|Part|SECTION|Section|PROLOGUE|Prologue|EPILOGUE|Epilogue|BOOK|Book)\\s+[0-9IVXLCDM]+", RegexOption.IGNORE_CASE).containsMatchIn(rawTrimmed) ||
                        Regex("^[0-9]+\\.\\s+[A-Z]").containsMatchIn(rawTrimmed)
                    )

            if (isMarkdownHeader || isChapterTitle) {
                val cleanTitle = rawTrimmed.replace(Regex("^#+\\s*"), "").trim()
                if (cleanTitle.isNotBlank()) {
                    val displayTitle =
                        if (cleanTitle.length > 40) cleanTitle.take(40) + "..." else cleanTitle
                    val totalParas = paragraphs.size.coerceAtLeast(1)
                    val estimatedTotalPages = (totalParas / 4).coerceAtLeast(1)
                    val pageNum = ((pIdx.toFloat() / totalParas) * estimatedTotalPages).toInt() + 1
                    indices.add(pIdx)
                    items.add(ChapterItem(displayTitle, pageNum))
                }
            }
        }

        if (items.isEmpty() && paragraphs.isNotEmpty()) {
            indices.add(0)
            items.add(ChapterItem("Beginning of Document", 1))
        }

        indices to items
    }

    val parsedParagraphs: List<ParsedParagraph> = remember(paragraphs, viewModel.fontWeightVal, chapterHeadingIndices) {
        paragraphs.mapIndexed { index: Int, p: String ->
            val rawTrimmed = unescapeHtml(p).trim()
            val isImage = (rawTrimmed.startsWith("![") && rawTrimmed.contains("](")) || (rawTrimmed.startsWith("http") && (rawTrimmed.endsWith(".jpg") || rawTrimmed.endsWith(".png") || rawTrimmed.endsWith(".webp")))
            val imageUrl = if (isImage) {
                if (rawTrimmed.contains("](")) {
                    val start = rawTrimmed.indexOf("](") + 2
                    val end = rawTrimmed.indexOf(")", start)
                    if (start > 1 && end > start) rawTrimmed.substring(start, end) else rawTrimmed
                } else rawTrimmed
            } else ""

            val isChapterHeader = chapterHeadingIndices.contains(index)
            val isBulletList = rawTrimmed.startsWith("- ") || rawTrimmed.startsWith("* ")
            val cleanP = unescapeHtml(
                when {
                    isBulletList -> rawTrimmed.removePrefix("- ").removePrefix("* ").trim()
                    isChapterHeader -> rawTrimmed.removePrefix("#").trim()
                    else -> rawTrimmed
                }
            ).replace(Regex("<[^>]*>"), "")

            val processedParagraph = cleanP

            val isHeading = isChapterHeader ||
                    (rawTrimmed.startsWith("#") && !rawTrimmed.startsWith("# ")) ||
                    (rawTrimmed.startsWith("# ") && rawTrimmed.length <= 80) ||
                    (!rawTrimmed.startsWith("- ") && !rawTrimmed.startsWith("*") && rawTrimmed.length <= 60 && (
                        rawTrimmed.startsWith("Chapter", true) ||
                        rawTrimmed.startsWith("Part", true) ||
                        rawTrimmed.startsWith("Section", true) ||
                        rawTrimmed.startsWith("Book", true)
                    ))

            val annotated = androidx.compose.ui.text.buildAnnotatedString {
                val regex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`|~~.*?~~|<u>.*?</u>)")
                var lastIndex = 0

                regex.findAll(processedParagraph).forEach { match ->
                    val start = match.range.first
                    val end = match.range.last + 1
                    if (start > lastIndex) {
                        append(processedParagraph.substring(lastIndex, start))
                    }
                    val matchedStr = match.value
                    when {
                        matchedStr.startsWith("**") && matchedStr.endsWith("**") -> {
                            withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(matchedStr.removeSurrounding("**"))
                            }
                        }
                        matchedStr.startsWith("*") && matchedStr.endsWith("*") -> {
                            withStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                                append(matchedStr.removeSurrounding("*"))
                            }
                        }
                        matchedStr.startsWith("~~") && matchedStr.endsWith("~~") -> {
                            withStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                                append(matchedStr.removeSurrounding("~~"))
                            }
                        }
                        matchedStr.startsWith("<u>") && matchedStr.endsWith("</u>") -> {
                            withStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                                append(matchedStr.removeSurrounding("<u>", "</u>"))
                            }
                        }
                        matchedStr.startsWith("`") && matchedStr.endsWith("`") -> {
                            withStyle(
                                androidx.compose.ui.text.SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = Color(0x33888888)
                                )
                            ) {
                                append(matchedStr.removeSurrounding("`"))
                            }
                        }
                        else -> append(matchedStr)
                    }
                    lastIndex = end
                }
                if (lastIndex < processedParagraph.length) {
                    append(processedParagraph.substring(lastIndex))
                }
            }

            ParsedParagraph(
                rawTrimmed = rawTrimmed,
                isHeading = isHeading,
                isBulletList = isBulletList,
                isChapterHeader = isChapterHeader,
                isImage = isImage && imageUrl.isNotBlank(),
                imageUrl = imageUrl,
                annotatedParagraph = annotated
            )
        }
    }

    val savedTargetPosition = remember(session.bookId) { session.lastReadPosition }
    val initialIndex = remember(paragraphs.size, session.bookId) {
        val target = if (session.lastReadPosition > 0) session.lastReadPosition else savedTargetPosition
        if (paragraphs.isNotEmpty() && target > 0) {
            target.toInt().coerceIn(0, paragraphs.size - 1)
        } else if (paragraphs.isNotEmpty() && session.progress > 0) {
            ((session.progress.toFloat() / 100f) * paragraphs.size).toInt()
                .coerceIn(0, paragraphs.size - 1)
        } else 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    var isInitialScrollRestored by remember(session.bookId) { mutableStateOf(false) }
    LaunchedEffect(paragraphs.size, initialIndex) {
        if (paragraphs.isNotEmpty() && !isInitialScrollRestored) {
            if (initialIndex > 0) {
                listState.scrollToItem(initialIndex)
            }
            isInitialScrollRestored = true
        }
    }
    val bookDao: BookDao = koinInject()

    LaunchedEffect(listState, isInitialScrollRestored) {
        if (!isInitialScrollRestored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }.collect { firstVisible ->
            if (!isInitialScrollRestored) return@collect
            val activeChapterIndex =
                chapterHeadingIndices.indexOfLast { it <= firstVisible }.coerceAtLeast(0)
            val total = paragraphs.size.coerceAtLeast(1)
            val calculatedProgress =
                ((firstVisible.toFloat() / total) * 100).toInt().coerceIn(0, 100)
            if (session.bookId.isNotBlank() && (session.currentChapter != activeChapterIndex || session.progress != calculatedProgress || session.lastReadPosition != firstVisible.toLong())) {
                ReaderSessionStore.update {
                    copy(
                        currentChapter = activeChapterIndex,
                        progress = calculatedProgress,
                        lastReadPosition = firstVisible.toLong()
                    )
                }
                LibraryStore.updateProgress(
                    bookId = session.bookId,
                    progress = calculatedProgress,
                    chapterIndex = activeChapterIndex,
                    lastReadPosition = firstVisible.toLong(),
                    dao = bookDao,
                    scope = scope
                )
            }
        }
    }

    val autoscrollSpeed = viewModel.autoscrollSpeed
    val autoscrollMode = viewModel.autoscrollMode

    LaunchedEffect(isAutoScrolling, isAutoMode, autoscrollSpeed, autoscrollMode, paragraphs.size) {
        while ((isAutoScrolling || isAutoMode) && paragraphs.isNotEmpty()) {
            val delayMs = ((autoscrollSpeed.coerceIn(0.5f, 10.0f)) * 1000).toLong()
            if (autoscrollMode == "Line by Line") {
                // Smooth continuous line-by-line scrolling
                val stepMs = 30L
                val totalSteps = (delayMs / stepMs).coerceAtLeast(1)
                val pixelsPerStep = 2f
                for (step in 0 until totalSteps) {
                    if (!viewModel.isAutoScrolling && !viewModel.isAutoMode) break
                    kotlinx.coroutines.delay(stepMs)
                    runCatching {
                        listState.scrollBy(pixelsPerStep)
                    }
                }
            } else {
                // Page by Page / Paragraph by Paragraph step scroll
                kotlinx.coroutines.delay(delayMs)
                if (!viewModel.isAutoScrolling && !viewModel.isAutoMode) break
                val current = listState.firstVisibleItemIndex
                val step = if (autoscrollMode == "Page by Page") {
                    val visibleCount = listState.layoutInfo.visibleItemsInfo.size
                    visibleCount.coerceAtLeast(1)
                } else 1

                if (current < paragraphs.size - 1) {
                    runCatching {
                        listState.animateScrollToItem((current + step).coerceAtMost(paragraphs.size - 1))
                    }
                } else {
                    viewModel.updateIsAutoScrolling(false)
                    viewModel.updateIsAutoMode(false)
                    break
                }
            }
        }
    }
    val contentTransitionSpec = if (viewModel.pagingAnimation) {
        when (viewModel.animationStyle) {
            "Slide" -> slideInHorizontally() + fadeIn()
            "Fade" -> fadeIn()
            else -> slideInHorizontally(initialOffsetX = { it }) + fadeIn()
        }
    } else {
        EnterTransition.None
    }
    val highlightColorObj = when (viewModel.highlightColorSelection) {
        "Yellow" -> Color.Yellow
        "Green" -> Color.Green
        "Cyan" -> Color.Cyan
        "Pink" -> Color.Magenta
        else -> Color(0xFFFFEB3B)
    }.copy(alpha = viewModel.highlightOpacity.coerceIn(0.1f, 1.0f))

    var showBrightnessPill by remember { mutableStateOf(false) }
    SetScreenBrightnessEffect(
        brightness = if (viewModel.systemScreenBrightness) -1f else (viewModel.brightnessValue.toFloat() / 100f),
        isAuto = viewModel.autoBrightness
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (viewModel.overrideBookColor) {
                    when (viewModel.activeBackgroundIndex) {
                        0 -> scheme.background
                        1 -> scheme.background
                        else -> when (viewModel.themeMode) {
                            "Light" -> scheme.background
                            "Dark" -> scheme.background
                            else -> if (viewModel.eInkMode) scheme.onBackground else themeBg
                        }
                    }
                }

                else {
                    when (viewModel.themeMode) {
                        "Light" -> scheme.background
                        "Dark" -> scheme.background
                        else -> if (viewModel.eInkMode) scheme.onBackground else themeBg
                    }
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = viewModel.maxColumnWidth.dp)
                .heightIn(max = viewModel.maxColumnHeight.dp)
                .onGloballyPositioned { readerBoxCoordinates = it }
                // Apply column gap configuration dynamically if multi-column layout is active
                .padding(horizontal = if (viewModel.maxColumns > 1) 8.dp else 0.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!isLocked) {
                                controlsVisible = !controlsVisible
                                showSelectionTray = false
                                selectedWord = null
                            }
                        }
                    )
                }
                .pointerInput(viewModel.swipeBrightness) {
                    var accumulatedDrag = 0f
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val triggerWidth = if (size.width > 0) size.width * 0.15f else 120f
                            if (offset.x <= triggerWidth && viewModel.swipeBrightness) {
                                showBrightnessPill = true
                                accumulatedDrag = 0f
                                if (viewModel.autoBrightness) {
                                    viewModel.updateAutoBrightness(false)
                                }
                            }
                        },
                        onDragEnd = { showBrightnessPill = false },
                        onDragCancel = { showBrightnessPill = false },
                        onVerticalDrag = { change, dragAmount ->
                            if (showBrightnessPill) {
                                change.consume()
                                accumulatedDrag -= dragAmount
                                val stepPx = 8f // 8px per 1% brightness
                                if (kotlin.math.abs(accumulatedDrag) >= stepPx) {
                                    val delta = (accumulatedDrag / stepPx).toInt()
                                    accumulatedDrag %= stepPx
                                    viewModel.updateBrightnessValue(
                                        (viewModel.brightnessValue + delta).coerceIn(0, 100)
                                    )
                                }
                            }
                        }
                    )
                }
        ) {
            if (isLoadingChunks && activeText.isEmpty()) {
                // Show loading indicator while chunks load from Room DB after app restart
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFFB300),
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Loading book...",
                            color = Color(0xFFAAAAAA),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            // Immersive Book Text View with column and animation parameters applied
            AnimatedVisibility(
                visible = !isLoadingChunks || activeText.isNotEmpty(),
                enter = contentTransitionSpec,
                exit = if (viewModel.pagingAnimation) fadeOut() else ExitTransition.None
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = (if (viewModel.useBookLayout) 12 else viewModel.leftMargin).dp,
                            end = (if (viewModel.useBookLayout) 12 else viewModel.rightMargin).dp,
                            top = (if (viewModel.showHeader) (40 + viewModel.topMargin).dp else viewModel.topMargin.dp),
                            bottom = (if (viewModel.showFooter) (40 + viewModel.bottomMargin).dp else viewModel.bottomMargin.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(if (viewModel.useBookLayout) 16.dp else (viewModel.defaultFontSize * viewModel.paragraphMargin).dp)
                ) {
                    items(parsedParagraphs.size) { index ->
                        val item = parsedParagraphs[index]
                        val rawTrimmed = item.rawTrimmed
                        if (rawTrimmed.isBlank()) return@items

                        // Image Rendering (PDF pages & EPUB rich illustrations)
                        if (item.isImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = if (viewModel.useBookLayout) 6.dp else 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    shadowElevation = if (viewModel.useBookLayout) 6.dp else 0.dp,
                                    color = if (viewModel.useBookLayout) Color(0xFFFFFFFF) else Color.Transparent
                                ) {
                                    coil3.compose.AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = "Document Illustration",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                            return@items
                        }

                        if (rawTrimmed.startsWith("```")) {
                            val codeContent = rawTrimmed.removeSurrounding("```").trim()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = viewModel.maxColumnWidth.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (viewModel.eInkMode || viewModel.colorEInkMode) Color.LightGray else Color(
                                            0xFF1E1E1E
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        if (viewModel.eInkMode || viewModel.colorEInkMode) Color.DarkGray else Color(
                                            0xFF333333
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (viewModel.enableCodeHighlighting) {
                                        Text(
                                            text = "Lang: ${viewModel.codeLanguageSelection} | Theme: ${viewModel.codeThemeSelection}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFE8883A),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = codeContent,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (viewModel.defaultFontSize * 0.9).sp,
                                        color = if (viewModel.eInkMode || viewModel.colorEInkMode) Color.Black else Color(
                                            0xFFE6E1E5
                                        )
                                    )
                                }
                            }
                            return@items
                        }

                        val isHeading = item.isHeading
                        val isBulletList = item.isBulletList

                        val displayFontSize =
                            if (isHeading) (viewModel.defaultFontSize * 1.4).sp else viewModel.defaultFontSize.sp
                        val displayFontWeight =
                            if (isHeading) FontWeight.Bold else FontWeight(viewModel.fontWeightVal)
                        val displayTextAlign =
                            if (isHeading) TextAlign.Center else (if (viewModel.fullJustification) TextAlign.Justify else TextAlign.Left)

                        val annotatedParagraph = item.annotatedParagraph

                        val isHighlighted =
                            bookmarksList.contains(rawTrimmed) || bookmarksList.any {
                                rawTrimmed.contains(
                                    it,
                                    ignoreCase = true
                                )
                            }

                        if (viewModel.searchQuery.isNotEmpty() && rawTrimmed.contains(
                                viewModel.searchQuery,
                                ignoreCase = true
                            )
                        ) {
                            val annotatedWithSearch =
                                androidx.compose.ui.text.buildAnnotatedString {
                                    val lowerParagraph = rawTrimmed.lowercase()
                                    val lowerQuery = viewModel.searchQuery.lowercase()
                                    var startIdx = 0
                                    while (true) {
                                        val foundIdx = lowerParagraph.indexOf(lowerQuery, startIdx)
                                        if (foundIdx == -1) {
                                            append(rawTrimmed.substring(startIdx))
                                            break
                                        }
                                        append(rawTrimmed.substring(startIdx, foundIdx))
                                        withStyle(
                                            androidx.compose.ui.text.SpanStyle(
                                                background = Color(0xFFFFD54F),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(
                                                rawTrimmed.substring(
                                                    foundIdx,
                                                    foundIdx + lowerQuery.length
                                                )
                                            )
                                        }
                                        startIdx = foundIdx + lowerQuery.length
                                    }
                                }
                            Text(
                                text = annotatedWithSearch,
                                color = if (viewModel.eInkMode) scheme.onBackground else themeText,
                                fontSize = displayFontSize,
                                lineHeight = (viewModel.defaultFontSize * viewModel.lineSpacing).sp,
                                textAlign = displayTextAlign,
                                fontFamily = selectedFont,
                                fontWeight = displayFontWeight,
                                onTextLayout = { layoutResults[index] = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = viewModel.maxColumnWidth.dp)
                                    .onGloballyPositioned {
                                        paragraphCoordinates[index] = it
                                    }
                            )
                        } else if (selectedParagraphIndex == index && selectedWord != null) {
                            val activeParaText = annotatedParagraph.text
                            val safeStart = selectedRangeStart.coerceIn(0, activeParaText.length)
                            val safeEnd = selectedRangeEnd.coerceIn(safeStart, activeParaText.length)

                            val annotatedWithSelection = androidx.compose.ui.text.buildAnnotatedString {
                                if (safeStart > 0) {
                                    append(activeParaText.substring(0, safeStart))
                                }
                                withStyle(
                                    androidx.compose.ui.text.SpanStyle(
                                        background = AmberWarm.copy(alpha = 0.35f),
                                        color = if (viewModel.eInkMode) scheme.onBackground else themeText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                ) {
                                    append(activeParaText.substring(safeStart, safeEnd))
                                }
                                if (safeEnd < activeParaText.length) {
                                    append(activeParaText.substring(safeEnd))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = viewModel.maxColumnWidth.dp)
                            ) {
                                Text(
                                    text = annotatedWithSelection,
                                    color = if (viewModel.eInkMode) scheme.onBackground else themeText,
                                    fontSize = displayFontSize,
                                    lineHeight = (viewModel.defaultFontSize * viewModel.lineSpacing).sp,
                                    textAlign = displayTextAlign,
                                    fontFamily = selectedFont,
                                    fontWeight = displayFontWeight,
                                    onTextLayout = { layoutResults[index] = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned {
                                            paragraphCoordinates[index] = it
                                        }
                                )

                                // Native Android-style Left & Right Drag Handle Pins
                                val layoutResult = layoutResults[index]
                                if (layoutResult != null && safeStart < safeEnd) {
                                    val startRect = runCatching { layoutResult.getBoundingBox(safeStart) }.getOrNull()
                                    val endRect = runCatching { layoutResult.getBoundingBox((safeEnd - 1).coerceAtLeast(0)) }.getOrNull()

                                    // Left Handle (Draggable start pin)
                                    if (startRect != null) {
                                        Box(
                                            modifier = Modifier
                                                .offset {
                                                    IntOffset(
                                                        x = (startRect.left - 16.dp.toPx()).toInt().coerceAtLeast(0),
                                                        y = startRect.bottom.toInt()
                                                    )
                                                }
                                                .size(32.dp)
                                                .pointerInput(selectedParagraphIndex) {
                                                    detectDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val targetPos = Offset(startRect.left + dragAmount.x, startRect.top + dragAmount.y)
                                                        val charOffset = layoutResult.getOffsetForPosition(targetPos)
                                                        val clampedStart = charOffset.coerceIn(0, (selectedRangeEnd - 1).coerceAtLeast(0))
                                                        if (clampedStart != selectedRangeStart) {
                                                            selectedRangeStart = clampedStart
                                                            val newText = activeParaText.substring(selectedRangeStart, selectedRangeEnd)
                                                            if (newText.isNotEmpty()) {
                                                                selectedWord = newText
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                                    .background(AmberWarm)
                                            )
                                        }
                                    }

                                    // Right Handle (Draggable end pin)
                                    if (endRect != null) {
                                        Box(
                                            modifier = Modifier
                                                .offset {
                                                    IntOffset(
                                                        x = (endRect.right - 2.dp.toPx()).toInt(),
                                                        y = endRect.bottom.toInt()
                                                    )
                                                }
                                                .size(32.dp)
                                                .pointerInput(selectedParagraphIndex) {
                                                    detectDragGestures { change, dragAmount ->
                                                        change.consume()
                                                        val targetPos = Offset(endRect.right + dragAmount.x, endRect.bottom + dragAmount.y)
                                                        val charOffset = layoutResult.getOffsetForPosition(targetPos)
                                                        val clampedEnd = charOffset.coerceIn((selectedRangeStart + 1).coerceAtMost(activeParaText.length), activeParaText.length)
                                                        if (clampedEnd != selectedRangeEnd) {
                                                            selectedRangeEnd = clampedEnd
                                                            val newText = activeParaText.substring(selectedRangeStart, selectedRangeEnd)
                                                            if (newText.isNotEmpty()) {
                                                                selectedWord = newText
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                                                    .background(AmberWarm)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (isHighlighted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = viewModel.maxColumnWidth.dp)
                                    .background(highlightColorObj, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = annotatedParagraph,
                                    color = if (viewModel.eInkMode) scheme.onBackground else themeText,
                                    fontSize = displayFontSize,
                                    lineHeight = (viewModel.defaultFontSize * viewModel.lineSpacing).sp,
                                    textAlign = displayTextAlign,
                                    fontFamily = selectedFont,
                                    fontWeight = displayFontWeight,
                                    onTextLayout = { layoutResults[index] = it },
                                    modifier = Modifier.onGloballyPositioned {
                                        paragraphCoordinates[index] = it
                                    }
                                )
                            }
                        } else {
                            if (isBulletList) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = viewModel.maxColumnWidth.dp)
                                        .padding(start = 16.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "• ",
                                        color = AmberWarm,
                                        fontSize = displayFontSize,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = annotatedParagraph,
                                        color = if (viewModel.eInkMode) scheme.onBackground else themeText,
                                        fontSize = displayFontSize,
                                        lineHeight = (viewModel.defaultFontSize * viewModel.lineSpacing).sp,
                                        textAlign = displayTextAlign,
                                        fontFamily = selectedFont,
                                        fontWeight = displayFontWeight,
                                        onTextLayout = { layoutResults[index] = it },
                                        modifier = Modifier.onGloballyPositioned {
                                            paragraphCoordinates[index] = it
                                        }
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = viewModel.maxColumnWidth.dp)
                                        .padding(horizontal = (if (viewModel.useBookLayout) 8 else 0).dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = annotatedParagraph,
                                        color = if (viewModel.eInkMode) scheme.onBackground else (if (isHeading) AmberWarm else themeText),
                                        fontSize = displayFontSize,
                                        lineHeight = (viewModel.defaultFontSize * viewModel.lineSpacing).sp,
                                        textAlign = displayTextAlign,
                                        fontFamily = selectedFont,
                                        fontWeight = displayFontWeight,
                                        letterSpacing = (viewModel.letterSpacing * 0.05).sp,
                                        style = if (isHeading) androidx.compose.ui.text.TextStyle(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                        ) else androidx.compose.ui.text.TextStyle.Default,
                                        onTextLayout = { layoutResults[index] = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { paragraphCoordinates[index] = it }
                                            .pointerInput(isHeading) {
                                                detectTapGestures(
                                                    onTap = {
                                                        if (isHeading) {
                                                            showTocSheet = true
                                                        } else if (!isLocked) {
                                                            controlsVisible = !controlsVisible
                                                            showSelectionTray = false
                                                            selectedWord = null
                                                        }
                                                    },
                                                    onLongPress = { offset ->
                                                        val layoutResult = layoutResults[index]
                                                        val coords = paragraphCoordinates[index]
                                                        selectedParagraphIndex = index
                                                        if (layoutResult != null && coords != null && readerBoxCoordinates != null) {
                                                            val charIndex = layoutResult.getOffsetForPosition(offset)
                                                            val text = annotatedParagraph.text
                                                            var start = charIndex.coerceIn(0, (text.length - 1).coerceAtLeast(0))
                                                            while (start > 0 && !text[start - 1].isWhitespace()) start--
                                                            var end = charIndex.coerceIn(0, (text.length - 1).coerceAtLeast(0))
                                                            while (end < text.length && !text[end].isWhitespace()) end++

                                                            selectedRangeStart = start
                                                            selectedRangeEnd = end
                                                            val word = if (start < end) text.substring(start, end).trim() else rawTrimmed.take(35)
                                                            if (word.isNotBlank()) {
                                                                selectedWord = word
                                                                showSelectionTray = true
                                                                val positionInBox = readerBoxCoordinates!!.localPositionOf(coords, offset)
                                                                selectionOffset = IntOffset(positionInBox.x.toInt(), positionInBox.y.toInt())
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reading Ruler Overlay
            if (viewModel.enableReadingRuler) {
                val rColor = when (viewModel.rulerColor) {
                    "Yellow" -> Color.Yellow
                    "Cyan" -> Color.Cyan
                    "Green" -> Color.Green
                    "Magenta" -> Color.Magenta
                    else -> Color(0xFFFFB74D)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((viewModel.defaultFontSize * 2.5 * viewModel.linesToHighlight).dp)
                        .align(Alignment.Center)
                        .background(
                            rColor.copy(
                                alpha = viewModel.rulerOpacity.coerceIn(
                                    0.1f,
                                    0.9f
                                )
                            )
                        )
                        .border(1.dp, rColor.copy(alpha = 0.8f))
                )
            }
            val manager = remember { PlatformStatusManager() }
            val statusData by manager.statusFlow.collectAsState()

            DisposableEffect(manager) {
                manager.startMonitoring()
                onDispose { manager.stopMonitoring() }
            }

            // 2. Select dynamic icons depending on framework targets
            val batteryIcon = remember(statusData.batteryPercentage, statusData.isCharging) {
                when {
                    statusData.isCharging -> Icons.Filled.BatteryChargingFull
                    statusData.batteryPercentage > 85 -> Icons.Filled.BatteryFull
                    statusData.batteryPercentage > 65 -> Icons.Filled.Battery6Bar
                    statusData.batteryPercentage > 45 -> Icons.Filled.Battery4Bar
                    statusData.batteryPercentage > 25 -> Icons.Filled.Battery2Bar
                    statusData.batteryPercentage > 15 -> Icons.Filled.Battery1Bar
                    else -> Icons.Filled.BatteryAlert
                }
            }
            // Persistent Header Overlay
            if (viewModel.showHeader) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // .height(36.dp)
                        .align(Alignment.TopCenter)
                        // .background((if (viewModel.eInkMode) scheme.onBackground else themeBg).copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (viewModel.showCurrentTime) {
                                Text(
                                    text = statusData.timeText, // ACTUAL TIME DATA
                                    fontSize = 12.sp,
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    )
                                )
                            }
                            if (viewModel.showReadingProgress) {
                                if (viewModel.showCurrentTime) Text(
                                    "•",
                                    fontSize = 10.sp,
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    )
                                )
                                Text(
                                    text = "${session.progress}% Read",
                                    fontSize = 12.sp,
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    )
                                )
                            }
                            if (!viewModel.showCurrentTime && !viewModel.showReadingProgress) {
                                Text(
                                    text = session.title.ifBlank { "Reader Document" },
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (viewModel.showBatteryStatus || viewModel.showBatteryPercentage) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewModel.showBatteryPercentage) {
                                    Text(
                                        text = "${statusData.batteryPercentage}%", // ACTUAL BATTERY PERCENTAGE
                                        fontSize = 12.sp,
                                        color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                            alpha = 0.6f
                                        )
                                    )
                                }
                                if (viewModel.showBatteryStatus) {
                                    Icon(
                                        imageVector = batteryIcon, // DYNAMIC SYSTEM ICON
                                        contentDescription = "Battery Status",
                                        tint = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                            alpha = 0.6f
                                        ),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Persistent Footer Overlay
            if (viewModel.showFooter) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // .height(32.dp)
                        .align(Alignment.BottomCenter)
                        // .background((if (viewModel.eInkMode) scheme.onBackground else themeBg).copy(alpha = 0.95f))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Leading Progress Text Component
                            if (viewModel.showRemainingTime) {
                                Text(
                                    text = "${((100 - session.progress) * 0.5).toInt()} m left",
                                    fontSize = 11.sp,
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    )
                                )
                            } else {
                                val progressLabel = when (viewModel.progressStyle) {
                                    "Percentage Read" -> "${session.progress}% Read"
                                    "Estimated Time Left" -> "${((100 - session.progress) * 0.5).toInt()} mins left"
                                    "Chapter Position" -> "Chapter ${session.currentChapter + 1} of ${
                                        chapters.size.coerceAtLeast(
                                            1
                                        )
                                    }"

                                    else -> "Page ${(session.progress * 2).coerceAtLeast(1)} • ${session.progress}%"
                                }
                                Text(
                                    text = progressLabel,
                                    fontSize = 11.sp,
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    ),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // 2. FIXED: Central Progress Bar Component (Uses weight to share space)
                            if (viewModel.showProgressBar) {
                                LinearProgressIndicator(
                                    progress = { session.progress / 100f },
                                    modifier = Modifier
                                        .weight(1f) // Key Fix: Allows text to sit safely on either side
                                        .padding(horizontal = 12.dp) // Prevents progress bar from squishing text
                                        .height(2.dp)
                                        .clip(CircleShape),
                                    color = AmberWarm,
                                    trackColor = Color.DarkGray
                                )
                            }

                            // 3. Trailing Pages Left Component
                            if (viewModel.showRemainingPages) {
                                Text(
                                    text = "${((100 - session.progress) * 0.12).toInt()} pages left",
                                    fontSize = 11.sp,
                                    color = (if (viewModel.eInkMode) scheme.onBackground else themeText).copy(
                                        alpha = 0.6f
                                    )
                                )
                            }
                        }
                    }
                }
            }


            // Selection Tray overlay
            if (showSelectionTray && !isLocked) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = selectionOffset.copy(y = (selectionOffset.y - 140).coerceAtLeast(10)),
                    onDismissRequest = { 
                        showSelectionTray = false 
                        selectedWord = null
                    },
                    properties = PopupProperties(focusable = false)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202020).copy(alpha = 0.95f))
                            .border(
                                1.dp,
                                AmberWarm.copy(alpha = 0.6f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Select Whole Paragraph
                        IconButton(onClick = {
                            val activePara = parsedParagraphs.getOrNull(selectedParagraphIndex)?.annotatedParagraph?.text ?: ""
                            if (activePara.isNotBlank()) {
                                selectedRangeStart = 0
                                selectedRangeEnd = activePara.length
                                selectedWord = activePara
                            }
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SelectAll, "Select All", tint = scheme.primary, modifier = Modifier.size(16.dp))
                        }

                        IconButton(onClick = {
                            selectedWord?.let {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                            }
                            showSelectionTray = false
                            selectedWord = null
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.ContentCopy,
                                "Copy",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        var showColorPickerInTray by remember { mutableStateOf(false) }
                        if (showColorPickerInTray) {
                            val highlightColors = listOf(
                                Color(0xFFFFE082) to "#FFE082", // Solar Yellow
                                Color(0xFFFFB74D) to "#FFB74D", // Warm Amber
                                Color(0xFF81C784) to "#81C784", // Mint Green
                                Color(0xFFE57373) to "#E57373", // Rose Coral
                                Color(0xFF64B5F6) to "#64B5F6", // Sky Blue
                                Color(0xFFBA68C8) to "#BA68C8"  // Lavender Purple
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                highlightColors.forEach { (col, hex) ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .clickable {
                                                val wordToHighlight = selectedWord ?: "Snippet"
                                                viewModel.saveHighlight(
                                                    bookId = session.bookId,
                                                    text = wordToHighlight,
                                                    colorHex = hex,
                                                    pageProgress = session.progress,
                                                    chapterIndex = session.currentChapter
                                                )
                                                showSelectionTray = false
                                                selectedWord = null
                                                showColorPickerInTray = false
                                            }
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = {
                                showColorPickerInTray = true
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Default.Brush,
                                    "Highlight",
                                    tint = AmberWarm,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(onClick = {
                            noteInputText = selectedWord ?: ""
                            showAddNoteDialog = true
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                "Note",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            showVoiceNoteCapture = true
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Mic,
                                "Voice Note",
                                tint = scheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            selectedWord?.let {
                                viewModel.updateSearchQuery(it, paragraphs)
                                isSearchActive = true
                            }
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Search,
                                "Search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            aiCompanionContextText = selectedWord ?: ""
                            aiSelectedScope = "Selection"
                            aiAnalysisResult = null
                            showAiCompanionDialog = true
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                "AI Analyze",
                                tint = AmberWarm,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            translationSourceText = selectedWord ?: ""
                            showTranslationDialog = true
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Translate,
                                "Translate",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            showTtsQuickConfig = true
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                "TTS",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = {
                            showSettingsSheet = true
                            showSelectionTray = false
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.Build,
                                "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

// Floating Brightness Indicator HUD Overlay
            AnimatedVisibility(
                visible = showBrightnessPill,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1E24).copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, AmberWarm.copy(alpha = 0.5f)),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.brightnessValue < 30) Icons.Default.BrightnessLow else if (viewModel.brightnessValue < 70) Icons.Default.BrightnessMedium else Icons.Default.BrightnessHigh,
                            contentDescription = "Brightness",
                            tint = AmberWarm,
                            modifier = Modifier.size(28.dp)
                        )

                        Text(
                            text = "${viewModel.brightnessValue}%",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )

                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF33333E))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(viewModel.brightnessValue / 100f)
                                    .background(AmberWarm)
                            )
                        }
                    }
                }
            }
            if (showTtsQuickConfig) {
                TtsQuickConfigDialog(
                    onDismiss = { showTtsQuickConfig = false },
                    viewModel = viewModel,
                    onStartPlayback = {
                        showTtsQuickConfig = false
                        if (selectedWord != null) {
                            viewModel.speakText(selectedWord!!, mediaActions)
                        } else {
                            viewModel.speakParagraph(listState.firstVisibleItemIndex, mediaActions)
                        }
                    }
                )
            }

            if (isLocked && !controlsVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.End // Forces content to the far right
                ) {
                    Button(
                        onClick = {
                            isLocked = false
                            controlsVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.12f
                            )
                        )
                    ) {
                        Icon(Icons.Outlined.Lock, null, tint = scheme.primary)
                    }
                }
            }



// 1. Transparent Glassmorphic Top Bar Overlay Control
            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF161616).copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isSearchActive) Arrangement.Start else Arrangement.SpaceBetween
                ) {
                    if (isSearchActive) {
                        IconButton(
                            onClick = {
                                isSearchActive = false
                                viewModel.clearSearch()
                            },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = scheme.primary)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = viewModel.searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it, paragraphs) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search in book...", color = scheme.onSurface.copy(alpha = 0.6f)) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                cursorColor = AmberWarm,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearSearch() },
                                modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "Clear", tint = scheme.primary)
                            }
                        }
                    } else {
                        // 1. Back button
                        IconButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = scheme.primary)
                        }

                        // 2. Bookmark toggle button
                        val currentBookmarkEntry = "Page ${session.progress}% - ${session.title}"
                        val isBookmarked = bookmarksList.contains(currentBookmarkEntry)
                        IconButton(
                            onClick = {
                                if (isBookmarked) {
                                    viewModel.deleteBookmarkByTitle(session.bookId, currentBookmarkEntry)
                                } else {
                                    viewModel.addBookmark(
                                        bookId = session.bookId,
                                        title = currentBookmarkEntry,
                                        pageProgress = session.progress,
                                        chapterIndex = session.currentChapter
                                    )
                                }
                            },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                "Bookmark",
                                tint = scheme.primary
                            )
                        }

                        // 3. Translation button
                        IconButton(
                            onClick = {
                                translationSourceText = selectedWord ?: paragraphs.getOrNull(listState.firstVisibleItemIndex) ?: session.title
                                showTranslationDialog = true
                            },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.Default.Translate, "Translation", tint = scheme.primary)
                        }

                        // 4. AutoScroll button
                        IconButton(
                            onClick = { viewModel.updateIsAutoScrolling(!isAutoScrolling) },
                            modifier = Modifier.size(38.dp).background(if (isAutoScrolling) AmberWarm.copy(alpha = 0.3f) else Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.Default.SwapVert, "AutoScroll", tint = if (isAutoScrolling) AmberWarm else scheme.primary)
                        }

                        // 5. Search button
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.Default.Search, "Search", tint = scheme.primary)
                        }

                        // 6. Font & Layout button
                        IconButton(
                            onClick = { showQuickFontSheet = true },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.Default.TextFields, "Font & Layout", tint = scheme.primary)
                        }

                        // 7. Settings button
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, "Settings", tint = scheme.primary)
                        }

                        // 8. Menu / Overflow button
                        Box {
                            IconButton(
                                onClick = { showMenu = !showMenu },
                                modifier = Modifier.size(38.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                            ) {
                                Icon(Icons.Default.Menu, "Menu", tint = scheme.primary)
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Font & Layout", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        showQuickFontSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Table of Contents", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        showTocSheet = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isAutoScrolling) "Stop Auto-Scroll" else "Start Auto-Scroll", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        viewModel.updateIsAutoScrolling(!isAutoScrolling)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Original PDF Layout", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = if (session.format.equals("PDF", ignoreCase = true) && !viewModel.useBookLayout) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateUseBookLayout(!viewModel.useBookLayout)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Scrolled Mode", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = if (isScrolledMode) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateIsScrolledMode(!isScrolledMode)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Auto Scroll", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = if (isAutoScrolling) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateIsAutoScrolling(!isAutoScrolling)
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF333333))
                                DropdownMenuItem(
                                    text = { Text("Paragraph Mode", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = if (isParagraphMode) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateIsParagraphMode(!isParagraphMode)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Speed Reading Mode", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = if (isSpeedReading) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateIsSpeedReading(!isSpeedReading)
                                        showMenu = false
                                    }
                                )

                                HorizontalDivider(color = Color(0xFF333333))
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.SettingsSuggest,
                                                null,
                                                tint = scheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "Auto Mode",
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    trailingIcon = if (isAutoMode) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateIsAutoMode(!isAutoMode)
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("AI Book Companion (Local LLM)", color = AmberWarm) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        aiCompanionContextText = paragraphs.take(15).joinToString("\n\n")
                                        aiSelectedScope = "Chapter"
                                        aiAnalysisResult = null
                                        showAiCompanionDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (viewModel.isBookTranslated) "Revert to Original Language" else "Translate Full Book...",
                                            color = AmberWarm
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (viewModel.isBookTranslated) Icons.Default.Restore else Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (viewModel.isBookTranslated) {
                                            viewModel.revertToOriginalBookText()
                                        } else {
                                            translationSourceText = paragraphs.take(5).joinToString("\n\n")
                                            showTranslationDialog = true
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Invert Image In Dark Mode", color = MaterialTheme.colorScheme.onSurface) },
                                    trailingIcon = if (viewModel.invertImageDark) {
                                        { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.updateInvertImageDark(!viewModel.invertImageDark)
                                        showMenu = false
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF333333))
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                null,
                                                tint = scheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Share Book Info", color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        val shareText = "I'm reading '${session.title}' by ${session.author} (${session.progress}% completed) on OmniMind Reader!"
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(shareText))
                                    }
                                )
                            }
                        }
                    }
                }
            }

// 2. Transparent Glassmorphic Bottom Bar Overlay Control
            AnimatedVisibility(
                visible = controlsVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF161616).copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { showTocSheet = true },
                        modifier = Modifier.size(40.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, "TOC", tint = scheme.primary)
                    }
                    IconButton(
                        onClick = { showBrightnessSheet = true },
                        modifier = Modifier.size(40.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(Icons.Default.LightMode, "Brightness", tint = scheme.primary)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF242424).copy(alpha = 0.85f))
                            .clickable { showQuickSeekSheet = !showQuickSeekSheet }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${session.progress}%",
                            color = AmberWarm,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            isLocked = true
                            controlsVisible = false
                        },
                        modifier = Modifier.size(40.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.LockOpen, "Lock Controls", tint = scheme.primary)
                    }
                    IconButton(
                        onClick = { showTtsQuickConfig = true },
                        modifier = Modifier.size(40.dp).background(Color(0xFF242424).copy(alpha = 0.85f), CircleShape)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, "TTS", tint = scheme.primary)
                    }
                }
            }

// 3. Translation Active Banner Overlay
            if (isTranslationActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = if (controlsVisible) 60.dp else 0.dp) // Auto offset below top bar if visible
                        .fillMaxWidth()
                        .background(Color(0xFF2D3E50).copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Translation Active: $activeTranslationService ($translationTarget)${if (isWordLensActive) " • Word Lens On" else ""}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

// 4. Search Navigation Overlay
            if (isSearchActive && viewModel.searchResults.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${viewModel.currentMatchIndex + 1} / ${viewModel.searchResults.size}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        FloatingActionButton(
                            onClick = {
                                viewModel.goToPreviousMatch()
                                val targetIdx = viewModel.searchResults[viewModel.currentMatchIndex]
                                scope.launch { listState.animateScrollToItem(targetIdx) }
                            },
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, "Previous", tint = MaterialTheme.colorScheme.primary)
                        }
                        FloatingActionButton(
                            onClick = {
                                viewModel.goToNextMatch()
                                val targetIdx = viewModel.searchResults[viewModel.currentMatchIndex]
                                scope.launch { listState.animateScrollToItem(targetIdx) }
                            },
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, "Next", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

// 5. Sheets & Dialogs (Outside structural visibility animations)
            QuickFontSheet(
                showQuickFontSheet = showQuickFontSheet,
                onDismiss = { showQuickFontSheet = false },
                defaultFontSize = viewModel.defaultFontSize,
                onFontSizeChange = { viewModel.updateDefaultFontSize(it) },
                lineSpacing = viewModel.lineSpacing,
                onLineSpacingChange = { viewModel.updateLineSpacing(it) },
                paragraphMargin = viewModel.paragraphMargin,
                onParagraphMarginChange = { viewModel.updateParagraphMargin(it) },
                letterSpacing = viewModel.letterSpacing,
                onLetterSpacingChange = { viewModel.updateLetterSpacing(it) }
            )

            QuickSeekSheet(
                showQuickSeekSheet = showQuickSeekSheet,
                onDismiss = { showQuickSeekSheet = false },
                session = session,
                onProgressChange = { newProgress ->
                    ReaderSessionStore.update { copy(progress = newProgress) }
                    val targetIdx = ((newProgress.toFloat() / 100f) * paragraphs.size).toInt()
                        .coerceIn(0, paragraphs.size - 1)
                    scope.launch { listState.scrollToItem(targetIdx) }
                    viewModel.updateProgress(session.bookId, newProgress, session.currentChapter, targetIdx.toLong())
                },
                onChapterChange = { newChapter ->
                    ReaderSessionStore.update { copy(currentChapter = newChapter) }
                    val targetParagraphIndex = chapterHeadingIndices.getOrNull(newChapter) ?: 0
                    scope.launch { listState.scrollToItem(targetParagraphIndex) }
                },
                onSeekToParagraph = { targetIdx ->
                    scope.launch { listState.scrollToItem(targetIdx) }
                }
            )

            TocSheet(
                showTocSheet = showTocSheet,
                onDismiss = { showTocSheet = false },
                showBookDetailsSheet = { showBookDetailsSheet = true },
                selectedTocTab = selectedTocTab,
                onTabChange = { selectedTocTab = it },
                bookmarksList = bookmarksList,
                onRemoveBookmark = { bookmark ->
                    viewModel.deleteBookmarkByTitle(session.bookId, bookmark)
                },
                onAddBookmark = {
                    viewModel.addBookmark(
                        bookId = session.bookId,
                        title = "Page ${session.progress}% - ${session.title}",
                        pageProgress = session.progress,
                        chapterIndex = session.currentChapter
                    )
                },
                notesList = notesList,
                onRemoveNote = { note ->
                    val titlePrefix = note.substringBefore(":")
                    viewModel.deleteBookmarkByTitle(session.bookId, titlePrefix)
                },
                onAddNote = {
                    noteInputText = ""
                    showAddNoteDialog = true
                },
                onSelectBookmark = { targetText ->
                    val cleanTarget = targetText.substringBefore(":")
                    val targetIdx = paragraphs.indexOfFirst {
                        it.contains(cleanTarget.take(15), ignoreCase = true)
                    }.coerceAtLeast(0)
                    scope.launch {
                        listState.animateScrollToItem(targetIdx)
                    }
                },
                chapters = chapters,
                session = session,
                onChapterSelect = { index ->
                    ReaderSessionStore.update { copy(currentChapter = index) }
                    val targetParagraphIndex = chapterHeadingIndices.getOrNull(index) ?: 0
                    scope.launch {
                        listState.scrollToItem(targetParagraphIndex)
                    }
                }
            )

            if (showAddNoteDialog) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { showAddNoteDialog = false }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Add Note",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            OutlinedTextField(
                                value = noteInputText,
                                onValueChange = { noteInputText = it },
                                label = { Text("Note content") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AmberWarm,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAddNoteDialog = false }) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (noteInputText.isNotBlank()) {
                                            viewModel.addBookmark(
                                                bookId = session.bookId,
                                                title = "Note in Ch. ${session.currentChapter + 1}",
                                                pageProgress = session.progress,
                                                chapterIndex = session.currentChapter,
                                                noteText = noteInputText
                                            )
                                            showAddNoteDialog = false
                                            noteInputText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarm)
                                ) {
                                    Text("Save Note", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            BookReaderSettingsSheet(
                showSettingsSheet = showSettingsSheet,
                onDismiss = { showSettingsSheet = false },
                selectedSheetTab = selectedSheetTab,
                onSheetTabChange = { selectedSheetTab = it },
                viewModel = viewModel
            )

            BookDetailsSheet(
                showBookDetailsSheet = showBookDetailsSheet,
                onDismiss = { showBookDetailsSheet = false },
                session = session
            )

            BrightnessSheet(
                showBrightnessSheet = showBrightnessSheet,
                onDismiss = { showBrightnessSheet = false },
                brightnessValue = viewModel.brightnessValue,
                onBrightnessChange = { viewModel.updateBrightnessValue(it) },
                activeColorTheme = viewModel.activeColorTheme,
                onColorThemeChange = { viewModel.updateActiveColorTheme(it) },
                autoBrightness = viewModel.autoBrightness,
                onAutoBrightnessChange = { viewModel.updateAutoBrightness(it) }
            )

            if (showAiCompanionDialog) {
                BookReaderAiCompanionDialog(
                    onDismiss = { showAiCompanionDialog = false },
                    contextText = aiCompanionContextText,
                    scope = aiSelectedScope,
                    onScopeChange = { newScope ->
                        aiSelectedScope = newScope
                        aiCompanionContextText = when (newScope) {
                            "Selection" -> selectedWord ?: paragraphs.firstOrNull() ?: session.title
                            "Chapter" -> paragraphs.take(15).joinToString("\n\n")
                            else -> paragraphs.take(30).joinToString("\n\n")
                        }
                    },
                    customPrompt = aiCustomPromptInput,
                    onCustomPromptChange = { aiCustomPromptInput = it },
                    selectedMode = aiSelectedMode,
                    onModeChange = { aiSelectedMode = it },
                    analysisResult = aiAnalysisResult,
                    isAnalyzing = isAiAnalyzing,
                    onRunAnalysis = {
                        isAiAnalyzing = true
                        scope.launch {
                            val result = when (aiSelectedMode) {
                                "Custom Prompt" -> {
                                    val prompt = aiCustomPromptInput.ifBlank { "Summarize and extract key insights from this text." }
                                    viewModel.bookAnalyzer.analyzeWithCustomPrompt(aiCompanionContextText, prompt)
                                }
                                "Fantasy Powers & Abilities" -> {
                                    viewModel.bookAnalyzer.analyzeSelection(aiCompanionContextText, "Character Powers")
                                }
                                "Chess Tactics & FEN" -> {
                                    viewModel.bookAnalyzer.analyzeSelection(aiCompanionContextText, "Chess Tactics")
                                }
                                "Self-Help Framework" -> {
                                    viewModel.bookAnalyzer.analyzeSelection(aiCompanionContextText, "Actionable Takeaways")
                                }
                                "Simplify / Explain" -> {
                                    viewModel.bookAnalyzer.analyzeSelection(aiCompanionContextText, "Simplify / Explain")
                                }
                                else -> {
                                    viewModel.bookAnalyzer.analyzeWithCustomPrompt(aiCompanionContextText, aiCustomPromptInput)
                                }
                            }
                            aiAnalysisResult = result
                            isAiAnalyzing = false
                        }
                    },
                    onOpenChessBoardWithFen = { fen ->
                        showAiCompanionDialog = false
                        navController?.navigate(Screen.Chess.route)
                    }
                )
            }

            if (showVoiceNoteCapture) {
                VoiceNoteCaptureDialog(
                    onDismiss = { showVoiceNoteCapture = false },
                    onSave = { pcm: FloatArray ->
                        viewModel.saveVoiceNote(
                            pcmData = pcm,
                            bookId = session.bookId,
                            progress = session.progress,
                            chapter = session.currentChapter
                        )
                        showVoiceNoteCapture = false
                    },
                    mediaActions = mediaActions
                )
            }
// PDF Decryption Password Dialog
            var showPasswordDialog by remember(session.isPasswordRequired) {
                mutableStateOf(session.isPasswordRequired)
            }
            if (showPasswordDialog) {
                var pdfPasswordInput by remember { mutableStateOf("") }
                Dialog(onDismissRequest = { showPasswordDialog = false }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF18181C)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    null,
                                    tint = Color(0xFFE8883A),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    "PDF Password Required",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "This PDF document is encrypted. Please enter the password to decrypt and view full contents.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = pdfPasswordInput,
                                onValueChange = { pdfPasswordInput = it },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Enter PDF Password...",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFE8883A),
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPasswordDialog = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        if (pdfPasswordInput.isNotBlank()) {
                                            viewModel.reimportWithPassword(
                                                id = session.bookId,
                                                path = session.sourcePath,
                                                pass = pdfPasswordInput
                                            )
                                            showPasswordDialog = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE8883A)
                                    )
                                ) {
                                    Text(
                                        "Decrypt",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            TranslationDialog(
                show = showTranslationDialog,
                sourceText = translationSourceText,
                translatedText = viewModel.activeTranslationResult,
                isTranslating = viewModel.isTranslating,
                onDismiss = { showTranslationDialog = false },
                onTranslate = { targetLang ->
                    viewModel.translateText(translationSourceText, targetLang)
                },
                onCopy = { copied ->
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copied))
                }
            )
        }
    }
}

@Composable
fun VoiceNoteCaptureDialog(
    onDismiss: () -> Unit,
    onSave: (FloatArray) -> Unit,
    mediaActions: MediaActions
) {
    var isRecording by remember { mutableStateOf(false) }
    var recordedPcm by remember { mutableStateOf<FloatArray?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        com.creativeblisslabs.omnimind.core.ui.components.GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Record Voice Note",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .clickable {
                            if (isRecording) {
                                mediaActions.stopPcmRecording { pcm ->
                                    recordedPcm = pcm
                                    isRecording = false
                                }
                            } else {
                                recordedPcm = null
                                mediaActions.startPcmRecording()
                                isRecording = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop" else "Record",
                        tint = if (isRecording) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRecording) "Recording..." else if (recordedPcm != null) "Recording captured" else "Tap to start recording",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { recordedPcm?.let { onSave(it) } },
                        enabled = recordedPcm != null && !isRecording,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Note")
                    }
                }
            }
        }
    }
}

@Composable
fun TtsQuickConfigDialog(
    onDismiss: () -> Unit,
    viewModel: ReaderHubViewModel,
    onStartPlayback: () -> Unit
) {
    val AmberWarm = Color(0xFFE8883A)
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "TTS Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                TtsSettingSlider(
                    label = "Speed",
                    value = viewModel.ttsSpeed,
                    onValueChange = { viewModel.updateTtsSpeed(it) },
                    valueRange = 0.5f..2.5f
                )

                TtsSettingSlider(
                    label = "Pitch",
                    value = viewModel.ttsPitch,
                    onValueChange = { viewModel.updateTtsPitch(it) },
                    valueRange = 0.5f..2.0f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onStartPlayback,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarm)
                    ) {
                        Text("Start Reading")
                    }
                }
            }
        }
    }
}

@Composable
fun TtsSettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    val AmberWarm = Color(0xFFE8883A)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text("${(value * 10).toInt() / 10.0}x", color = AmberWarm, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AmberWarm,
                activeTrackColor = AmberWarm,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
fun BookReaderAiCompanionDialog(
    onDismiss: () -> Unit,
    contextText: String,
    scope: String,
    onScopeChange: (String) -> Unit,
    customPrompt: String,
    onCustomPromptChange: (String) -> Unit,
    selectedMode: String,
    onModeChange: (String) -> Unit,
    analysisResult: String?,
    isAnalyzing: Boolean,
    onRunAnalysis: () -> Unit,
    onOpenChessBoardWithFen: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val AmberWarm = Color(0xFFE8883A)
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val fenRegex = remember {
        Regex("""([rnbqkpRNBQKP1-8]{1,8}/){7}[rnbqkpRNBQKP1-8]{1,8}\s+[wb]\s+([KQkq\-]+)\s+([a-h][36]|\-)\s+\d+\s+\d+""")
    }
    val detectedFen = remember(analysisResult, contextText) {
        val inResult = analysisResult?.let { fenRegex.find(it)?.value }
        inResult ?: fenRegex.find(contextText)?.value
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF16161A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = AmberWarm, modifier = Modifier.size(22.dp))
                        Text(
                            "AI Book Companion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEDEDED)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }

                // Scope Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Selection", "Chapter", "Full Book").forEach { s ->
                        FilterChip(
                            selected = scope == s,
                            onClick = { onScopeChange(s) },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberWarm.copy(alpha = 0.25f),
                                selectedLabelColor = AmberWarm
                            )
                        )
                    }
                }

                // Preset Modes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Custom Prompt",
                        "Fantasy Powers & Abilities",
                        "Chess Tactics & FEN",
                        "Self-Help Framework",
                        "Simplify / Explain"
                    ).forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { onModeChange(mode) },
                            label = { Text(mode, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberWarm,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                // Custom Prompt Input
                if (selectedMode == "Custom Prompt") {
                    OutlinedTextField(
                        value = customPrompt,
                        onValueChange = onCustomPromptChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Ask anything about this chapter, characters, or tactics...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberWarm,
                            unfocusedBorderColor = Color(0xFF333333),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 3
                    )

                    // Quick Prompt Suggestion Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Summarize in 3 bullets",
                            "What are the character powers?",
                            "Extract all FENs & tactics",
                            "Actionable habit checklist"
                        ).forEach { suggestion ->
                            AssistChip(
                                onClick = { onCustomPromptChange(suggestion) },
                                label = { Text(suggestion, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Action Button
                Button(
                    onClick = onRunAnalysis,
                    enabled = !isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyzing on Local LLM...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Run AI Analysis", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Detected FEN Position Button
                if (!detectedFen.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E261E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("♟️ Chess Position Detected", fontWeight = FontWeight.Bold, color = Color(0xFF81C784), fontSize = 12.sp)
                                Text(detectedFen.take(35) + "...", color = Color.Gray, fontSize = 10.sp)
                            }
                            Button(
                                onClick = { onOpenChessBoardWithFen(detectedFen) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Play on Board", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Analysis Result Area
                if (analysisResult != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF101014)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Analysis Result", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberWarm)
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(analysisResult))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, "Copy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFF282828), modifier = Modifier.padding(vertical = 4.dp))
                            SelectionContainer {
                                Text(
                                    text = analysisResult,
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
