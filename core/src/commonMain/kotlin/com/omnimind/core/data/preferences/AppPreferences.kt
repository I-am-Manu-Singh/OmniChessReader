package com.creativeblisslabs.omnimind.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferences(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_THEME = stringPreferencesKey("theme") // system, dark, light
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        val KEY_ZAPIER_TOKEN = stringPreferencesKey("zapier_token")
        val KEY_VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val KEY_TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val KEY_AI_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_HF_TOKEN = stringPreferencesKey("hf_token")
        
        // Multi-model storage keys (Map-like via concatenated keys)
        fun modelStatusKey(profile: String) = stringPreferencesKey("model_status_$profile")
        fun modelPathKey(profile: String) = stringPreferencesKey("model_path_$profile")
        fun modelErrorKey(profile: String) = stringPreferencesKey("model_error_$profile")
        fun modelUrlKey(profile: String) = stringPreferencesKey("model_url_$profile")

        val KEY_LOCAL_MODEL = stringPreferencesKey("local_model") // Primary LLM selected
      
        val KEY_UI_FONT_SCALE = floatPreferencesKey("ui_font_scale")
        val KEY_UI_CONTENT_WIDTH = stringPreferencesKey("ui_content_width")
        val KEY_UI_SPACING = intPreferencesKey("ui_spacing")
        val KEY_READER_FONT_SIZE = floatPreferencesKey("reader_font_size")
        val KEY_READER_LINE_HEIGHT = floatPreferencesKey("reader_line_height")
        val KEY_READER_THEME = stringPreferencesKey("reader_theme")
        val KEY_READER_FONT_FAMILY = stringPreferencesKey("reader_font_family")
        val KEY_READER_TTS_ENABLED = booleanPreferencesKey("reader_tts_enabled")
        val KEY_READER_LANGUAGE = stringPreferencesKey("reader_language")
        val KEY_READER_TRANSLATION_ENABLED = booleanPreferencesKey("reader_translation_enabled")
        val KEY_READER_KEEP_SCREEN_AWAKE = booleanPreferencesKey("reader_keep_screen_awake")
        val KEY_READER_PAGING_ANIMATION = booleanPreferencesKey("reader_paging_animation")

        // Expanded Reader Preferences
        val KEY_READER_OVERRIDE_BOOK_FONT = booleanPreferencesKey("reader_override_book_font")
        val KEY_READER_DEFAULT_FONT_SIZE = intPreferencesKey("reader_default_font_size")
        val KEY_READER_MIN_FONT_SIZE = intPreferencesKey("reader_min_font_size")
        val KEY_READER_FONT_WEIGHT = intPreferencesKey("reader_font_weight")
        val KEY_READER_FONT_FACE_SERIF = stringPreferencesKey("reader_font_face_serif")
        val KEY_READER_FONT_FACE_SANS = stringPreferencesKey("reader_font_face_sans")
        val KEY_READER_FONT_FACE_MONO = stringPreferencesKey("reader_font_face_mono")
        val KEY_READER_OVERRIDE_BOOK_LAYOUT = booleanPreferencesKey("reader_override_book_layout")
        val KEY_READER_USE_BOOK_LAYOUT = booleanPreferencesKey("reader_use_book_layout")
        val KEY_READER_PARAGRAPH_MARGIN = floatPreferencesKey("reader_paragraph_margin")
        val KEY_READER_LINE_SPACING = floatPreferencesKey("reader_line_spacing")
        val KEY_READER_WORD_SPACING = intPreferencesKey("reader_word_spacing")
        val KEY_READER_LETTER_SPACING = intPreferencesKey("reader_letter_spacing")
        val KEY_READER_TEXT_INDENT = intPreferencesKey("reader_text_indent")
        val KEY_READER_FULL_JUSTIFICATION = booleanPreferencesKey("reader_full_justification")
        val KEY_READER_HYPHENATION = booleanPreferencesKey("reader_hyphenation")
        val KEY_READER_ACTIVE_COLOR_THEME = stringPreferencesKey("reader_active_color_theme")
        val KEY_READER_THEME_MODE = stringPreferencesKey("reader_theme_mode")
        val KEY_READER_INVERT_IMAGE_DARK = booleanPreferencesKey("reader_invert_image_dark")
        val KEY_READER_OVERRIDE_BOOK_COLOR = booleanPreferencesKey("reader_override_book_color")
        val KEY_READER_ACTIVE_BACKGROUND_INDEX = intPreferencesKey("reader_active_background_index")
        val KEY_READER_HIGHLIGHT_COLOR_SELECTION = stringPreferencesKey("reader_highlight_color_selection")
        val KEY_READER_HIGHLIGHT_OPACITY = floatPreferencesKey("reader_highlight_opacity")
        val KEY_READER_ENABLE_READING_RULER = booleanPreferencesKey("reader_enable_reading_ruler")
        val KEY_READER_LINES_TO_HIGHLIGHT = intPreferencesKey("reader_lines_to_highlight")
        val KEY_READER_RULER_COLOR = stringPreferencesKey("reader_ruler_color")
        val KEY_READER_RULER_OPACITY = floatPreferencesKey("reader_ruler_opacity")
        val KEY_READER_ENABLE_CODE_HIGHLIGHTING = booleanPreferencesKey("reader_enable_code_highlighting")
        val KEY_READER_CODE_LANGUAGE_SELECTION = stringPreferencesKey("reader_code_language_selection")
        val KEY_READER_E_INK_MODE = booleanPreferencesKey("reader_e_ink_mode")
        val KEY_READER_TOP_MARGIN = intPreferencesKey("reader_top_margin")
        val KEY_READER_BOTTOM_MARGIN = intPreferencesKey("reader_bottom_margin")
        val KEY_READER_LEFT_MARGIN = intPreferencesKey("reader_left_margin")
        val KEY_READER_RIGHT_MARGIN = intPreferencesKey("reader_right_margin")
        val KEY_READER_SHOW_HEADER = booleanPreferencesKey("reader_show_header")
        val KEY_READER_SHOW_FOOTER = booleanPreferencesKey("reader_show_footer")
        val KEY_READER_SHOW_REMAINING_TIME = booleanPreferencesKey("reader_show_remaining_time")
        val KEY_READER_SHOW_REMAINING_PAGES = booleanPreferencesKey("reader_show_remaining_pages")
        val KEY_READER_SHOW_READING_PROGRESS = booleanPreferencesKey("reader_show_reading_progress")
        val KEY_READER_PROGRESS_STYLE = stringPreferencesKey("reader_progress_style")
        val KEY_READER_SHOW_PROGRESS_BAR = booleanPreferencesKey("reader_show_progress_bar")
        val KEY_READER_SHOW_CURRENT_TIME = booleanPreferencesKey("reader_show_current_time")
        val KEY_READER_SHOW_BATTERY_STATUS = booleanPreferencesKey("reader_show_battery_status")
        val KEY_READER_SHOW_BATTERY_PERCENTAGE = booleanPreferencesKey("reader_show_battery_percentage")
        val KEY_READER_ORIENTATION_SELECTED = stringPreferencesKey("reader_orientation_selected")
        val KEY_READER_WRITING_MODE = stringPreferencesKey("reader_writing_mode")
        val KEY_READER_COLUMN_GAP = intPreferencesKey("reader_column_gap")
        val KEY_READER_MAX_COLUMNS = intPreferencesKey("reader_max_columns")
        val KEY_READER_MAX_COLUMN_WIDTH = intPreferencesKey("reader_max_column_width")
        val KEY_READER_MAX_COLUMN_HEIGHT = intPreferencesKey("reader_max_column_height")
        val KEY_READER_CUSTOM_CONTENT_CSS = stringPreferencesKey("reader_custom_content_css")
        val KEY_READER_CUSTOM_READER_UI_CSS = stringPreferencesKey("reader_custom_reader_ui_css")
        val KEY_READER_ANIMATION_STYLE = stringPreferencesKey("reader_animation_style")
        val KEY_READER_SYSTEM_SCREEN_BRIGHTNESS = booleanPreferencesKey("reader_system_screen_brightness")
        val KEY_READER_BRIGHTNESS_VALUE = intPreferencesKey("reader_brightness_value")
        val KEY_READER_AUTO_BRIGHTNESS = booleanPreferencesKey("reader_auto_brightness")
        val KEY_READER_IS_SCROLLED_MODE = booleanPreferencesKey("reader_is_scrolled_mode")
        val KEY_READER_IS_AUTO_SCROLLING = booleanPreferencesKey("reader_is_auto_scrolling")
        val KEY_READER_AUTOSCROLL_SPEED = floatPreferencesKey("reader_autoscroll_speed")
        val KEY_READER_AUTOSCROLL_MODE = stringPreferencesKey("reader_autoscroll_mode")
        val KEY_READER_IS_PARAGRAPH_MODE = booleanPreferencesKey("reader_is_paragraph_mode")
        val KEY_READER_IS_SPEED_READING = booleanPreferencesKey("reader_is_speed_reading")
        val KEY_READER_IS_AUTO_MODE = booleanPreferencesKey("reader_is_auto_mode")
        val KEY_READER_SWIPE_BRIGHTNESS = booleanPreferencesKey("reader_swipe_brightness")
        val KEY_READER_COLOR_EINK_MODE = booleanPreferencesKey("reader_color_eink_mode")
        val KEY_READER_ALLOW_JAVASCRIPT = booleanPreferencesKey("reader_allow_javascript")
        val KEY_READER_WORD_LENS_ENABLED = booleanPreferencesKey("reader_word_lens_enabled")
        val KEY_READER_SHOW_SOURCE_TEXT = booleanPreferencesKey("reader_show_source_text")
        val KEY_READER_TTS_TEXT_OPTION = stringPreferencesKey("reader_tts_text_option")
        val KEY_READER_TRANSLATION_SERVICE = stringPreferencesKey("reader_translation_service")
        val KEY_READER_TRANSLATE_TO_LANGUAGE = stringPreferencesKey("reader_translate_to_language")
        val KEY_READER_TTS_GRANULARITY = stringPreferencesKey("reader_tts_granularity")
        val KEY_READER_TTS_STYLE = stringPreferencesKey("reader_tts_style")
        val KEY_READER_TTS_COLOR_THEME = stringPreferencesKey("reader_tts_color_theme")
        val KEY_READER_TTS_UPDATE_FREQUENCY = stringPreferencesKey("reader_tts_update_frequency")
        val KEY_READER_TTS_LANGUAGE = stringPreferencesKey("reader_tts_language")
        val KEY_READER_TTS_SPEED = floatPreferencesKey("reader_tts_speed")
        val KEY_READER_TTS_PITCH = floatPreferencesKey("reader_tts_pitch")
        val KEY_READER_TTS_VOICE = stringPreferencesKey("reader_tts_voice")

        // Chess Settings
        val KEY_CHESS_ENGINE_VERSION = stringPreferencesKey("chess_engine_version")
        val KEY_CHESS_THEME = stringPreferencesKey("chess_theme")
        val KEY_CHESS_PIECE_STYLE = stringPreferencesKey("chess_piece_style")
        val KEY_CHESS_BOT_LEVEL = intPreferencesKey("chess_bot_level")
        val KEY_CHESS_ENGINE_SEARCH_TIME = intPreferencesKey("chess_engine_search_time")
        val KEY_CHESS_ENGINE_MULTI_PV = intPreferencesKey("chess_engine_multi_pv")
        val KEY_CHESS_MAGNIFY_PIECE = booleanPreferencesKey("chess_magnify_piece")
        val KEY_CHESS_TARGET_CIRCLE = booleanPreferencesKey("chess_target_circle")
        val KEY_CHESS_PIECE_ANIMATION = booleanPreferencesKey("chess_piece_animation")
        val KEY_CHESS_IMMERSIVE_MODE = booleanPreferencesKey("chess_immersive_mode")
        val KEY_CHESS_SHOW_DESTINATIONS = booleanPreferencesKey("chess_show_destinations")
        val KEY_CHESS_SHOW_HIGHLIGHTS = booleanPreferencesKey("chess_show_highlights")
        val KEY_CHESS_SHOW_MOVE_LIST = booleanPreferencesKey("chess_show_move_list")
        val KEY_CHESS_SHOW_MATERIAL_DIFF = booleanPreferencesKey("chess_show_material_diff")
        val KEY_CHESS_CLOCK_RIGHT = booleanPreferencesKey("chess_clock_right")
        val KEY_CHESS_PREMOVES = booleanPreferencesKey("chess_premoves")
        val KEY_CHESS_CONFIRM_RESIGN = booleanPreferencesKey("chess_confirm_resign")
        val KEY_CHESS_TOUCH_FEEDBACK = booleanPreferencesKey("chess_touch_feedback")
        val KEY_CHESS_LICHESS_USERNAME = stringPreferencesKey("chess_lichess_username")
        val KEY_CHESS_CHESSCOM_USERNAME = stringPreferencesKey("chess_chesscom_username")

        val KEY_LAST_READ_BOOK_ID = stringPreferencesKey("last_read_book_id")

        // Last Active / Played Chess Game state
        val KEY_CHESS_LAST_GAME_FEN = stringPreferencesKey("chess_last_game_fen")
        val KEY_CHESS_LAST_GAME_PGN = stringPreferencesKey("chess_last_game_pgn")
        val KEY_CHESS_LAST_GAME_TITLE = stringPreferencesKey("chess_last_game_title")
        val KEY_CHESS_LAST_GAME_DATE = longPreferencesKey("chess_last_game_date")
        val KEY_CHESS_LAST_GAME_MODE = stringPreferencesKey("chess_last_game_mode")
    }

    val geminiApiKey: Flow<String> = dataStore.data.map { it[KEY_GEMINI_API_KEY] ?: "" }
    val userName: Flow<String> = dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val theme: Flow<String> = dataStore.data.map { it[KEY_THEME] ?: "dark" }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIFICATIONS_ENABLED] ?: true }
    val voiceEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_VOICE_ENABLED] ?: true }
    val ttsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_TTS_ENABLED] ?: true }

    val hfToken: Flow<String> = dataStore.data.map { 
        val stored = it[KEY_HF_TOKEN]
        if (stored.isNullOrBlank()) "" else stored 
    }
    val aiProvider: Flow<String> = dataStore.data.map { it[KEY_AI_PROVIDER] ?: "local_first" }
    val localModel: Flow<String> = dataStore.data.map { it[KEY_LOCAL_MODEL] ?: "Gemma4" }

    fun getModelStatus(profile: String): Flow<String> = dataStore.data.map { it[modelStatusKey(profile)] ?: "not_downloaded" }
    fun getModelPath(profile: String): Flow<String> = dataStore.data.map { it[modelPathKey(profile)] ?: "" }
    fun getModelError(profile: String): Flow<String> = dataStore.data.map { it[modelErrorKey(profile)] ?: "" }
    fun getModelUrl(profile: String): Flow<String> = dataStore.data.map { 
        it[modelUrlKey(profile)] ?: LocalModelProfile.entries.find { p -> p.name == profile }?.suggestedPackUrl ?: ""
    }

    val readerFontSize: Flow<Float> = dataStore.data.map { it[KEY_READER_FONT_SIZE] ?: 18f }
    val readerLineHeight: Flow<Float> = dataStore.data.map { it[KEY_READER_LINE_HEIGHT] ?: 1.2f }
    val readerTheme: Flow<String> = dataStore.data.map { it[KEY_READER_THEME] ?: "dark" }
    val readerFontFamily: Flow<String> = dataStore.data.map { it[KEY_READER_FONT_FAMILY] ?: "serif" }
    val readerTtsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_READER_TTS_ENABLED] ?: true }
    val readerLanguage: Flow<String> = dataStore.data.map { it[KEY_READER_LANGUAGE] ?: "en" }
    val readerTranslationEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_READER_TRANSLATION_ENABLED] ?: false }
    val readerKeepScreenAwake: Flow<Boolean> = dataStore.data.map { it[KEY_READER_KEEP_SCREEN_AWAKE] ?: true }
    val readerPagingAnimation: Flow<Boolean> = dataStore.data.map { it[KEY_READER_PAGING_ANIMATION] ?: true }

    // Expanded Reader Preference Flows
    val readerOverrideBookFont: Flow<Boolean> = dataStore.data.map { it[KEY_READER_OVERRIDE_BOOK_FONT] ?: false }
    val readerDefaultFontSizeInt: Flow<Int> = dataStore.data.map { it[KEY_READER_DEFAULT_FONT_SIZE] ?: 18 }
    val readerMinFontSize: Flow<Int> = dataStore.data.map { it[KEY_READER_MIN_FONT_SIZE] ?: 12 }
    val readerFontWeight: Flow<Int> = dataStore.data.map { it[KEY_READER_FONT_WEIGHT] ?: 400 }
    val readerFontFaceSerif: Flow<String> = dataStore.data.map { it[KEY_READER_FONT_FACE_SERIF] ?: "Merriweather" }
    val readerFontFaceSans: Flow<String> = dataStore.data.map { it[KEY_READER_FONT_FACE_SANS] ?: "Inter" }
    val readerFontFaceMono: Flow<String> = dataStore.data.map { it[KEY_READER_FONT_FACE_MONO] ?: "Fira Code" }
    val readerOverrideBookLayout: Flow<Boolean> = dataStore.data.map { it[KEY_READER_OVERRIDE_BOOK_LAYOUT] ?: false }
    val readerUseBookLayout: Flow<Boolean> = dataStore.data.map { it[KEY_READER_USE_BOOK_LAYOUT] ?: true }
    val readerParagraphMargin: Flow<Float> = dataStore.data.map { it[KEY_READER_PARAGRAPH_MARGIN] ?: 1.2f }
    val readerLineSpacing: Flow<Float> = dataStore.data.map { it[KEY_READER_LINE_SPACING] ?: 1.4f }
    val readerWordSpacing: Flow<Int> = dataStore.data.map { it[KEY_READER_WORD_SPACING] ?: 0 }
    val readerLetterSpacing: Flow<Int> = dataStore.data.map { it[KEY_READER_LETTER_SPACING] ?: 0 }
    val readerTextIndent: Flow<Int> = dataStore.data.map { it[KEY_READER_TEXT_INDENT] ?: 0 }
    val readerFullJustification: Flow<Boolean> = dataStore.data.map { it[KEY_READER_FULL_JUSTIFICATION] ?: false }
    val readerHyphenation: Flow<Boolean> = dataStore.data.map { it[KEY_READER_HYPHENATION] ?: false }
    val readerActiveColorTheme: Flow<String> = dataStore.data.map { it[KEY_READER_ACTIVE_COLOR_THEME] ?: "Default" }
    val readerThemeMode: Flow<String> = dataStore.data.map { it[KEY_READER_THEME_MODE] ?: "Dark" }
    val readerInvertImageDark: Flow<Boolean> = dataStore.data.map { it[KEY_READER_INVERT_IMAGE_DARK] ?: true }
    val readerOverrideBookColor: Flow<Boolean> = dataStore.data.map { it[KEY_READER_OVERRIDE_BOOK_COLOR] ?: false }
    val readerActiveBackgroundIndex: Flow<Int> = dataStore.data.map { it[KEY_READER_ACTIVE_BACKGROUND_INDEX] ?: 0 }
    val readerHighlightColorSelection: Flow<String> = dataStore.data.map { it[KEY_READER_HIGHLIGHT_COLOR_SELECTION] ?: "Yellow" }
    val readerHighlightOpacity: Flow<Float> = dataStore.data.map { it[KEY_READER_HIGHLIGHT_OPACITY] ?: 0.4f }
    val readerEnableReadingRuler: Flow<Boolean> = dataStore.data.map { it[KEY_READER_ENABLE_READING_RULER] ?: false }
    val readerLinesToHighlight: Flow<Int> = dataStore.data.map { it[KEY_READER_LINES_TO_HIGHLIGHT] ?: 2 }
    val readerRulerColor: Flow<String> = dataStore.data.map { it[KEY_READER_RULER_COLOR] ?: "Yellow" }
    val readerRulerOpacity: Flow<Float> = dataStore.data.map { it[KEY_READER_RULER_OPACITY] ?: 0.5f }
    val readerEnableCodeHighlighting: Flow<Boolean> = dataStore.data.map { it[KEY_READER_ENABLE_CODE_HIGHLIGHTING] ?: false }
    val readerCodeLanguageSelection: Flow<String> = dataStore.data.map { it[KEY_READER_CODE_LANGUAGE_SELECTION] ?: "Auto-Detect" }
    val readerEInkMode: Flow<Boolean> = dataStore.data.map { it[KEY_READER_E_INK_MODE] ?: false }
    val readerTopMargin: Flow<Int> = dataStore.data.map { it[KEY_READER_TOP_MARGIN] ?: 16 }
    val readerBottomMargin: Flow<Int> = dataStore.data.map { it[KEY_READER_BOTTOM_MARGIN] ?: 16 }
    val readerLeftMargin: Flow<Int> = dataStore.data.map { it[KEY_READER_LEFT_MARGIN] ?: 16 }
    val readerRightMargin: Flow<Int> = dataStore.data.map { it[KEY_READER_RIGHT_MARGIN] ?: 16 }
    val readerShowHeader: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_HEADER] ?: true }
    val readerShowFooter: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_FOOTER] ?: true }
    val readerShowRemainingTime: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_REMAINING_TIME] ?: true }
    val readerShowRemainingPages: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_REMAINING_PAGES] ?: true }
    val readerShowReadingProgress: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_READING_PROGRESS] ?: true }
    val readerProgressStyle: Flow<String> = dataStore.data.map { it[KEY_READER_PROGRESS_STYLE] ?: "Page Number" }
    val readerShowProgressBar: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_PROGRESS_BAR] ?: true }
    val readerShowCurrentTime: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_CURRENT_TIME] ?: true }
    val readerShowBatteryStatus: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_BATTERY_STATUS] ?: true }
    val readerShowBatteryPercentage: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_BATTERY_PERCENTAGE] ?: true }
    val readerOrientationSelected: Flow<String> = dataStore.data.map { it[KEY_READER_ORIENTATION_SELECTED] ?: "Portrait" }
    val readerWritingMode: Flow<String> = dataStore.data.map { it[KEY_READER_WRITING_MODE] ?: "Horizontal" }
    val readerColumnGap: Flow<Int> = dataStore.data.map { it[KEY_READER_COLUMN_GAP] ?: 0 }
    val readerMaxColumns: Flow<Int> = dataStore.data.map { it[KEY_READER_MAX_COLUMNS] ?: 1 }
    val readerMaxColumnWidth: Flow<Int> = dataStore.data.map { it[KEY_READER_MAX_COLUMN_WIDTH] ?: 720 }
    val readerMaxColumnHeight: Flow<Int> = dataStore.data.map { it[KEY_READER_MAX_COLUMN_HEIGHT] ?: 1440 }
    val readerCustomContentCss: Flow<String> = dataStore.data.map { it[KEY_READER_CUSTOM_CONTENT_CSS] ?: "" }
    val readerCustomReaderUiCss: Flow<String> = dataStore.data.map { it[KEY_READER_CUSTOM_READER_UI_CSS] ?: "" }
    val readerAnimationStyle: Flow<String> = dataStore.data.map { it[KEY_READER_ANIMATION_STYLE] ?: "Push Fade" }
    val readerSystemScreenBrightness: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SYSTEM_SCREEN_BRIGHTNESS] ?: false }
    val readerBrightnessValue: Flow<Int> = dataStore.data.map { it[KEY_READER_BRIGHTNESS_VALUE] ?: 100 }
    val readerAutoBrightness: Flow<Boolean> = dataStore.data.map { it[KEY_READER_AUTO_BRIGHTNESS] ?: true }
    val readerIsScrolledMode: Flow<Boolean> = dataStore.data.map { it[KEY_READER_IS_SCROLLED_MODE] ?: false }
    val readerIsAutoScrolling: Flow<Boolean> = dataStore.data.map { it[KEY_READER_IS_AUTO_SCROLLING] ?: false }
    val readerAutoscrollSpeed: Flow<Float> = dataStore.data.map { it[KEY_READER_AUTOSCROLL_SPEED] ?: 2.0f }
    val readerAutoscrollMode: Flow<String> = dataStore.data.map { it[KEY_READER_AUTOSCROLL_MODE] ?: "Line by Line" }
    val readerIsParagraphMode: Flow<Boolean> = dataStore.data.map { it[KEY_READER_IS_PARAGRAPH_MODE] ?: false }
    val readerIsSpeedReading: Flow<Boolean> = dataStore.data.map { it[KEY_READER_IS_SPEED_READING] ?: false }
    val readerIsAutoMode: Flow<Boolean> = dataStore.data.map { it[KEY_READER_IS_AUTO_MODE] ?: false }
    val readerSwipeBrightness: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SWIPE_BRIGHTNESS] ?: true }
    val readerColorEInkMode: Flow<Boolean> = dataStore.data.map { it[KEY_READER_COLOR_EINK_MODE] ?: false }
    val readerAllowJavaScript: Flow<Boolean> = dataStore.data.map { it[KEY_READER_ALLOW_JAVASCRIPT] ?: false }
    val readerWordLensEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_READER_WORD_LENS_ENABLED] ?: false }
    val readerShowSourceText: Flow<Boolean> = dataStore.data.map { it[KEY_READER_SHOW_SOURCE_TEXT] ?: true }
    val readerTtsTextOption: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_TEXT_OPTION] ?: "Source and Translated" }
    val readerTranslationService: Flow<String> = dataStore.data.map { it[KEY_READER_TRANSLATION_SERVICE] ?: "Azure Translator" }
    val readerTranslateToLanguage: Flow<String> = dataStore.data.map { it[KEY_READER_TRANSLATE_TO_LANGUAGE] ?: "English" }
    val readerTtsGranularity: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_GRANULARITY] ?: "Word" }
    val readerTtsStyle: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_STYLE] ?: "Highlighter" }
    val readerTtsColorTheme: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_COLOR_THEME] ?: "Yellow" }
    val readerTtsUpdateFrequency: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_UPDATE_FREQUENCY] ?: "Every Sentence" }
    val readerTtsLanguage: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_LANGUAGE] ?: "en-US" }
    val readerTtsSpeed: Flow<Float> = dataStore.data.map { it[KEY_READER_TTS_SPEED] ?: 1.0f }
    val readerTtsPitch: Flow<Float> = dataStore.data.map { it[KEY_READER_TTS_PITCH] ?: 1.0f }
    val readerTtsVoice: Flow<String> = dataStore.data.map { it[KEY_READER_TTS_VOICE] ?: "default" }

    // Chess Settings Flows
    val chessEngineVersion: Flow<String> = dataStore.data.map { it[KEY_CHESS_ENGINE_VERSION] ?: "SF17" }
    val chessTheme: Flow<String?> = dataStore.data.map { it[KEY_CHESS_THEME] }
    val chessPieceStyle: Flow<String> = dataStore.data.map { it[KEY_CHESS_PIECE_STYLE] ?: "cburnett" }
    val chessBotLevel: Flow<Int> = dataStore.data.map { it[KEY_CHESS_BOT_LEVEL] ?: 10 }
    val chessEngineSearchTime: Flow<Int> = dataStore.data.map { it[KEY_CHESS_ENGINE_SEARCH_TIME] ?: 6 }
    val chessEngineMultiPv: Flow<Int> = dataStore.data.map { it[KEY_CHESS_ENGINE_MULTI_PV] ?: 2 }
    val chessMagnifyPiece: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_MAGNIFY_PIECE] ?: true }
    val chessTargetCircle: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_TARGET_CIRCLE] ?: true }
    val chessPieceAnimation: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_PIECE_ANIMATION] ?: true }
    val chessImmersiveMode: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_IMMERSIVE_MODE] ?: false }
    val chessShowDestinations: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_SHOW_DESTINATIONS] ?: true }
    val chessShowHighlights: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_SHOW_HIGHLIGHTS] ?: true }
    val chessShowMoveList: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_SHOW_MOVE_LIST] ?: true }
    val chessShowMaterialDiff: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_SHOW_MATERIAL_DIFF] ?: true }
    val chessClockRight: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_CLOCK_RIGHT] ?: true }
    val chessPremoves: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_PREMOVES] ?: true }
    val chessConfirmResign: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_CONFIRM_RESIGN] ?: true }
    val chessTouchFeedback: Flow<Boolean> = dataStore.data.map { it[KEY_CHESS_TOUCH_FEEDBACK] ?: true }

    val lastReadBookId: Flow<String?> = dataStore.data.map { it[KEY_LAST_READ_BOOK_ID] }

    suspend fun setGeminiApiKey(key: String) = dataStore.edit { it[KEY_GEMINI_API_KEY] = key }
    suspend fun setUserName(name: String) = dataStore.edit { it[KEY_USER_NAME] = name }
    suspend fun setTheme(theme: String) = dataStore.edit { it[KEY_THEME] = theme }
    suspend fun setNotificationsEnabled(enabled: Boolean) = dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    suspend fun setVoiceEnabled(enabled: Boolean) = dataStore.edit { it[KEY_VOICE_ENABLED] = enabled }
    suspend fun setTtsEnabled(enabled: Boolean) = dataStore.edit { it[KEY_TTS_ENABLED] = enabled }

    suspend fun setHfToken(token: String) = dataStore.edit { it[KEY_HF_TOKEN] = token }
    suspend fun setAiProvider(provider: String) = dataStore.edit { it[KEY_AI_PROVIDER] = provider }
    suspend fun setLocalModel(model: String) = dataStore.edit { it[KEY_LOCAL_MODEL] = model }
    
    suspend fun setModelStatus(profile: String, status: String) = dataStore.edit { it[modelStatusKey(profile)] = status }
    suspend fun setModelPath(profile: String, path: String) = dataStore.edit { it[modelPathKey(profile)] = path }
    suspend fun setModelError(profile: String, error: String) = dataStore.edit { it[modelErrorKey(profile)] = error }
    suspend fun setModelUrl(profile: String, url: String) = dataStore.edit { it[modelUrlKey(profile)] = url }

    suspend fun setReaderFontSize(size: Float) = dataStore.edit { it[KEY_READER_FONT_SIZE] = size }
    suspend fun setReaderLineHeight(height: Float) = dataStore.edit { it[KEY_READER_LINE_HEIGHT] = height }
    suspend fun setReaderTheme(theme: String) = dataStore.edit { it[KEY_READER_THEME] = theme }
    suspend fun setReaderFontFamily(family: String) = dataStore.edit { it[KEY_READER_FONT_FAMILY] = family }
    suspend fun setReaderTtsEnabled(enabled: Boolean) = dataStore.edit { it[KEY_READER_TTS_ENABLED] = enabled }
    suspend fun setReaderLanguage(language: String) = dataStore.edit { it[KEY_READER_LANGUAGE] = language }
    suspend fun setReaderTranslationEnabled(enabled: Boolean) = dataStore.edit { it[KEY_READER_TRANSLATION_ENABLED] = enabled }
    suspend fun setReaderKeepScreenAwake(enabled: Boolean) = dataStore.edit { it[KEY_READER_KEEP_SCREEN_AWAKE] = enabled }
    suspend fun setReaderPagingAnimation(enabled: Boolean) = dataStore.edit { it[KEY_READER_PAGING_ANIMATION] = enabled }

    // Expanded Reader Preference Setters
    suspend fun setReaderOverrideBookFont(value: Boolean) = dataStore.edit { it[KEY_READER_OVERRIDE_BOOK_FONT] = value }
    suspend fun setReaderDefaultFontSizeInt(value: Int) = dataStore.edit { it[KEY_READER_DEFAULT_FONT_SIZE] = value }
    suspend fun setReaderMinFontSize(value: Int) = dataStore.edit { it[KEY_READER_MIN_FONT_SIZE] = value }
    suspend fun setReaderFontWeight(value: Int) = dataStore.edit { it[KEY_READER_FONT_WEIGHT] = value }
    suspend fun setReaderFontFaceSerif(value: String) = dataStore.edit { it[KEY_READER_FONT_FACE_SERIF] = value }
    suspend fun setReaderFontFaceSans(value: String) = dataStore.edit { it[KEY_READER_FONT_FACE_SANS] = value }
    suspend fun setReaderFontFaceMono(value: String) = dataStore.edit { it[KEY_READER_FONT_FACE_MONO] = value }
    suspend fun setReaderOverrideBookLayout(value: Boolean) = dataStore.edit { it[KEY_READER_OVERRIDE_BOOK_LAYOUT] = value }
    suspend fun setReaderUseBookLayout(value: Boolean) = dataStore.edit { it[KEY_READER_USE_BOOK_LAYOUT] = value }
    suspend fun setReaderParagraphMargin(value: Float) = dataStore.edit { it[KEY_READER_PARAGRAPH_MARGIN] = value }
    suspend fun setReaderLineSpacing(value: Float) = dataStore.edit { it[KEY_READER_LINE_SPACING] = value }
    suspend fun setReaderWordSpacing(value: Int) = dataStore.edit { it[KEY_READER_WORD_SPACING] = value }
    suspend fun setReaderLetterSpacing(value: Int) = dataStore.edit { it[KEY_READER_LETTER_SPACING] = value }
    suspend fun setReaderTextIndent(value: Int) = dataStore.edit { it[KEY_READER_TEXT_INDENT] = value }
    suspend fun setReaderFullJustification(value: Boolean) = dataStore.edit { it[KEY_READER_FULL_JUSTIFICATION] = value }
    suspend fun setReaderHyphenation(value: Boolean) = dataStore.edit { it[KEY_READER_HYPHENATION] = value }
    suspend fun setReaderActiveColorTheme(value: String) = dataStore.edit { it[KEY_READER_ACTIVE_COLOR_THEME] = value }
    suspend fun setReaderThemeMode(value: String) = dataStore.edit { it[KEY_READER_THEME_MODE] = value }
    suspend fun setReaderInvertImageDark(value: Boolean) = dataStore.edit { it[KEY_READER_INVERT_IMAGE_DARK] = value }
    suspend fun setReaderOverrideBookColor(value: Boolean) = dataStore.edit { it[KEY_READER_OVERRIDE_BOOK_COLOR] = value }
    suspend fun setReaderActiveBackgroundIndex(value: Int) = dataStore.edit { it[KEY_READER_ACTIVE_BACKGROUND_INDEX] = value }
    suspend fun setReaderHighlightColorSelection(value: String) = dataStore.edit { it[KEY_READER_HIGHLIGHT_COLOR_SELECTION] = value }
    suspend fun setReaderHighlightOpacity(value: Float) = dataStore.edit { it[KEY_READER_HIGHLIGHT_OPACITY] = value }
    suspend fun setReaderEnableReadingRuler(value: Boolean) = dataStore.edit { it[KEY_READER_ENABLE_READING_RULER] = value }
    suspend fun setReaderLinesToHighlight(value: Int) = dataStore.edit { it[KEY_READER_LINES_TO_HIGHLIGHT] = value }
    suspend fun setReaderRulerColor(value: String) = dataStore.edit { it[KEY_READER_RULER_COLOR] = value }
    suspend fun setReaderRulerOpacity(value: Float) = dataStore.edit { it[KEY_READER_RULER_OPACITY] = value }
    suspend fun setReaderEnableCodeHighlighting(value: Boolean) = dataStore.edit { it[KEY_READER_ENABLE_CODE_HIGHLIGHTING] = value }
    suspend fun setReaderCodeLanguageSelection(value: String) = dataStore.edit { it[KEY_READER_CODE_LANGUAGE_SELECTION] = value }
    suspend fun setReaderEInkMode(value: Boolean) = dataStore.edit { it[KEY_READER_E_INK_MODE] = value }
    suspend fun setReaderTopMargin(value: Int) = dataStore.edit { it[KEY_READER_TOP_MARGIN] = value }
    suspend fun setReaderBottomMargin(value: Int) = dataStore.edit { it[KEY_READER_BOTTOM_MARGIN] = value }
    suspend fun setReaderLeftMargin(value: Int) = dataStore.edit { it[KEY_READER_LEFT_MARGIN] = value }
    suspend fun setReaderRightMargin(value: Int) = dataStore.edit { it[KEY_READER_RIGHT_MARGIN] = value }
    suspend fun setReaderShowHeader(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_HEADER] = value }
    suspend fun setReaderShowFooter(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_FOOTER] = value }
    suspend fun setReaderShowRemainingTime(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_REMAINING_TIME] = value }
    suspend fun setReaderShowRemainingPages(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_REMAINING_PAGES] = value }
    suspend fun setReaderShowReadingProgress(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_READING_PROGRESS] = value }
    suspend fun setReaderProgressStyle(value: String) = dataStore.edit { it[KEY_READER_PROGRESS_STYLE] = value }
    suspend fun setReaderShowProgressBar(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_PROGRESS_BAR] = value }
    suspend fun setReaderShowCurrentTime(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_CURRENT_TIME] = value }
    suspend fun setReaderShowBatteryStatus(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_BATTERY_STATUS] = value }
    suspend fun setReaderShowBatteryPercentage(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_BATTERY_PERCENTAGE] = value }
    suspend fun setReaderOrientationSelected(value: String) = dataStore.edit { it[KEY_READER_ORIENTATION_SELECTED] = value }
    suspend fun setReaderWritingMode(value: String) = dataStore.edit { it[KEY_READER_WRITING_MODE] = value }
    suspend fun setReaderColumnGap(value: Int) = dataStore.edit { it[KEY_READER_COLUMN_GAP] = value }
    suspend fun setReaderMaxColumns(value: Int) = dataStore.edit { it[KEY_READER_MAX_COLUMNS] = value }
    suspend fun setReaderMaxColumnWidth(value: Int) = dataStore.edit { it[KEY_READER_MAX_COLUMN_WIDTH] = value }
    suspend fun setReaderMaxColumnHeight(value: Int) = dataStore.edit { it[KEY_READER_MAX_COLUMN_HEIGHT] = value }
    suspend fun setReaderCustomContentCss(value: String) = dataStore.edit { it[KEY_READER_CUSTOM_CONTENT_CSS] = value }
    suspend fun setReaderCustomReaderUiCss(value: String) = dataStore.edit { it[KEY_READER_CUSTOM_READER_UI_CSS] = value }
    suspend fun setReaderAnimationStyle(value: String) = dataStore.edit { it[KEY_READER_ANIMATION_STYLE] = value }
    suspend fun setReaderSystemScreenBrightness(value: Boolean) = dataStore.edit { it[KEY_READER_SYSTEM_SCREEN_BRIGHTNESS] = value }
    suspend fun setReaderBrightnessValue(value: Int) = dataStore.edit { it[KEY_READER_BRIGHTNESS_VALUE] = value }
    suspend fun setReaderAutoBrightness(value: Boolean) = dataStore.edit { it[KEY_READER_AUTO_BRIGHTNESS] = value }
    suspend fun setReaderIsScrolledMode(value: Boolean) = dataStore.edit { it[KEY_READER_IS_SCROLLED_MODE] = value }
    suspend fun setReaderIsAutoScrolling(value: Boolean) = dataStore.edit { it[KEY_READER_IS_AUTO_SCROLLING] = value }
    suspend fun setReaderAutoscrollSpeed(value: Float) = dataStore.edit { it[KEY_READER_AUTOSCROLL_SPEED] = value }
    suspend fun setReaderAutoscrollMode(value: String) = dataStore.edit { it[KEY_READER_AUTOSCROLL_MODE] = value }
    suspend fun setReaderIsParagraphMode(value: Boolean) = dataStore.edit { it[KEY_READER_IS_PARAGRAPH_MODE] = value }
    suspend fun setReaderIsSpeedReading(value: Boolean) = dataStore.edit { it[KEY_READER_IS_SPEED_READING] = value }
    suspend fun setReaderIsAutoMode(value: Boolean) = dataStore.edit { it[KEY_READER_IS_AUTO_MODE] = value }
    suspend fun setReaderSwipeBrightness(value: Boolean) = dataStore.edit { it[KEY_READER_SWIPE_BRIGHTNESS] = value }
    suspend fun setReaderColorEInkMode(value: Boolean) = dataStore.edit { it[KEY_READER_COLOR_EINK_MODE] = value }
    suspend fun setReaderAllowJavaScript(value: Boolean) = dataStore.edit { it[KEY_READER_ALLOW_JAVASCRIPT] = value }
    suspend fun setReaderWordLensEnabled(value: Boolean) = dataStore.edit { it[KEY_READER_WORD_LENS_ENABLED] = value }
    suspend fun setReaderShowSourceText(value: Boolean) = dataStore.edit { it[KEY_READER_SHOW_SOURCE_TEXT] = value }
    suspend fun setReaderTtsTextOption(value: String) = dataStore.edit { it[KEY_READER_TTS_TEXT_OPTION] = value }
    suspend fun setReaderTranslationService(value: String) = dataStore.edit { it[KEY_READER_TRANSLATION_SERVICE] = value }
    suspend fun setReaderTranslateToLanguage(value: String) = dataStore.edit { it[KEY_READER_TRANSLATE_TO_LANGUAGE] = value }
    suspend fun setReaderTtsGranularity(value: String) = dataStore.edit { it[KEY_READER_TTS_GRANULARITY] = value }
    suspend fun setReaderTtsStyle(value: String) = dataStore.edit { it[KEY_READER_TTS_STYLE] = value }
    suspend fun setReaderTtsColorTheme(value: String) = dataStore.edit { it[KEY_READER_TTS_COLOR_THEME] = value }
    suspend fun setReaderTtsUpdateFrequency(value: String) = dataStore.edit { it[KEY_READER_TTS_UPDATE_FREQUENCY] = value }
    suspend fun setReaderTtsLanguage(value: String) = dataStore.edit { it[KEY_READER_TTS_LANGUAGE] = value }
    suspend fun setReaderTtsSpeed(value: Float) = dataStore.edit { it[KEY_READER_TTS_SPEED] = value }
    suspend fun setReaderTtsPitch(value: Float) = dataStore.edit { it[KEY_READER_TTS_PITCH] = value }
    suspend fun setReaderTtsVoice(value: String) = dataStore.edit { it[KEY_READER_TTS_VOICE] = value }

    // Chess Settings Setters
    suspend fun setChessEngineVersion(version: String) = dataStore.edit { it[KEY_CHESS_ENGINE_VERSION] = version }
    suspend fun setChessTheme(theme: String) = dataStore.edit { it[KEY_CHESS_THEME] = theme }
    suspend fun setChessPieceStyle(style: String) = dataStore.edit { it[KEY_CHESS_PIECE_STYLE] = style }
    suspend fun setChessBotLevel(level: Int) = dataStore.edit { it[KEY_CHESS_BOT_LEVEL] = level }
    suspend fun setChessEngineSearchTime(time: Int) = dataStore.edit { it[KEY_CHESS_ENGINE_SEARCH_TIME] = time }
    suspend fun setChessEngineMultiPv(lines: Int) = dataStore.edit { it[KEY_CHESS_ENGINE_MULTI_PV] = lines }
    suspend fun setChessMagnifyPiece(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_MAGNIFY_PIECE] = enabled }
    suspend fun setChessTargetCircle(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_TARGET_CIRCLE] = enabled }
    suspend fun setChessPieceAnimation(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_PIECE_ANIMATION] = enabled }
    suspend fun setChessImmersiveMode(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_IMMERSIVE_MODE] = enabled }
    suspend fun setChessShowDestinations(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_SHOW_DESTINATIONS] = enabled }
    suspend fun setChessShowHighlights(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_SHOW_HIGHLIGHTS] = enabled }
    suspend fun setChessShowMoveList(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_SHOW_MOVE_LIST] = enabled }
    suspend fun setChessShowMaterialDiff(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_SHOW_MATERIAL_DIFF] = enabled }
    suspend fun setChessClockRight(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_CLOCK_RIGHT] = enabled }
    suspend fun setChessPremoves(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_PREMOVES] = enabled }
    suspend fun setChessConfirmResign(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_CONFIRM_RESIGN] = enabled }
    suspend fun setChessTouchFeedback(enabled: Boolean) = dataStore.edit { it[KEY_CHESS_TOUCH_FEEDBACK] = enabled }

    suspend fun setLichessUsername(username: String) = dataStore.edit { it[KEY_CHESS_LICHESS_USERNAME] = username }
    suspend fun setChessComUsername(username: String) = dataStore.edit { it[KEY_CHESS_CHESSCOM_USERNAME] = username }

    val lichessUsername: Flow<String> = dataStore.data.map { it[KEY_CHESS_LICHESS_USERNAME] ?: "" }
    val chessComUsername: Flow<String> = dataStore.data.map { it[KEY_CHESS_CHESSCOM_USERNAME] ?: "" }

    val lastChessGameFen: Flow<String> = dataStore.data.map { it[KEY_CHESS_LAST_GAME_FEN] ?: "" }
    val lastChessGamePgn: Flow<String> = dataStore.data.map { it[KEY_CHESS_LAST_GAME_PGN] ?: "" }
    val lastChessGameTitle: Flow<String> = dataStore.data.map { it[KEY_CHESS_LAST_GAME_TITLE] ?: "Stockfish Bot Match" }
    val lastChessGameDate: Flow<Long> = dataStore.data.map { it[KEY_CHESS_LAST_GAME_DATE] ?: 0L }
    val lastChessGameMode: Flow<String> = dataStore.data.map { it[KEY_CHESS_LAST_GAME_MODE] ?: "VS_BOT" }

    suspend fun saveLastChessGame(fen: String, pgn: String, title: String, mode: String) = dataStore.edit {
        it[KEY_CHESS_LAST_GAME_FEN] = fen
        it[KEY_CHESS_LAST_GAME_PGN] = pgn
        it[KEY_CHESS_LAST_GAME_TITLE] = title
        it[KEY_CHESS_LAST_GAME_DATE] = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        it[KEY_CHESS_LAST_GAME_MODE] = mode
    }

    suspend fun setLastReadBookId(bookId: String?) = dataStore.edit {
        if (bookId == null) it.remove(KEY_LAST_READ_BOOK_ID) else it[KEY_LAST_READ_BOOK_ID] = bookId
    }
}

// Platform-specific DataStore creation
expect fun createDataStore(): DataStore<Preferences>
