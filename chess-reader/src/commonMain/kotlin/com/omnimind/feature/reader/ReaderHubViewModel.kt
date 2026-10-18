package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.material3.MaterialTheme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creativeblisslabs.omnimind.core.data.database.dao.BookDao
import com.creativeblisslabs.omnimind.core.data.database.dao.BookChunkDao
import com.creativeblisslabs.omnimind.core.data.database.entity.BookChunkEntity
import com.creativeblisslabs.omnimind.core.media.MediaProcessor
import com.creativeblisslabs.omnimind.core.media.MediaAttachmentUi
import com.creativeblisslabs.omnimind.feature.reader.engine.BookAnalyzer
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.snapshots.SnapshotStateList

import com.creativeblisslabs.omnimind.core.data.database.dao.BookmarkDao
import com.creativeblisslabs.omnimind.core.data.database.entity.BookmarkEntity
import com.creativeblisslabs.omnimind.core.data.ai.OnDeviceAiRuntime
import com.creativeblisslabs.omnimind.core.media.MediaActions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.Clock

class ReaderHubViewModel(
    val bookDao: BookDao,
    val bookChunkDao: BookChunkDao,
    val bookmarkDao: BookmarkDao,
    val onDeviceAiRuntime: OnDeviceAiRuntime,
    val mediaProcessor: MediaProcessor,
    val bookAnalyzer: BookAnalyzer,
    val appPreferences: AppPreferences
) : ViewModel() {

    var isImportingBook: Boolean by mutableStateOf(false)
        private set
    var importBookName: String by mutableStateOf("")
        private set
    var importProgress: Float by mutableStateOf(0f)
        private set

    var overrideBookFont: Boolean by mutableStateOf(false)
    var defaultFontSize: Int by mutableStateOf(18)
    var minFontSize: Int by mutableStateOf(12)
    var fontWeightVal: Int by mutableStateOf(400)
    var fontFaceSerif: String by mutableStateOf("Charter")
    var fontFaceSans: String by mutableStateOf("Inter")
    var fontFaceMono: String by mutableStateOf("Iosevka")

    var overrideBookLayout: Boolean by mutableStateOf(false)
    var useBookLayout: Boolean by mutableStateOf(false)
    var paragraphMargin: Float by mutableStateOf(1.0f)
    var lineSpacing: Float by mutableStateOf(1.4f)
    var wordSpacing: Int by mutableStateOf(0)
    var letterSpacing: Int by mutableStateOf(0)
    var textIndent: Int by mutableStateOf(24)
    var fullJustification: Boolean by mutableStateOf(true)
    var hyphenation: Boolean by mutableStateOf(true)

    var topMargin: Int by mutableStateOf(40)
    var bottomMargin: Int by mutableStateOf(40)
    var leftMargin: Int by mutableStateOf(32)
    var rightMargin: Int by mutableStateOf(32)

    var showHeader: Boolean by mutableStateOf(true)
    var showFooter: Boolean by mutableStateOf(true)
    var showRemainingTime: Boolean by mutableStateOf(true)
    var showRemainingPages: Boolean by mutableStateOf(true)
    var showReadingProgress: Boolean by mutableStateOf(true)
    var progressStyle: String by mutableStateOf("Percentage")
    var showProgressBar: Boolean by mutableStateOf(true)
    var showCurrentTime: Boolean by mutableStateOf(true)
    var showBatteryStatus: Boolean by mutableStateOf(true)
    var showBatteryPercentage: Boolean by mutableStateOf(true)
    var orientationSelected: String by mutableStateOf("Auto")
    var writingMode: String by mutableStateOf("Horizontal")

    var invertImageDark: Boolean by mutableStateOf(true)
    var activeColorTheme: String by mutableStateOf("White")
    var eInkMode: Boolean by mutableStateOf(false)
    var enableReadingRuler: Boolean by mutableStateOf(false)
    var linesToHighlight: Int by mutableStateOf(3)
    var rulerColor: String by mutableStateOf("#FFCC00")
    var rulerOpacity: Float by mutableStateOf(0.3f)
    var enableCodeHighlighting: Boolean by mutableStateOf(true)
    var codeLanguageSelection: String by mutableStateOf("Auto")

    var swipeBrightness: Boolean by mutableStateOf(true)
    var keepScreenAwake: Boolean by mutableStateOf(true)

    var codeThemeSelection: String by mutableStateOf("GitHub Dark")
    var colorEInkMode: Boolean by mutableStateOf(false)
    var customContentCss: String by mutableStateOf("")
    var maxColumnWidth: Int by mutableStateOf(800)
    var maxColumnHeight: Int by mutableStateOf(1200)
    var columnGap: Int by mutableStateOf(48)
    var themeMode: String by mutableStateOf("System")
    var overrideBookColor: Boolean by mutableStateOf(false)
    var activeBackgroundIndex: Int by mutableStateOf(0)
    var highlightColorSelection: String by mutableStateOf("#FFFF00")
    var highlightOpacity: Float by mutableStateOf(0.4f)
    var maxColumns: Int by mutableStateOf(2)
    var systemScreenBrightness: Boolean by mutableStateOf(true)
    var brightnessValue: Int by mutableStateOf(100)
    var autoBrightness: Boolean by mutableStateOf(true)
    
    var isScrolledMode: Boolean by mutableStateOf(false)
    var isAutoScrolling: Boolean by mutableStateOf(false)
    var autoscrollSpeed: Float by mutableStateOf(2.0f)
    var autoscrollMode: String by mutableStateOf("Line by Line")
    var isParagraphMode: Boolean by mutableStateOf(false)
    var isSpeedReading: Boolean by mutableStateOf(false)
    var isAutoMode: Boolean by mutableStateOf(false)

    var pagingAnimation: Boolean by mutableStateOf(true)
    var animationStyle: String by mutableStateOf("Slide")

    var allowJavaScript: Boolean by mutableStateOf(false)
    var wordLensEnabled: Boolean by mutableStateOf(false)
    var enableTranslation: Boolean by mutableStateOf(false)
    var showSourceText: Boolean by mutableStateOf(true)
    var ttsTextOption: String by mutableStateOf("Selection")
    var translationService: String by mutableStateOf("Google")
    var translateToLanguage: String by mutableStateOf("English")
    var ttsGranularity: String by mutableStateOf("Paragraph")
    var ttsStyle: String by mutableStateOf("Natural")
    var ttsColorTheme: String by mutableStateOf("Amber")
    var ttsUpdateFrequency: String by mutableStateOf("Realtime")
    var ttsLanguage: String by mutableStateOf("en-US")
    var ttsSpeed: Float by mutableStateOf(1.0f)
    var ttsPitch: Float by mutableStateOf(1.0f)
    var ttsVoice: String by mutableStateOf("default")
    var customReaderUiCss: String by mutableStateOf("")

    val chunks: SnapshotStateList<BookChunkEntity> = mutableStateListOf()

    // Search State
    var searchQuery by mutableStateOf("")
    var searchResults = mutableStateListOf<Int>()
    var currentMatchIndex by mutableStateOf(-1)

    init {
        LibraryStore.initDatabase(bookDao, viewModelScope)
        restoreLastSession()

        // Sync ReaderPreferencesStore with AppPreferences on start
        viewModelScope.launch {
            val prefs = ReaderPreferences(
                overrideBookFont = appPreferences.readerOverrideBookFont.first(),
                defaultFontSize = appPreferences.readerDefaultFontSizeInt.first(),
                minFontSize = appPreferences.readerMinFontSize.first(),
                fontWeightVal = appPreferences.readerFontWeight.first(),
                fontFaceSerif = appPreferences.readerFontFaceSerif.first(),
                fontFaceSans = appPreferences.readerFontFaceSans.first(),
                fontFaceMono = appPreferences.readerFontFaceMono.first(),
                overrideBookLayout = appPreferences.readerOverrideBookLayout.first(),
                useBookLayout = appPreferences.readerUseBookLayout.first(),
                paragraphMargin = appPreferences.readerParagraphMargin.first(),
                lineSpacing = appPreferences.readerLineSpacing.first(),
                wordSpacing = appPreferences.readerWordSpacing.first(),
                letterSpacing = appPreferences.readerLetterSpacing.first(),
                textIndent = appPreferences.readerTextIndent.first(),
                fullJustification = appPreferences.readerFullJustification.first(),
                hyphenation = appPreferences.readerHyphenation.first(),
                activeColorTheme = appPreferences.readerActiveColorTheme.first(),
                themeMode = appPreferences.readerThemeMode.first(),
                invertImageDark = appPreferences.readerInvertImageDark.first(),
                overrideBookColor = appPreferences.readerOverrideBookColor.first(),
                activeBackgroundIndex = appPreferences.readerActiveBackgroundIndex.first(),
                highlightColorSelection = appPreferences.readerHighlightColorSelection.first(),
                highlightOpacity = appPreferences.readerHighlightOpacity.first(),
                enableReadingRuler = appPreferences.readerEnableReadingRuler.first(),
                linesToHighlight = appPreferences.readerLinesToHighlight.first(),
                rulerColor = appPreferences.readerRulerColor.first(),
                rulerOpacity = appPreferences.readerRulerOpacity.first(),
                enableCodeHighlighting = appPreferences.readerEnableCodeHighlighting.first(),
                codeLanguageSelection = appPreferences.readerCodeLanguageSelection.first(),
                eInkMode = appPreferences.readerEInkMode.first(),
                topMargin = appPreferences.readerTopMargin.first(),
                bottomMargin = appPreferences.readerBottomMargin.first(),
                leftMargin = appPreferences.readerLeftMargin.first(),
                rightMargin = appPreferences.readerRightMargin.first(),
                showHeader = appPreferences.readerShowHeader.first(),
                showFooter = appPreferences.readerShowFooter.first(),
                showRemainingTime = appPreferences.readerShowRemainingTime.first(),
                showRemainingPages = appPreferences.readerShowRemainingPages.first(),
                showReadingProgress = appPreferences.readerShowReadingProgress.first(),
                progressStyle = appPreferences.readerProgressStyle.first(),
                showProgressBar = appPreferences.readerShowProgressBar.first(),
                showCurrentTime = appPreferences.readerShowCurrentTime.first(),
                showBatteryStatus = appPreferences.readerShowBatteryStatus.first(),
                showBatteryPercentage = appPreferences.readerShowBatteryPercentage.first(),
                orientationSelected = appPreferences.readerOrientationSelected.first(),
                writingMode = appPreferences.readerWritingMode.first(),
                columnGap = appPreferences.readerColumnGap.first(),
                maxColumns = appPreferences.readerMaxColumns.first(),
                maxColumnWidth = appPreferences.readerMaxColumnWidth.first(),
                maxColumnHeight = appPreferences.readerMaxColumnHeight.first(),
                customContentCss = appPreferences.readerCustomContentCss.first(),
                customReaderUiCss = appPreferences.readerCustomReaderUiCss.first(),
                pagingAnimation = appPreferences.readerPagingAnimation.first(),
                animationStyle = appPreferences.readerAnimationStyle.first(),
                systemScreenBrightness = appPreferences.readerSystemScreenBrightness.first(),
                brightnessValue = appPreferences.readerBrightnessValue.first(),
                autoBrightness = appPreferences.readerAutoBrightness.first(),
                isScrolledMode = appPreferences.readerIsScrolledMode.first(),
                isAutoScrolling = appPreferences.readerIsAutoScrolling.first(),
                autoscrollSpeed = appPreferences.readerAutoscrollSpeed.first(),
                autoscrollMode = appPreferences.readerAutoscrollMode.first(),
                isParagraphMode = appPreferences.readerIsParagraphMode.first(),
                isSpeedReading = appPreferences.readerIsSpeedReading.first(),
                isAutoMode = appPreferences.readerIsAutoMode.first(),
                swipeBrightness = appPreferences.readerSwipeBrightness.first(),
                keepScreenAwake = appPreferences.readerKeepScreenAwake.first(),
                colorEInkMode = appPreferences.readerColorEInkMode.first(),
                allowJavaScript = appPreferences.readerAllowJavaScript.first(),
                wordLensEnabled = appPreferences.readerWordLensEnabled.first(),
                enableTranslation = appPreferences.readerTranslationEnabled.first(),
                showSourceText = appPreferences.readerShowSourceText.first(),
                ttsTextOption = appPreferences.readerTtsTextOption.first(),
                translationService = appPreferences.readerTranslationService.first(),
                translateToLanguage = appPreferences.readerTranslateToLanguage.first(),
                ttsGranularity = appPreferences.readerTtsGranularity.first(),
                ttsStyle = appPreferences.readerTtsStyle.first(),
                ttsColorTheme = appPreferences.readerTtsColorTheme.first(),
                ttsUpdateFrequency = appPreferences.readerTtsUpdateFrequency.first(),
                ttsLanguage = appPreferences.readerTtsLanguage.first(),
                ttsSpeed = appPreferences.readerTtsSpeed.first(),
                ttsPitch = appPreferences.readerTtsPitch.first(),
                ttsVoice = appPreferences.readerTtsVoice.first()
            )
            ReaderPreferencesStore.update { prefs }
        }

        viewModelScope.launch {
            androidx.compose.runtime.snapshotFlow { ReaderPreferencesStore.preferences }.collect { prefs ->
                overrideBookFont = prefs.overrideBookFont
                defaultFontSize = prefs.defaultFontSize
                minFontSize = prefs.minFontSize
                fontWeightVal = prefs.fontWeightVal
                fontFaceSerif = prefs.fontFaceSerif
                fontFaceSans = prefs.fontFaceSans
                fontFaceMono = prefs.fontFaceMono
                overrideBookLayout = prefs.overrideBookLayout
                useBookLayout = prefs.useBookLayout
                paragraphMargin = prefs.paragraphMargin
                lineSpacing = prefs.lineSpacing
                wordSpacing = prefs.wordSpacing
                letterSpacing = prefs.letterSpacing
                textIndent = prefs.textIndent
                fullJustification = prefs.fullJustification
                hyphenation = prefs.hyphenation
                activeColorTheme = prefs.activeColorTheme
                themeMode = prefs.themeMode
                invertImageDark = prefs.invertImageDark
                overrideBookColor = prefs.overrideBookColor
                activeBackgroundIndex = prefs.activeBackgroundIndex
                highlightColorSelection = prefs.highlightColorSelection
                highlightOpacity = prefs.highlightOpacity
                enableReadingRuler = prefs.enableReadingRuler
                linesToHighlight = prefs.linesToHighlight
                rulerColor = prefs.rulerColor
                rulerOpacity = prefs.rulerOpacity
                enableCodeHighlighting = prefs.enableCodeHighlighting
                codeLanguageSelection = prefs.codeLanguageSelection
                eInkMode = prefs.eInkMode
                topMargin = prefs.topMargin
                bottomMargin = prefs.bottomMargin
                leftMargin = prefs.leftMargin
                rightMargin = prefs.rightMargin
                showHeader = prefs.showHeader
                showFooter = prefs.showFooter
                showRemainingTime = prefs.showRemainingTime
                showRemainingPages = prefs.showRemainingPages
                showReadingProgress = prefs.showReadingProgress
                progressStyle = prefs.progressStyle
                showProgressBar = prefs.showProgressBar
                showCurrentTime = prefs.showCurrentTime
                showBatteryStatus = prefs.showBatteryStatus
                showBatteryPercentage = prefs.showBatteryPercentage
                orientationSelected = prefs.orientationSelected
                writingMode = prefs.writingMode
                columnGap = prefs.columnGap
                maxColumns = prefs.maxColumns
                maxColumnWidth = prefs.maxColumnWidth
                maxColumnHeight = prefs.maxColumnHeight
                pagingAnimation = prefs.pagingAnimation
                animationStyle = prefs.animationStyle
                systemScreenBrightness = prefs.systemScreenBrightness
                brightnessValue = prefs.brightnessValue
                autoBrightness = prefs.autoBrightness
                isScrolledMode = prefs.isScrolledMode
                isAutoScrolling = prefs.isAutoScrolling
                autoscrollSpeed = prefs.autoscrollSpeed
                autoscrollMode = prefs.autoscrollMode
                isParagraphMode = prefs.isParagraphMode
                isSpeedReading = prefs.isSpeedReading
                isAutoMode = prefs.isAutoMode
                swipeBrightness = prefs.swipeBrightness
                keepScreenAwake = prefs.keepScreenAwake
                colorEInkMode = prefs.colorEInkMode
                allowJavaScript = prefs.allowJavaScript
                wordLensEnabled = prefs.wordLensEnabled
                enableTranslation = prefs.enableTranslation
                showSourceText = prefs.showSourceText
                ttsTextOption = prefs.ttsTextOption
                translationService = prefs.translationService
                translateToLanguage = prefs.translateToLanguage
                ttsGranularity = prefs.ttsGranularity
                ttsStyle = prefs.ttsStyle
                ttsColorTheme = prefs.ttsColorTheme
                ttsUpdateFrequency = prefs.ttsUpdateFrequency
                ttsLanguage = prefs.ttsLanguage
                ttsSpeed = prefs.ttsSpeed
                ttsPitch = prefs.ttsPitch
                ttsVoice = prefs.ttsVoice
            }
        }
    }

   private fun restoreLastSession() {
        viewModelScope.launch {
            val lastBookId = appPreferences.lastReadBookId.first()
            if (!lastBookId.isNullOrBlank()) {
                val bookEntity = withContext(Dispatchers.IO) {
                    bookDao.getBookById(lastBookId)
                }
                if (bookEntity != null) {
                    val book = bookEntity.toLibraryBook()
                    val chunkEntities = withContext(Dispatchers.IO) {
                        bookChunkDao.getChunksListForBook(lastBookId)
                    }
                    val chunkTexts = chunkEntities.map { it.text }
                    
                    withContext(Dispatchers.Main) {
                        if (chunkEntities.isNotEmpty()) {
                            chunks.clear()
                            chunks.addAll(chunkEntities)
                            ReaderSessionStore.loadBook(book, chunkTexts)
                        }
                    }
                    loadBookmarksForBook(lastBookId)
                }
            }
        }
    }

    fun loadBookChunks(bookId: String) {
        viewModelScope.launch {
            val bookEntity = withContext(Dispatchers.IO) {
                bookDao.getBookById(bookId)
            }
            if (bookEntity != null) {
                val book = bookEntity.toLibraryBook()
                val chunkEntities = withContext(Dispatchers.IO) {
                    bookChunkDao.getChunksListForBook(bookId)
                }
                val chunkTexts = chunkEntities.map { it.text }
                
                withContext(Dispatchers.Main) {
                    if (chunkEntities.isNotEmpty()) {
                        chunks.clear()
                        chunks.addAll(chunkEntities)
                        ReaderSessionStore.loadBook(book, chunkTexts)
                        appPreferences.setLastReadBookId(bookId)
                    }
                }
                loadBookmarksForBook(bookId)
            }
        }
    }

    fun updateSearchQuery(query: String, paragraphs: List<String>) {
        searchQuery = query
        searchResults.clear()
        if (query.length >= 2) {
            paragraphs.forEachIndexed { index, text ->
                if (text.contains(query, ignoreCase = true)) {
                    searchResults.add(index)
                }
            }
        }
        currentMatchIndex = if (searchResults.isNotEmpty()) 0 else -1
    }

    fun goToNextMatch() {
        if (searchResults.isNotEmpty()) {
            currentMatchIndex = (currentMatchIndex + 1) % searchResults.size
        }
    }

    fun goToPreviousMatch() {
        if (searchResults.isNotEmpty()) {
            currentMatchIndex = if (currentMatchIndex <= 0) searchResults.size - 1 else currentMatchIndex - 1
        }
    }

    fun clearSearch() {
        searchQuery = ""
        searchResults.clear()
        currentMatchIndex = -1
    }

    fun updateActiveColorTheme(value: String) {
        activeColorTheme = value
        ReaderPreferencesStore.update { copy(activeColorTheme = value) }
        viewModelScope.launch { appPreferences.setReaderActiveColorTheme(value) }
    }

    fun updateColorEInkMode(value: Boolean) {
        colorEInkMode = value
        ReaderPreferencesStore.update { copy(colorEInkMode = value) }
        viewModelScope.launch { appPreferences.setReaderColorEInkMode(value) }
    }

    fun updateTtsGranularity(value: String) {
        ttsGranularity = value
        ReaderPreferencesStore.update { copy(ttsGranularity = value) }
        viewModelScope.launch { appPreferences.setReaderTtsGranularity(value) }
    }

    fun updateEInkMode(value: Boolean) {
        eInkMode = value
        ReaderPreferencesStore.update { copy(eInkMode = value) }
        viewModelScope.launch { appPreferences.setReaderEInkMode(value) }
    }

    fun updateEnableReadingRuler(value: Boolean) {
        enableReadingRuler = value
        ReaderPreferencesStore.update { copy(enableReadingRuler = value) }
        viewModelScope.launch { appPreferences.setReaderEnableReadingRuler(value) }
    }

    fun updateOverrideBookFont(value: Boolean) {
        overrideBookFont = value
        ReaderPreferencesStore.update { copy(overrideBookFont = value) }
        viewModelScope.launch { appPreferences.setReaderOverrideBookFont(value) }
    }

    fun updateDefaultFontSize(value: Int) {
        defaultFontSize = value
        ReaderPreferencesStore.update { copy(defaultFontSize = value) }
        viewModelScope.launch { appPreferences.setReaderDefaultFontSizeInt(value) }
    }

    fun updateMinFontSize(value: Int) {
        minFontSize = value
        ReaderPreferencesStore.update { copy(minFontSize = value) }
        viewModelScope.launch { appPreferences.setReaderMinFontSize(value) }
    }

    fun updateFontWeight(value: Int) {
        fontWeightVal = value
        ReaderPreferencesStore.update { copy(fontWeightVal = value) }
        viewModelScope.launch { appPreferences.setReaderFontWeight(value) }
    }

    fun updateFontFaceSerif(value: String) {
        fontFaceSerif = value
        ReaderPreferencesStore.update { copy(fontFaceSerif = value) }
        viewModelScope.launch { appPreferences.setReaderFontFaceSerif(value) }
    }

    fun updateFontFaceSans(value: String) {
        fontFaceSans = value
        ReaderPreferencesStore.update { copy(fontFaceSans = value) }
        viewModelScope.launch { appPreferences.setReaderFontFaceSans(value) }
    }

    fun updateFontFaceMono(value: String) {
        fontFaceMono = value
        ReaderPreferencesStore.update { copy(fontFaceMono = value) }
        viewModelScope.launch { appPreferences.setReaderFontFaceMono(value) }
    }

    fun updateOverrideBookLayout(value: Boolean) {
        overrideBookLayout = value
        ReaderPreferencesStore.update { copy(overrideBookLayout = value) }
        viewModelScope.launch { appPreferences.setReaderOverrideBookLayout(value) }
    }

    fun updateUseBookLayout(value: Boolean) {
        useBookLayout = value
        ReaderPreferencesStore.update { copy(useBookLayout = value) }
        viewModelScope.launch { appPreferences.setReaderUseBookLayout(value) }
    }

    fun updateParagraphMargin(value: Float) {
        paragraphMargin = value
        ReaderPreferencesStore.update { copy(paragraphMargin = value) }
        viewModelScope.launch { appPreferences.setReaderParagraphMargin(value) }
    }

    fun updateLineSpacing(value: Float) {
        lineSpacing = value
        ReaderPreferencesStore.update { copy(lineSpacing = value) }
        viewModelScope.launch { appPreferences.setReaderLineSpacing(value) }
    }

    fun updateWordSpacing(value: Int) {
        wordSpacing = value
        ReaderPreferencesStore.update { copy(wordSpacing = value) }
        viewModelScope.launch { appPreferences.setReaderWordSpacing(value) }
    }

    fun updateLetterSpacing(value: Int) {
        letterSpacing = value
        ReaderPreferencesStore.update { copy(letterSpacing = value) }
        viewModelScope.launch { appPreferences.setReaderLetterSpacing(value) }
    }

    fun updateTextIndent(value: Int) {
        textIndent = value
        ReaderPreferencesStore.update { copy(textIndent = value) }
        viewModelScope.launch { appPreferences.setReaderTextIndent(value) }
    }

    fun updateFullJustification(value: Boolean) {
        fullJustification = value
        ReaderPreferencesStore.update { copy(fullJustification = value) }
        viewModelScope.launch { appPreferences.setReaderFullJustification(value) }
    }

    fun updateHyphenation(value: Boolean) {
        hyphenation = value
        ReaderPreferencesStore.update { copy(hyphenation = value) }
        viewModelScope.launch { appPreferences.setReaderHyphenation(value) }
    }

    fun updateTopMargin(value: Int) {
        topMargin = value
        ReaderPreferencesStore.update { copy(topMargin = value) }
        viewModelScope.launch { appPreferences.setReaderTopMargin(value) }
    }

    fun updateBottomMargin(value: Int) {
        bottomMargin = value
        ReaderPreferencesStore.update { copy(bottomMargin = value) }
        viewModelScope.launch { appPreferences.setReaderBottomMargin(value) }
    }

    fun updateLeftMargin(value: Int) {
        leftMargin = value
        ReaderPreferencesStore.update { copy(leftMargin = value) }
        viewModelScope.launch { appPreferences.setReaderLeftMargin(value) }
    }

    fun updateRightMargin(value: Int) {
        rightMargin = value
        ReaderPreferencesStore.update { copy(rightMargin = value) }
        viewModelScope.launch { appPreferences.setReaderRightMargin(value) }
    }

    fun updateShowHeader(value: Boolean) {
        showHeader = value
        ReaderPreferencesStore.update { copy(showHeader = value) }
        viewModelScope.launch { appPreferences.setReaderShowHeader(value) }
    }

    fun updateShowFooter(value: Boolean) {
        showFooter = value
        ReaderPreferencesStore.update { copy(showFooter = value) }
        viewModelScope.launch { appPreferences.setReaderShowFooter(value) }
    }

    fun updateShowRemainingTime(value: Boolean) {
        showRemainingTime = value
        ReaderPreferencesStore.update { copy(showRemainingTime = value) }
        viewModelScope.launch { appPreferences.setReaderShowRemainingTime(value) }
    }

    fun updateShowRemainingPages(value: Boolean) {
        showRemainingPages = value
        ReaderPreferencesStore.update { copy(showRemainingPages = value) }
        viewModelScope.launch { appPreferences.setReaderShowRemainingPages(value) }
    }

    fun updateShowReadingProgress(value: Boolean) {
        showReadingProgress = value
        ReaderPreferencesStore.update { copy(showReadingProgress = value) }
        viewModelScope.launch { appPreferences.setReaderShowReadingProgress(value) }
    }

    fun updateProgressStyle(value: String) {
        progressStyle = value
        ReaderPreferencesStore.update { copy(progressStyle = value) }
        viewModelScope.launch { appPreferences.setReaderProgressStyle(value) }
    }

    fun updateShowProgressBar(value: Boolean) {
        showProgressBar = value
        ReaderPreferencesStore.update { copy(showProgressBar = value) }
        viewModelScope.launch { appPreferences.setReaderShowProgressBar(value) }
    }

    fun updateShowCurrentTime(value: Boolean) {
        showCurrentTime = value
        ReaderPreferencesStore.update { copy(showCurrentTime = value) }
        viewModelScope.launch { appPreferences.setReaderShowCurrentTime(value) }
    }

    fun updateShowBatteryStatus(value: Boolean) {
        showBatteryStatus = value
        ReaderPreferencesStore.update { copy(showBatteryStatus = value) }
        viewModelScope.launch { appPreferences.setReaderShowBatteryStatus(value) }
    }

    fun updateShowBatteryPercentage(value: Boolean) {
        showBatteryPercentage = value
        ReaderPreferencesStore.update { copy(showBatteryPercentage = value) }
        viewModelScope.launch { appPreferences.setReaderShowBatteryPercentage(value) }
    }

    fun updateOrientation(value: String) {
        orientationSelected = value
        ReaderPreferencesStore.update { copy(orientationSelected = value) }
        viewModelScope.launch { appPreferences.setReaderOrientationSelected(value) }
    }

    fun updateInvertImageDark(value: Boolean) {
        invertImageDark = value
        ReaderPreferencesStore.update { copy(invertImageDark = value) }
        viewModelScope.launch { appPreferences.setReaderInvertImageDark(value) }
    }

    fun updateOverrideBookColor(value: Boolean) {
        overrideBookColor = value
        ReaderPreferencesStore.update { copy(overrideBookColor = value) }
        viewModelScope.launch { appPreferences.setReaderOverrideBookColor(value) }
    }

    fun updateActiveBackgroundIndex(value: Int) {
        activeBackgroundIndex = value
        ReaderPreferencesStore.update { copy(activeBackgroundIndex = value) }
        viewModelScope.launch { appPreferences.setReaderActiveBackgroundIndex(value) }
    }

    fun updateHighlightColorSelection(value: String) {
        highlightColorSelection = value
        ReaderPreferencesStore.update { copy(highlightColorSelection = value) }
        viewModelScope.launch { appPreferences.setReaderHighlightColorSelection(value) }
    }

    fun updateHighlightOpacity(value: Float) {
        highlightOpacity = value
        ReaderPreferencesStore.update { copy(highlightOpacity = value) }
        viewModelScope.launch { appPreferences.setReaderHighlightOpacity(value) }
    }

    fun updateLinesToHighlight(value: Int) {
        linesToHighlight = value
        ReaderPreferencesStore.update { copy(linesToHighlight = value) }
        viewModelScope.launch { appPreferences.setReaderLinesToHighlight(value) }
    }

    fun updateRulerColor(value: String) {
        rulerColor = value
        ReaderPreferencesStore.update { copy(rulerColor = value) }
        viewModelScope.launch { appPreferences.setReaderRulerColor(value) }
    }

    fun updateRulerOpacity(value: Float) {
        rulerOpacity = value
        ReaderPreferencesStore.update { copy(rulerOpacity = value) }
        viewModelScope.launch { appPreferences.setReaderRulerOpacity(value) }
    }

    fun updateEnableCodeHighlighting(value: Boolean) {
        enableCodeHighlighting = value
        ReaderPreferencesStore.update { copy(enableCodeHighlighting = value) }
        viewModelScope.launch { appPreferences.setReaderEnableCodeHighlighting(value) }
    }

    fun updateCodeLanguageSelection(value: String) {
        codeLanguageSelection = value
        ReaderPreferencesStore.update { copy(codeLanguageSelection = value) }
        viewModelScope.launch { appPreferences.setReaderCodeLanguageSelection(value) }
    }

    fun updatePagingAnimation(value: Boolean) {
        pagingAnimation = value
        ReaderPreferencesStore.update { copy(pagingAnimation = value) }
        viewModelScope.launch { appPreferences.setReaderPagingAnimation(value) }
    }

    fun updateAnimationStyle(value: String) {
        animationStyle = value
        ReaderPreferencesStore.update { copy(animationStyle = value) }
        viewModelScope.launch { appPreferences.setReaderAnimationStyle(value) }
    }

    fun updateThemeMode(value: String) {
        themeMode = value
        ReaderPreferencesStore.update { copy(themeMode = value) }
        viewModelScope.launch { appPreferences.setReaderThemeMode(value) }
    }

    fun updateWritingMode(value: String) {
        writingMode = value
        ReaderPreferencesStore.update { copy(writingMode = value) }
        viewModelScope.launch { appPreferences.setReaderWritingMode(value) }
    }

    fun updateColumnGap(value: Int) {
        columnGap = value
        ReaderPreferencesStore.update { copy(columnGap = value) }
        viewModelScope.launch { appPreferences.setReaderColumnGap(value) }
    }

    fun updateMaxColumns(value: Int) {
        maxColumns = value
        ReaderPreferencesStore.update { copy(maxColumns = value) }
        viewModelScope.launch { appPreferences.setReaderMaxColumns(value) }
    }

    fun updateMaxColumnWidth(value: Int) {
        maxColumnWidth = value
        ReaderPreferencesStore.update { copy(maxColumnWidth = value) }
        viewModelScope.launch { appPreferences.setReaderMaxColumnWidth(value) }
    }

    fun updateMaxColumnHeight(value: Int) {
        maxColumnHeight = value
        ReaderPreferencesStore.update { copy(maxColumnHeight = value) }
        viewModelScope.launch { appPreferences.setReaderMaxColumnHeight(value) }
    }

    fun updateCustomContentCss(value: String) {
        customContentCss = value
        ReaderPreferencesStore.update { copy(customContentCss = value) }
        viewModelScope.launch { appPreferences.setReaderCustomContentCss(value) }
    }

    fun updateCustomReaderUiCss(value: String) {
        customReaderUiCss = value
        ReaderPreferencesStore.update { copy(customReaderUiCss = value) }
        viewModelScope.launch { appPreferences.setReaderCustomReaderUiCss(value) }
    }

    fun updateSystemScreenBrightness(value: Boolean) {
        systemScreenBrightness = value
        ReaderPreferencesStore.update { copy(systemScreenBrightness = value) }
        viewModelScope.launch { appPreferences.setReaderSystemScreenBrightness(value) }
    }

    fun updateBrightnessValue(value: Int) {
        brightnessValue = value
        autoBrightness = false
        ReaderPreferencesStore.update { copy(brightnessValue = value, autoBrightness = false) }
        viewModelScope.launch { 
            appPreferences.setReaderBrightnessValue(value)
            appPreferences.setReaderAutoBrightness(false)
        }
    }

    fun updateAutoBrightness(value: Boolean) {
        autoBrightness = value
        ReaderPreferencesStore.update { copy(autoBrightness = value) }
        viewModelScope.launch { appPreferences.setReaderAutoBrightness(value) }
    }

    fun updateIsScrolledMode(value: Boolean) {
        isScrolledMode = value
        ReaderPreferencesStore.update { copy(isScrolledMode = value) }
        viewModelScope.launch { appPreferences.setReaderIsScrolledMode(value) }
    }

    fun updateIsAutoScrolling(value: Boolean) {
        isAutoScrolling = value
        ReaderPreferencesStore.update { copy(isAutoScrolling = value) }
        viewModelScope.launch { appPreferences.setReaderIsAutoScrolling(value) }
    }

    fun updateAutoscrollSpeed(value: Float) {
        autoscrollSpeed = value
        ReaderPreferencesStore.update { copy(autoscrollSpeed = value) }
        viewModelScope.launch { appPreferences.setReaderAutoscrollSpeed(value) }
    }

    fun updateAutoscrollMode(value: String) {
        autoscrollMode = value
        ReaderPreferencesStore.update { copy(autoscrollMode = value) }
        viewModelScope.launch { appPreferences.setReaderAutoscrollMode(value) }
    }

    fun updateIsParagraphMode(value: Boolean) {
        isParagraphMode = value
        ReaderPreferencesStore.update { copy(isParagraphMode = value) }
        viewModelScope.launch { appPreferences.setReaderIsParagraphMode(value) }
    }

    fun updateIsSpeedReading(value: Boolean) {
        isSpeedReading = value
        ReaderPreferencesStore.update { copy(isSpeedReading = value) }
        viewModelScope.launch { appPreferences.setReaderIsSpeedReading(value) }
    }

    fun updateIsAutoMode(value: Boolean) {
        isAutoMode = value
        ReaderPreferencesStore.update { copy(isAutoMode = value) }
        viewModelScope.launch { appPreferences.setReaderIsAutoMode(value) }
    }

    fun updateSwipeBrightness(value: Boolean) {
        swipeBrightness = value
        ReaderPreferencesStore.update { copy(swipeBrightness = value) }
        viewModelScope.launch { appPreferences.setReaderSwipeBrightness(value) }
    }

    fun updateKeepScreenAwake(value: Boolean) {
        keepScreenAwake = value
        ReaderPreferencesStore.update { copy(keepScreenAwake = value) }
        viewModelScope.launch { appPreferences.setReaderKeepScreenAwake(value) }
    }

    fun updateAllowJavaScript(value: Boolean) {
        allowJavaScript = value
        ReaderPreferencesStore.update { copy(allowJavaScript = value) }
        viewModelScope.launch { appPreferences.setReaderAllowJavaScript(value) }
    }

    fun updateEnableTranslation(value: Boolean) {
        enableTranslation = value
        ReaderPreferencesStore.update { copy(enableTranslation = value) }
        viewModelScope.launch { appPreferences.setReaderTranslationEnabled(value) }
    }

    fun updateShowSourceText(value: Boolean) {
        showSourceText = value
        ReaderPreferencesStore.update { copy(showSourceText = value) }
        viewModelScope.launch { appPreferences.setReaderShowSourceText(value) }
    }

    fun updateTtsTextOption(value: String) {
        ttsTextOption = value
        ReaderPreferencesStore.update { copy(ttsTextOption = value) }
        viewModelScope.launch { appPreferences.setReaderTtsTextOption(value) }
    }

    fun updateTranslationService(value: String) {
        translationService = value
        ReaderPreferencesStore.update { copy(translationService = value) }
        viewModelScope.launch { appPreferences.setReaderTranslationService(value) }
    }

    fun updateTranslateToLanguage(value: String) {
        translateToLanguage = value
        ReaderPreferencesStore.update { copy(translateToLanguage = value) }
        viewModelScope.launch { appPreferences.setReaderTranslateToLanguage(value) }
    }

    fun updateTtsStyle(value: String) {
        ttsStyle = value
        ReaderPreferencesStore.update { copy(ttsStyle = value) }
        viewModelScope.launch { appPreferences.setReaderTtsStyle(value) }
    }

    fun updateTtsColorTheme(value: String) {
        ttsColorTheme = value
        ReaderPreferencesStore.update { copy(ttsColorTheme = value) }
        viewModelScope.launch { appPreferences.setReaderTtsColorTheme(value) }
    }

    fun updateTtsUpdateFrequency(value: String) {
        ttsUpdateFrequency = value
        ReaderPreferencesStore.update { copy(ttsUpdateFrequency = value) }
        viewModelScope.launch { appPreferences.setReaderTtsUpdateFrequency(value) }
    }

    fun updateTtsLanguage(value: String) {
        ttsLanguage = value
        ReaderPreferencesStore.update { copy(ttsLanguage = value) }
        viewModelScope.launch { appPreferences.setReaderTtsLanguage(value) }
    }

    fun updateTtsSpeed(value: Float) {
        ttsSpeed = value
        ReaderPreferencesStore.update { copy(ttsSpeed = value) }
        viewModelScope.launch { appPreferences.setReaderTtsSpeed(value) }
    }

    fun updateTtsPitch(value: Float) {
        ttsPitch = value
        ReaderPreferencesStore.update { copy(ttsPitch = value) }
        viewModelScope.launch { appPreferences.setReaderTtsPitch(value) }
    }

    fun updateTtsVoice(value: String) {
        ttsVoice = value
        ReaderPreferencesStore.update { copy(ttsVoice = value) }
        viewModelScope.launch { appPreferences.setReaderTtsVoice(value) }
    }

    fun speakText(text: String, mediaActions: com.creativeblisslabs.omnimind.core.media.MediaActions) {
        if (text.isBlank()) return
        mediaActions.speakText(text, ttsLanguage, ttsSpeed, ttsPitch)
    }

    fun speakParagraph(index: Int, mediaActions: com.creativeblisslabs.omnimind.core.media.MediaActions) {
        val paragraphText = chunks.getOrNull(index)?.text ?: return
        speakText(paragraphText, mediaActions)
    }

    var isOriginalPdfMode: Boolean by mutableStateOf(false)
    var activeTranslationResult: String by mutableStateOf("")
    var isTranslating: Boolean by mutableStateOf(false)
    var isTranslatingFullBook: Boolean by mutableStateOf(false)
    var fullBookTranslationProgress: Float by mutableStateOf(0f)
    var isBookTranslated: Boolean by mutableStateOf(false)
    var originalChunksBackup: List<String> = emptyList()

    fun translateText(sourceText: String, targetLang: String = translateToLanguage, onResult: (String) -> Unit = {}) {
        if (sourceText.isBlank()) return
        isTranslating = true
        viewModelScope.launch(Dispatchers.IO) {
            val prompt = "Translate the following text to $targetLang accurately and return ONLY the translated text without notes or intro:\n\n$sourceText"
            val res = onDeviceAiRuntime.generate(prompt)
            val translated = res.getOrNull()?.trim() ?: "Translation completed."
            withContext(Dispatchers.Main) {
                activeTranslationResult = translated
                isTranslating = false
                onResult(translated)
            }
        }
    }

    fun translateFullBook(targetLang: String = translateToLanguage, onProgress: (Float) -> Unit = {}) {
        val currentChunks = ReaderSessionStore.state.chunks
        if (currentChunks.isEmpty()) return
        if (isTranslatingFullBook) return

        if (!isBookTranslated) {
            originalChunksBackup = currentChunks.toList()
        }

        isTranslatingFullBook = true
        fullBookTranslationProgress = 0f

        viewModelScope.launch(Dispatchers.IO) {
            val translatedChunks = mutableListOf<String>()
            val total = currentChunks.size
            for (i in currentChunks.indices) {
                val chunk = currentChunks[i]
                val prompt = "Translate the following book content accurately to $targetLang. Maintain headings, markdown, and paragraphs. Return ONLY the translated markdown:\n\n$chunk"
                val res = onDeviceAiRuntime.generate(prompt)
                val translatedText = res.getOrNull()?.trim() ?: chunk
                translatedChunks.add(translatedText)
                val progress = ((i + 1).toFloat() / total)
                withContext(Dispatchers.Main) {
                    fullBookTranslationProgress = progress
                    onProgress(progress)
                }
            }

            withContext(Dispatchers.Main) {
                isTranslatingFullBook = false
                isBookTranslated = true
                ReaderSessionStore.update { copy(chunks = translatedChunks) }
            }
        }
    }

    fun revertToOriginalBookText() {
        if (originalChunksBackup.isNotEmpty()) {
            ReaderSessionStore.update { copy(chunks = originalChunksBackup) }
            isBookTranslated = false
        }
    }

    fun translateSelectedText(text: String) {
        translateText(text, translateToLanguage)
    }

    fun saveHighlight(bookId: String, text: String, colorHex: String, pageProgress: Int, chapterIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val bookmark = BookmarkEntity(
                bookId = bookId,
                title = text.take(60),
                pageProgress = pageProgress,
                chapterIndex = chapterIndex,
                noteText = text,
                highlightColorHex = colorHex
            )
            bookmarkDao.insertBookmark(bookmark)
        }
    }

    fun updateWordLensEnabled(value: Boolean) {
        wordLensEnabled = value
        ReaderPreferencesStore.update { copy(wordLensEnabled = value) }
        viewModelScope.launch { appPreferences.setReaderWordLensEnabled(value) }
    }

    fun importBook(
        attachment: MediaAttachmentUi,
        password: String? = null,
        onPasswordRequired: ((path: String, name: String) -> Unit)? = null,
        onComplete: (LibraryBook, List<String>) -> Unit
    ) {
        viewModelScope.launch {
            // Check for existing book by path to avoid duplicates (unless re-authenticating with password)
            val existingBook = LibraryStore.books.find { it.filePath == attachment.path }
            if (existingBook != null && password == null) {
                val existingChunks = withContext(Dispatchers.IO) {
                    bookChunkDao.getChunksForBook(existingBook.id).firstOrNull()?.map { it.text } ?: emptyList()
                }
                val existingChunkEntities = withContext(Dispatchers.IO) {
                    bookChunkDao.getChunksForBook(existingBook.id).firstOrNull() ?: emptyList()
                }
                
                var bookToLoad = existingBook
                if (bookToLoad.coverImagePath.isBlank()) {
                    val cover = withContext(Dispatchers.IO) {
                        mediaProcessor.extractBookCover(existingBook.filePath, existingBook.id)
                    }
                    if (!cover.isNullOrBlank()) {
                        bookToLoad = existingBook.copy(coverImagePath = cover)
                        LibraryStore.addBook(bookToLoad, bookDao, this@launch)
                    }
                }

                if (existingChunks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        chunks.clear()
                        chunks.addAll(existingChunkEntities)
                        ReaderSessionStore.loadBook(bookToLoad, existingChunks)
                        appPreferences.setLastReadBookId(existingBook.id)
                    }

                    onComplete(bookToLoad, existingChunks)
                    return@launch
                }
            }

            isImportingBook = true
            importBookName = attachment.name
            importProgress = 0.1f

            val bookId = existingBook?.id ?: com.creativeblisslabs.omnimind.core.util.UuidUtils.generateUUID()
            val ext = attachment.path.substringAfterLast('.', "EPUB").uppercase()
            
            val allChunks = mutableListOf<String>()
            
            // Extract cover image
            val coverPath = withContext(Dispatchers.IO) {
                mediaProcessor.extractBookCover(attachment.path, bookId) ?: ""
            }

            // Create and insert book entity first
            val initialBook = LibraryBook(
                id = bookId,
                title = attachment.name.substringBeforeLast('.'),
                author = "Local Document",
                filePath = attachment.path,
                fileType = ext,
                publisher = "Local Storage",
                publishedDate = "Recent Import",
                addedDate = "Today",
                subjects = "User Import, $ext Document",
                descriptionText = "Importing...",
                coverImagePath = coverPath
            )

            // Insert initial book entity in DB first to satisfy foreign key constraints for book_chunks
            withContext(Dispatchers.IO) {
                bookDao.insertBook(initialBook.toEntity())
            }

            var isEncryptedFailure = false
            val book = withContext(Dispatchers.IO) {
                try {
                    var chunkIndex = 0
                    val currentChunkBuilder = StringBuilder()
                    val chunkSizeLimit = 1024 // 1KB chunks so all file sizes produce chunks

                    var firstChunk = true
                    var totalChars = 0
                    val first10k = StringBuilder()

                    mediaProcessor.readAsTextFlow(attachment.path, password).collect { chunk ->
                        currentChunkBuilder.append(chunk)
                        totalChars += chunk.length
                        
                        if (first10k.length < 10000) {
                            first10k.append(chunk.take(10000 - first10k.length))
                        }

                        if (currentChunkBuilder.length >= chunkSizeLimit) {
                            val chunkText = currentChunkBuilder.toString()
                            allChunks.add(chunkText)
                            bookChunkDao.insertChunks(listOf(BookChunkEntity(bookId = bookId, chunkIndex = chunkIndex, text = chunkText)))
                            chunkIndex++
                            currentChunkBuilder.clear()
                        }

                        if (firstChunk) {
                            withContext(Dispatchers.Main) { importProgress = 0.3f }
                            firstChunk = false
                        }
                    }

                    // Insert remaining text
                    if (currentChunkBuilder.isNotEmpty()) {
                        val remainingText = currentChunkBuilder.toString()
                        allChunks.add(remainingText)
                        bookChunkDao.insertChunks(listOf(BookChunkEntity(bookId = bookId, chunkIndex = chunkIndex, text = remainingText)))
                    }

                    if (allChunks.isEmpty()) {
                        bookDao.deleteBookById(bookId)
                        return@withContext null
                    }

                    withContext(Dispatchers.Main) { importProgress = 0.5f }

                    bookAnalyzer.analyzeBook(attachment.name, first10k.toString())
                    withContext(Dispatchers.Main) { importProgress = 0.7f }

                    val sizeStr = "${totalChars / 1024} KB"

                    val finalBook = initialBook.copy(
                        fileSize = sizeStr,
                        descriptionText = "Imported e-book document from device storage. File name: ${attachment.name}",
                        coverImagePath = coverPath
                    )

                    // Update the book entity with full metadata in DB and in-memory store
                    bookDao.updateBook(finalBook.toEntity())
                    withContext(Dispatchers.Main) { 
                        importProgress = 1.0f 
                        LibraryStore.addBook(finalBook, null, this)
                    }
                    finalBook
                } catch (e: Exception) {
                    if (e.message?.contains("password", ignoreCase = true) == true || e.message?.contains("decrypt", ignoreCase = true) == true) {
                        isEncryptedFailure = true
                    }
                    e.printStackTrace()
                    null
                }
            }

            isImportingBook = false
            if (isEncryptedFailure) {
                withContext(Dispatchers.Main) {
                    onPasswordRequired?.invoke(attachment.path, attachment.name)
                }
            } else {
                book?.let {
                    appPreferences.setLastReadBookId(it.id)
                    
                    val chunkEntities: List<BookChunkEntity> = allChunks.mapIndexed { index: Int, text: String ->
                        BookChunkEntity(bookId = it.id, chunkIndex = index, text = text)
                    }
                    
                    println("READER_LOG: Completed import for bookId=${it.id}, chunks=${allChunks.size}")
                    chunks.clear()
                    chunks.addAll(chunkEntities)
                    ReaderSessionStore.loadBook(it, allChunks)

                    onComplete(it, allChunks)
                }
            }
        }
    }

    private val _bookmarks = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkEntity>> = _bookmarks.asStateFlow()

    fun loadBookmarksForBook(bookId: String) {
        viewModelScope.launch {
            bookmarkDao.getBookmarksForBook(bookId).collect { list ->
                _bookmarks.value = list
            }
        }
    }

    fun addBookmark(
        bookId: String,
        title: String,
        pageProgress: Int,
        chapterIndex: Int,
        noteText: String = "",
        highlightColorHex: String = "#FFEA00"
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = BookmarkEntity(
                bookId = bookId,
                title = title,
                pageProgress = pageProgress,
                chapterIndex = chapterIndex,
                noteText = noteText,
                highlightColorHex = highlightColorHex
            )
            bookmarkDao.insertBookmark(entity)
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(bookmarkId)
        }
    }

    fun deleteBookmarkByTitle(bookId: String, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val found = _bookmarks.value.find { it.bookId == bookId && (it.title == title || it.noteText == title) }
            if (found != null) {
                bookmarkDao.deleteBookmark(found.id)
            }
        }
    }

    fun updateProgress(bookId: String, progress: Int, chapterIndex: Int, lastReadPosition: Long = 0L) {
        viewModelScope.launch {
            LibraryStore.updateProgress(bookId, progress, chapterIndex, lastReadPosition, bookDao, viewModelScope)
            appPreferences.setLastReadBookId(bookId)
        }
    }

    fun deleteBook(book: LibraryBook) {
        viewModelScope.launch {
            LibraryStore.deleteBook(book, bookDao, viewModelScope, bookChunkDao)
            val lastId = appPreferences.lastReadBookId.first()
            if (lastId == book.id) {
                appPreferences.setLastReadBookId(null)
            }
        }
    }

    fun saveVoiceNote(pcmData: FloatArray, bookId: String, progress: Int, chapter: Int) {
        viewModelScope.launch {
            
            val timestamp = kotlinx.datetime.Clock.System.now()
            val dateStr = timestamp.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                .let { "${it.month.name.take(3)} ${it.dayOfMonth}, ${it.hour}:${it.minute.toString().padStart(2, '0')}" }

            val bookmark = BookmarkEntity(
                bookId = bookId,
                title = "Voice Note - $dateStr",
                pageProgress = progress,
                chapterIndex = chapter,
                // noteText = transcription
            )
            
            withContext(Dispatchers.IO) {
                bookmarkDao.insertBookmark(bookmark)
            }
        }
    }

    fun reimportWithPassword(id: String, path: String, pass: String) {
        viewModelScope.launch {
            val existingBook = LibraryStore.books.find { it.id == id }
            val attachment = MediaAttachmentUi(
                type = "PDF",
                name = existingBook?.title ?: "Protected_PDF.pdf",
                path = path
            )
            importBook(attachment, password = pass) { book, chunks ->
                ReaderSessionStore.loadBook(book, chunks)
                ReaderSessionStore.update { copy(isPasswordRequired = false) }
            }
        }
    }
}
