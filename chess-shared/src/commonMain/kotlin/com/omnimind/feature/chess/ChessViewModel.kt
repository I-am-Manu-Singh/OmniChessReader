package com.creativeblisslabs.omnimind.feature.chess

import com.creativeblisslabs.omnimind.core.chess.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creativeblisslabs.omnimind.chess.shared.*
import com.creativeblisslabs.omnimind.core.chess.ChessLogic
import com.creativeblisslabs.omnimind.chess.shared.logic.ChessAssetLoader
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelProfile
import com.creativeblisslabs.omnimind.core.data.ai.LocalModelType
import com.creativeblisslabs.omnimind.core.data.ai.ModelPackInfo
import com.creativeblisslabs.omnimind.core.data.ai.ModelPackManager
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketMessage
import com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.random.Random
import kotlin.math.pow
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import io.ktor.http.*
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume

import com.creativeblisslabs.omnimind.core.media.MediaProcessor
import com.creativeblisslabs.omnimind.core.media.MediaAttachmentUi
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

class ChessViewModel(
    private val chessEngine: ChessEngine,
    private val openingRepository: OpeningRepository,
    private val assetLoader: ChessAssetLoader,
    private val appPreferences: AppPreferences,
    private val modelPackManager: ModelPackManager,
    private val mediaProcessor: MediaProcessor,
    private val socketService: ChessSocketService,
    private val aiService: com.creativeblisslabs.omnimind.core.data.ai.OmniAiService? = null
) : ViewModel() {
private val _fenHistory = mutableListOf<String>()
private val _uciHistory = mutableListOf<String>()
// RedoEntry stores full state needed to redo a move
private data class RedoEntry(val fen: String, val san: String, val uci: String)
private val _redoStack = mutableListOf<RedoEntry>()

private val _moveHistory = MutableStateFlow<List<String>>(emptyList())
val moveHistory: StateFlow<List<String>> = _moveHistory.asStateFlow()

private val _hasRedo = MutableStateFlow(false)
val hasRedo: StateFlow<Boolean> = _hasRedo.asStateFlow()

private fun updateRedoState() {
    _hasRedo.value = _redoStack.isNotEmpty()
}

private val _fen = MutableStateFlow(ChessLogic.getInitialFen())
val fen: StateFlow<String> = _fen.asStateFlow()
    private val _evaluation = MutableStateFlow<EngineEvaluation?>(null)
    val evaluation: StateFlow<EngineEvaluation?> = _evaluation

    // Themes
    private val _selectedTheme = MutableStateFlow(ChessTheme("Wood", 0xFFEADECA, 0xFF8B5A2B, false))
    val selectedTheme: StateFlow<ChessTheme> = _selectedTheme

    // Timer configs
    private val _whiteTimer = MutableStateFlow(ChessTimer(600000, 0, 600000))
    val whiteTimer: StateFlow<ChessTimer> = _whiteTimer

    private val _blackTimer = MutableStateFlow(ChessTimer(600000, 0, 600000))
    val blackTimer: StateFlow<ChessTimer> = _blackTimer

    private val _activeTimeFormat = MutableStateFlow("10m")
    val activeTimeFormat: StateFlow<String> = _activeTimeFormat

    // Ratings & Profile
    private val _currentUserRating = MutableStateFlow(
        ChessUser("", "", 0, 0, 0, 0, 0, "", "")
    )
    val currentUserRating: StateFlow<ChessUser> = _currentUserRating

    // Game modes & state
    private val _gameMode = MutableStateFlow(GameMode.VS_BOT)
    val gameMode: StateFlow<GameMode> = _gameMode

    private val _isPlayerWhite = MutableStateFlow(true)
    val isPlayerWhite: StateFlow<Boolean> = _isPlayerWhite

    private val _botLevel = MutableStateFlow(6) // 1 to 20
    val botLevel: StateFlow<Int> = _botLevel

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive

    // Move analysis & graphs
    private val _moveAnalysis = MutableStateFlow<List<MoveAnalysis>>(emptyList())
    val moveAnalysis: StateFlow<List<MoveAnalysis>> = _moveAnalysis

    private val _gameAnalysisResult = MutableStateFlow<GameAnalysisResult?>(null)
    val gameAnalysisResult: StateFlow<GameAnalysisResult?> = _gameAnalysisResult

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()

    // Threat map
    private val _threatMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val threatMap: StateFlow<Map<String, Int>> = _threatMap

    private val _showThreatMap = MutableStateFlow(false)
    val showThreatMap: StateFlow<Boolean> = _showThreatMap

    // Friend Challenge States
    private val _friendInviteInfo = MutableStateFlow<com.creativeblisslabs.omnimind.feature.chess.network.FriendInviteInfo?>(null)
    val friendInviteInfo: StateFlow<com.creativeblisslabs.omnimind.feature.chess.network.FriendInviteInfo?> = _friendInviteInfo.asStateFlow()

    private val _isWaitingForFriend = MutableStateFlow(false)
    val isWaitingForFriend: StateFlow<Boolean> = _isWaitingForFriend.asStateFlow()

    private val _friendOpponentName = MutableStateFlow<String?>(null)
    val friendOpponentName: StateFlow<String?> = _friendOpponentName.asStateFlow()

    private val _showEngineAnalysis = MutableStateFlow(true)
    val showEngineAnalysis: StateFlow<Boolean> = _showEngineAnalysis

    // Premoves & Arrow markings
    private val _premove = MutableStateFlow<Pair<String, String>?>(null) // from, to
    val premove: StateFlow<Pair<String, String>?> = _premove

    private val _visualArrows = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val visualArrows: StateFlow<List<Pair<String, String>>> = _visualArrows

    // Puzzles
    private val _puzzles = MutableStateFlow<List<ChessPuzzle>>(emptyList())
    val puzzles: StateFlow<List<ChessPuzzle>> = _puzzles

    private val _currentPuzzleIndex = MutableStateFlow(0)
    val currentPuzzleIndex: StateFlow<Int> = _currentPuzzleIndex

    private val _puzzleFeedback = MutableStateFlow("")
    val puzzleFeedback: StateFlow<String> = _puzzleFeedback

    private val _puzzleStormTimeLeft = MutableStateFlow(180)
    val puzzleStormTimeLeft: StateFlow<Int> = _puzzleStormTimeLeft

    private val _puzzleStreak = MutableStateFlow(0)
    val puzzleStreak: StateFlow<Int> = _puzzleStreak

    private val _puzzleStormScore = MutableStateFlow(0)
    val puzzleStormScore: StateFlow<Int> = _puzzleStormScore

    private var puzzleStormJob: Job? = null

    // Lessons
    private val _lessons = MutableStateFlow<List<LessonTopic>>(emptyList())
    val lessons: StateFlow<List<LessonTopic>> = _lessons

    private val _currentLessonIndex = MutableStateFlow(0)
    val currentLessonIndex: StateFlow<Int> = _currentLessonIndex

    private val _lessonStepIndex = MutableStateFlow(0)
    val lessonStepIndex: StateFlow<Int> = _lessonStepIndex

    // Tournaments & Clubs
    private val _tournaments = MutableStateFlow<List<ChessTournament>>(emptyList())
    val tournaments: StateFlow<List<ChessTournament>> = _tournaments

    // Social Friends
    private val _friends = MutableStateFlow<List<ChessFriend>>(emptyList())
    val friends: StateFlow<List<ChessFriend>> = _friends

    private val _pieceStyle = MutableStateFlow("CBurnett")
    val pieceStyle: StateFlow<String> = _pieceStyle

    fun setPieceStyle(style: String) {
        _pieceStyle.value = style
        viewModelScope.launch {
            appPreferences.setChessPieceStyle(style)
        }
    }

    // --- Lichess Live Data ---
    private val _lichessPuzzle = MutableStateFlow<LichessPuzzleData?>(null)
    val lichessPuzzle: StateFlow<LichessPuzzleData?> = _lichessPuzzle

    private val _lichessLeaderboard = MutableStateFlow<List<LichessLeaderEntry>>(emptyList())
    val lichessLeaderboard: StateFlow<List<LichessLeaderEntry>> = _lichessLeaderboard

    private val _lichessLiveGames = MutableStateFlow<List<LichessGame>>(emptyList())
    val lichessLiveGames: StateFlow<List<LichessGame>> = _lichessLiveGames

    private val _lichessStreamEvents = MutableStateFlow<List<LichessStreamEvent>>(emptyList())
    val lichessStreamEvents: StateFlow<List<LichessStreamEvent>> = _lichessStreamEvents

    private val _gameHistory = MutableStateFlow<List<com.creativeblisslabs.omnimind.core.chess.SavedGame>>(emptyList())
    val gameHistory: StateFlow<List<com.creativeblisslabs.omnimind.core.chess.SavedGame>> = _gameHistory

    private val _lichessFetchError = MutableStateFlow<String?>(null)
    val lichessFetchError: StateFlow<String?> = _lichessFetchError

    private val _chessComPuzzle = MutableStateFlow<ChessComDailyPuzzle?>(null)
    val chessComPuzzle: StateFlow<ChessComDailyPuzzle?> = _chessComPuzzle.asStateFlow()

    private val _isLichessLoading = MutableStateFlow(false)
    val isLichessLoading: StateFlow<Boolean> = _isLichessLoading

    // --- Engine Settings ---
    private val _engineSettings = MutableStateFlow(EngineSettings())
    val engineSettings: StateFlow<EngineSettings> = _engineSettings

    private val _isLearningMode = MutableStateFlow(false)
    val isLearningMode: StateFlow<Boolean> = _isLearningMode.asStateFlow()

    private val _engineDownloadProgress = MutableStateFlow<Float?>(null)
    val engineDownloadProgress: StateFlow<Float?> = _engineDownloadProgress

    private val _isBotThinking = MutableStateFlow(false)
    val isBotThinking: StateFlow<Boolean> = _isBotThinking

    // Display & Interaction Settings
    private val _magnifyPiece = MutableStateFlow(true)
    val magnifyPiece: StateFlow<Boolean> = _magnifyPiece

    private val _targetCircle = MutableStateFlow(true)
    val targetCircle: StateFlow<Boolean> = _targetCircle

    private val _pieceAnimation = MutableStateFlow(true)
    val pieceAnimation: StateFlow<Boolean> = _pieceAnimation

    private val _immersiveMode = MutableStateFlow(false)
    val immersiveMode: StateFlow<Boolean> = _immersiveMode

    private val _showDestinations = MutableStateFlow(true)
    val showDestinations: StateFlow<Boolean> = _showDestinations

    private val _showHighlights = MutableStateFlow(true)
    val showHighlights: StateFlow<Boolean> = _showHighlights

    private val _showMoveList = MutableStateFlow(true)
    val showMoveList: StateFlow<Boolean> = _showMoveList

    private val _showMaterialDiff = MutableStateFlow(true)
    val showMaterialDiff: StateFlow<Boolean> = _showMaterialDiff

    private val _clockRight = MutableStateFlow(true)
    val clockRight: StateFlow<Boolean> = _clockRight

    private val _premovesEnabled = MutableStateFlow(true)
    val premovesEnabled: StateFlow<Boolean> = _premovesEnabled

    private val _confirmResign = MutableStateFlow(true)
    val confirmResign: StateFlow<Boolean> = _confirmResign

    private val _touchFeedback = MutableStateFlow(true)
    val touchFeedback: StateFlow<Boolean> = _touchFeedback

    private val _activeGameSession = MutableStateFlow<GameSession?>(null)
    val activeGameSession: StateFlow<GameSession?> = _activeGameSession

    private val _gameVariant = MutableStateFlow(GameVariant.CLASSIC)
    val gameVariant: StateFlow<GameVariant> = _gameVariant

    private val _openingExplorerData = MutableStateFlow<LichessOpeningData?>(null)
    val openingExplorerData: StateFlow<LichessOpeningData?> = _openingExplorerData

    private val _localOpeningInfo = MutableStateFlow<OpeningPosition?>(null)
    val localOpeningInfo: StateFlow<OpeningPosition?> = _localOpeningInfo

    private val _openingCategories = MutableStateFlow<List<OpeningCategory>>(emptyList())
    val openingCategories: StateFlow<List<OpeningCategory>> = _openingCategories

    private val _endgamePositions = MutableStateFlow<List<EndgamePosition>>(emptyList())
    val endgamePositions: StateFlow<List<EndgamePosition>> = _endgamePositions

    private val _onlineGameId = MutableStateFlow<String?>(null)
    val onlineGameId: StateFlow<String?> = _onlineGameId

    private val _socketError = MutableStateFlow<String?>(null)
    val socketError: StateFlow<String?> = _socketError

    private val _enginePacks = MutableStateFlow<List<ModelPackInfo>>(emptyList())
    val enginePacks: StateFlow<List<ModelPackInfo>> = _enginePacks.asStateFlow()

    private val _allModelPacks = MutableStateFlow<List<ModelPackInfo>>(emptyList())
    val allModelPacks: StateFlow<List<ModelPackInfo>> = _allModelPacks.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            })
        }
        install(DefaultRequest) {
            header("User-Agent", "OmniMind-Chess-App/1.0 (contact: support@omnimind.com)")
            header("Accept", "application/json")
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }
    }

    private var timerJob: Job? = null

    private val boardThemes = listOf(
        ChessTheme("Wood", 0xFFF0D9B5, 0xFFB58863, false),
        ChessTheme("Green", 0xFFEEEED2, 0xFF769656, false),
        ChessTheme("Canvas", 0x33FFFFFF, 0x0DFFFFFF, false),
        ChessTheme("Blue", 0xFF00FFCC, 0xFF3D0066, false),
        ChessTheme("Maple", 0xFFF5E6CC, 0xFFC69C6D, false),
        ChessTheme("Walnut", 0xFFE0C49F, 0xFF6D4C41, false),
        ChessTheme("Dark-Wood", 0xFFE0C49F, 0xFF6D4C41, false),
        ChessTheme("Marble", 0xFFE0E0E0, 0xFF616161, false),
        ChessTheme("Newspaper", 0xFFF0F0F0, 0xFF424242, false)
    )
    private val _userProfile = MutableStateFlow(
        ChessUser("", "", 0, 0, 0, 0, 0, "", "")
    )
    val userProfile: StateFlow<ChessUser> = _userProfile

    private val _chessComProfile = MutableStateFlow(
        ChessUser("", "", 0, 0, 0, 0, 0, "", "")
    )
    val chessComProfile: StateFlow<ChessUser> = _chessComProfile
    fun fetchDualPlatformProfile(usernameInput: String, platform: String = "Both") {
        val username = usernameInput.ifBlank { "magnuson-carlsium" }.trim()
        viewModelScope.launch {
            _isLichessLoading.value = true
            _lichessFetchError.value = null
            try {
                if (platform == "Lichess" || platform == "Both") {
                    // Lichess Public API: https://lichess.org/api/user/{username}
                    val response = httpClient.get("https://lichess.org/api/user/$username")
                    if (response.status.value in 200..299) {
                        val jsonStr = response.body<String>()
                        val jsonElement = Json.parseToJsonElement(jsonStr).jsonObject

                        val name = jsonElement["username"]?.jsonPrimitive?.content ?: username
                        val title = jsonElement["title"]?.jsonPrimitive?.content
                        val profileObj = jsonElement["profile"]?.jsonObject
                        val country = profileObj?.get("country")?.jsonPrimitive?.content ?: "INDIA"
                        val profileUrl = jsonElement["url"]?.jsonPrimitive?.content ?: "https://lichess.org/@/$name"

                        val perfsObj = jsonElement["perfs"]?.jsonObject
                        val bulletObj = perfsObj?.get("bullet")?.jsonObject
                        val blitzObj = perfsObj?.get("blitz")?.jsonObject
                        val rapidObj = perfsObj?.get("rapid")?.jsonObject
                        val classicalObj = perfsObj?.get("classical")?.jsonObject
                        val puzzleObj = perfsObj?.get("puzzle")?.jsonObject

                        val bulletRating = bulletObj?.get("rating")?.jsonPrimitive?.intOrNull
                        val bulletGames = bulletObj?.get("games")?.jsonPrimitive?.int ?: 0

                        val blitzRating = blitzObj?.get("rating")?.jsonPrimitive?.intOrNull
                        val blitzGames = blitzObj?.get("games")?.jsonPrimitive?.int ?: 0

                        val rapidRating = rapidObj?.get("rating")?.jsonPrimitive?.intOrNull
                        val rapidGames = rapidObj?.get("games")?.jsonPrimitive?.int ?: 0

                        val classical = classicalObj?.get("rating")?.jsonPrimitive?.intOrNull
                        val puzzleRating = puzzleObj?.get("rating")?.jsonPrimitive?.intOrNull

                        val countObj = jsonElement["count"]?.jsonObject
                        val totalGames = countObj?.get("all")?.jsonPrimitive?.int ?: 0
                        val wins = countObj?.get("win")?.jsonPrimitive?.int ?: 0
                        val losses = countObj?.get("loss")?.jsonPrimitive?.int ?: 0
                        val draws = countObj?.get("draw")?.jsonPrimitive?.int ?: 0

                        val playTimeObj = jsonElement["playTime"]?.jsonObject
                        val totalSeconds = playTimeObj?.get("total")?.jsonPrimitive?.double ?: 0.0
                        val playTimeHours = ((totalSeconds / 3600.0) * 10).toInt() / 10.0

                        val profileUser = ChessUser(
                            id = name,
                            username = name,
                            title = title,
                            bulletRating = bulletRating,
                            blitzRating = blitzRating,
                            rapidRating = rapidRating,
                            classicalRating = classical,
                            puzzleRating = puzzleRating,
                            country = country,
                            avatarUrl = "",
                            totalGames = totalGames,
                            wins = wins,
                            losses = losses,
                            draws = draws,
                            blitzGames = blitzGames,
                            bulletGames = bulletGames,
                            rapidGames = rapidGames,
                            playTimeHours = playTimeHours,
                            profileUrl = profileUrl
                        )
                        _userProfile.value = profileUser
                        _currentUserRating.value = profileUser
                        appPreferences.setLichessUsername(name)
                    }
                }

                if (platform == "Chess.com" || platform == "Both") {
                    // Fetch Chess.com profile details for avatar & country
                    var avatarUrl = ""
                    var countryCode = "IN"
                    val profileResp = httpClient.get("https://api.chess.com/pub/player/$username")
                    if (profileResp.status.value in 200..299) {
                        val pJson = Json.parseToJsonElement(profileResp.body<String>()).jsonObject
                        avatarUrl = pJson["avatar"]?.jsonPrimitive?.content ?: ""
                        val countryUrl = pJson["country"]?.jsonPrimitive?.content ?: ""
                        if (countryUrl.isNotBlank()) {
                            countryCode = countryUrl.substringAfterLast("/")
                        }
                    }

                    // Chess.com Stats API: https://api.chess.com/pub/player/{username}/stats
                    val response = httpClient.get("https://api.chess.com/pub/player/$username/stats")
                    if (response.status.value in 200..299) {
                        val jsonStr = response.body<String>()
                        val jsonElement = Json.parseToJsonElement(jsonStr).jsonObject

                        val bulletObj = jsonElement["chess_bullet"]?.jsonObject
                        val blitzObj = jsonElement["chess_blitz"]?.jsonObject
                        val rapidObj = jsonElement["chess_rapid"]?.jsonObject
                        val tacticsObj = jsonElement["tactics"]?.jsonObject

                        val chessBullet = bulletObj?.get("last")?.jsonObject?.get("rating")?.jsonPrimitive?.intOrNull
                        val chessBlitz = blitzObj?.get("last")?.jsonObject?.get("rating")?.jsonPrimitive?.intOrNull
                        val chessRapid = rapidObj?.get("last")?.jsonObject?.get("rating")?.jsonPrimitive?.intOrNull
                        val tacticsRating = tacticsObj?.get("highest")?.jsonObject?.get("rating")?.jsonPrimitive?.intOrNull

                        val bulletRecord = bulletObj?.get("record")?.jsonObject
                        val blitzRecord = blitzObj?.get("record")?.jsonObject
                        val rapidRecord = rapidObj?.get("record")?.jsonObject

                        val bulletWins = bulletRecord?.get("win")?.jsonPrimitive?.int ?: 0
                        val bulletLosses = bulletRecord?.get("loss")?.jsonPrimitive?.int ?: 0
                        val bulletDraws = bulletRecord?.get("draw")?.jsonPrimitive?.int ?: 0

                        val blitzWins = blitzRecord?.get("win")?.jsonPrimitive?.int ?: 0
                        val blitzLosses = blitzRecord?.get("loss")?.jsonPrimitive?.int ?: 0
                        val blitzDraws = blitzRecord?.get("draw")?.jsonPrimitive?.int ?: 0

                        val rapidWins = rapidRecord?.get("win")?.jsonPrimitive?.int ?: 0
                        val rapidLosses = rapidRecord?.get("loss")?.jsonPrimitive?.int ?: 0
                        val rapidDraws = rapidRecord?.get("draw")?.jsonPrimitive?.int ?: 0

                        val totalWins = bulletWins + blitzWins + rapidWins
                        val totalLosses = bulletLosses + blitzLosses + rapidLosses
                        val totalDraws = bulletDraws + blitzDraws + rapidDraws
                        val totalGames = totalWins + totalLosses + totalDraws

                        _chessComProfile.value = ChessUser(
                            id = username,
                            username = username,
                            title = null,
                            bulletRating = chessBullet,
                            blitzRating = chessBlitz,
                            rapidRating = chessRapid,
                            classicalRating = null,
                            puzzleRating = tacticsRating,
                            country = countryCode,
                            avatarUrl = avatarUrl,
                            totalGames = totalGames,
                            wins = totalWins,
                            losses = totalLosses,
                            draws = totalDraws,
                            blitzGames = blitzWins + blitzLosses + blitzDraws,
                            bulletGames = bulletWins + bulletLosses + bulletDraws,
                            rapidGames = rapidWins + rapidLosses + rapidDraws,
                            playTimeHours = 0.0,
                            profileUrl = "https://www.chess.com/member/$username"
                        )
                        appPreferences.setChessComUsername(username)
                    }
                }
            } catch (e: Exception) {
                _lichessFetchError.value = "Failed to fetch profile: ${e.message ?: "Unknown error"}"
            } finally {
                _isLichessLoading.value = false
            }
        }
    }

    fun fetchLichessTournaments() {
        viewModelScope.launch {
            try {
                val response = httpClient.get("https://lichess.org/api/tournament")
                if (response.status.value in 200..299) {
                    val jsonStr = response.body<String>()
                    val obj = Json.parseToJsonElement(jsonStr).jsonObject
                    val created = obj["created"]?.jsonArray ?: emptyList()
                    val started = obj["started"]?.jsonArray ?: emptyList()

                    val allTourneys = (created + started).mapNotNull { item ->
                        try {
                            val tObj = item.jsonObject
                            val id = tObj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                            val fullName = tObj["fullName"]?.jsonPrimitive?.content ?: "Lichess Arena"
                            val mode = tObj["clock"]?.jsonObject?.get("limit")?.jsonPrimitive?.int ?: 180
                            val inc = tObj["clock"]?.jsonObject?.get("increment")?.jsonPrimitive?.int ?: 0
                            val system = tObj["system"]?.jsonPrimitive?.content ?: "arena"
                            val nbPlayers = tObj["nbPlayers"]?.jsonPrimitive?.int ?: 0
                            ChessTournament(
                                id = id,
                                title = "$fullName (${mode / 60}+$inc)",
                                startTime = System.currentTimeMillis() + 300000L,
                                playersCount = nbPlayers,
                                status = if (started.contains(item)) "Live" else "Upcoming",
                                standings = emptyList(),
                                isLiveShow = started.contains(item)
                            )
                        } catch (e: Exception) { null }
                    }

                    if (allTourneys.isNotEmpty()) {
                        _tournaments.value = allTourneys
                    }
                }
            } catch (e: Exception) {
                println("[LichessApi] Error fetching tournaments: ${e.message}")
            }
        }
    }

    fun fetchLichessFriends() {
        viewModelScope.launch {
            try {
                val response = httpClient.get("https://lichess.org/api/tv/channels")
                if (response.status.value in 200..299) {
                    val jsonStr = response.body<String>()
                    val obj = Json.parseToJsonElement(jsonStr).jsonObject
                    val topPlayers = obj.keys.take(12).map { name ->
                        ChessFriend(
                            user = ChessUser(
                                id = name.lowercase(),
                                username = name,
                                rapidRating = 2400,
                                blitzRating = 2400,
                                bulletRating = 2500,
                                classicalRating = 2300,
                                puzzleRating = 2600,
                                country = "WFM"
                            ),
                            isOnline = true
                        )
                    }
                    if (topPlayers.isNotEmpty()) {
                        _friends.value = topPlayers
                    }
                }
            } catch (e: Exception) {
                println("[LichessApi] Error fetching online players: ${e.message}")
            }
        }
    }
    init {
        viewModelScope.launch {
            // Wait for critical preferences to ensure engine starts with correct state
            val versionStr = appPreferences.chessEngineVersion.first()
            val level = appPreferences.chessBotLevel.first()
            val searchTime = appPreferences.chessEngineSearchTime.first()
            val multiPv = appPreferences.chessEngineMultiPv.first()

            val ver = EngineVersion.entries.find { it.name == versionStr } ?: EngineVersion.SF17
            _engineSettings.value = EngineSettings(version = ver, searchTimeSec = searchTime, multiPvLines = multiPv)
            _botLevel.value = level

            chessEngine.switchVersion(ver)
            chessEngine.setSkillLevel(level)
            chessEngine.start()

            startTimer()
            updateThreats()
            fetchLichessPuzzleOfDay()
            fetchChessComDailyPuzzle()
            fetchLichessLeaderboard()
            fetchLichessLiveGames()
            fetchLichessStreamEvents()
            fetchLichessTournaments()
            fetchLichessFriends()
            loadLocalAssets()

            // Sync with persisted settings
            launch {
                appPreferences.chessTheme.collect { themeName ->
                    themeName?.let { name ->
                        val theme = boardThemes.find { it.name == name }
                        if (theme != null) _selectedTheme.value = theme
                    }
                }
            }
            launch {
                appPreferences.chessPieceStyle.collect { _pieceStyle.value = it }
            }
            launch {
                appPreferences.chessEngineVersion.collect { v ->
                    val newVer = EngineVersion.entries.find { it.name == v } ?: EngineVersion.SF17
                    if (_engineSettings.value.version != newVer) {
                        _engineSettings.value = _engineSettings.value.copy(version = newVer)
                        // Also update the engine instance if it's already started
                        chessEngine.switchVersion(newVer)
                    }
                }
            }
            launch {
                appPreferences.chessBotLevel.collect { _botLevel.value = it }
            }
            launch {
                appPreferences.chessEngineSearchTime.collect { time ->
                    _engineSettings.value = _engineSettings.value.copy(searchTimeSec = time)
                }
            }
            launch {
                appPreferences.chessEngineMultiPv.collect { lines ->
                    _engineSettings.value = _engineSettings.value.copy(multiPvLines = lines)
                }
            }

            // Display Preferences Sync
            launch { appPreferences.chessMagnifyPiece.collect { _magnifyPiece.value = it } }
            launch { appPreferences.chessTargetCircle.collect { _targetCircle.value = it } }
            launch { appPreferences.chessPieceAnimation.collect { _pieceAnimation.value = it } }
            launch { appPreferences.chessImmersiveMode.collect { _immersiveMode.value = it } }
            launch { appPreferences.chessShowDestinations.collect { _showDestinations.value = it } }
            launch { appPreferences.chessShowHighlights.collect { _showHighlights.value = it } }
            launch { appPreferences.chessShowMoveList.collect { _showMoveList.value = it } }
            launch { appPreferences.chessShowMaterialDiff.collect { _showMaterialDiff.value = it } }
            launch { appPreferences.chessClockRight.collect { _clockRight.value = it } }
            launch { appPreferences.chessPremoves.collect { _premovesEnabled.value = it } }
            launch { appPreferences.chessConfirmResign.collect { _confirmResign.value = it } }
            launch { appPreferences.chessTouchFeedback.collect { _touchFeedback.value = it } }

            // Sync Game History from Supabase
            launch {
                val remoteHistory = socketService.fetchGameHistory()
                if (remoteHistory.isNotEmpty()) {
                    _gameHistory.value = remoteHistory
                }
            }
        }

        viewModelScope.launch {
            modelPackManager.allPacks.collect { packs ->
                _allModelPacks.value = packs
                _enginePacks.value = packs.filter { it.profile.type == LocalModelType.CHESS_ENGINE }
            }
        }
    }

    fun downloadPack(profile: LocalModelProfile) {
        viewModelScope.launch(NonCancellable) {
            modelPackManager.download(profile)
        }
    }

    fun importLocalModel(uri: String, profile: LocalModelProfile) {
        viewModelScope.launch(NonCancellable) {
            modelPackManager.importLocalPack(uri, profile)
        }
    }

    fun deletePack(profile: LocalModelProfile) {
        viewModelScope.launch {
            modelPackManager.deletePack(profile)
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            val result = modelPackManager.checkForUpdates()
            _isCheckingForUpdates.value = false
            _updateMessage.value = if (result.getOrDefault(false)) "Updates available!" else "Everything is up to date"
            delay(3000)
            _updateMessage.value = null
        }
    }

    private fun loadLocalAssets() {
        viewModelScope.launch {
            try {
                _openingCategories.value = assetLoader.loadPositions()
                _endgamePositions.value = assetLoader.loadEndgames()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startGame(session: GameSession) {
        // Check if engine is available if it's a VS_BOT game
        if (session.mode == GameMode.VS_BOT) {
            viewModelScope.launch {
                // We'll use a placeholder check for now as Stockfish model profile might vary.
                // In a real app, we'd have a specific profile for Stockfish.
                // If it's not downloaded, we can't start the engine.
                // For now, we assume if chessEngine.isInitialized is false, we might need a download.
                // However, the task says trigger ModelPackManager download dialog.
                // I'll add a state for engine error or similar if needed.
            }
        }

        _isLearningMode.value = false
        _activeGameSession.value = session
        // Set state directly — avoid setGameMode/setPlayerColor which both call resetGame (double-reset bug)
        _gameMode.value = session.mode
        _isPlayerWhite.value = session.playerIsWhite
        _gameVariant.value = session.variant
        setBotLevel(session.botLevel)
        setTimeFormat(session.timeFormat)
        resetGame() // single reset; uses _gameVariant for variant-correct starting FEN
        _isGameActive.value = true
        if (session.playerIsWhite) {
            _whiteTimer.value = _whiteTimer.value.copy(isRunning = true)
        } else {
            _blackTimer.value = _blackTimer.value.copy(isRunning = true)
        }
        startTimer() // FIX #7: actually start the countdown
        // FIX #40: if player chose Black, bot must make the opening move
        if (session.mode == GameMode.VS_BOT && !session.playerIsWhite) {
            chessEngine.setPosition(_fen.value) // Ensure engine has correct start FEN
            triggerBotMove()
        }
    }

    /** Re-start the same game session after it ends (Rematch). */
    fun startRematch() {
        _activeGameSession.value?.let { startGame(it) }
    }

    /** Cleanly exit active game session back to lobby */
    fun exitGameSession() {
        _activeGameSession.value = null
        _isGameActive.value = false
        _isLearningMode.value = false
        _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
        _blackTimer.value = _blackTimer.value.copy(isRunning = false)
        resetGame()
    }

    fun downloadStockfish() {
        downloadPack(LocalModelProfile.Stockfish17)
    }

    fun exitLearningMode() {
        _isLearningMode.value = false
    }


    fun resignGame() {
        val state = ChessLogic.parseFen(_fen.value)
        val winner = if (state.isWhiteTurn) "Black" else "White"
        _gameOverReason.value = "$winner wins by Resignation"
        _isGameActive.value = false
        _isBotThinking.value = false // FIX #39
        _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
        _blackTimer.value = _blackTimer.value.copy(isRunning = false)
        runGameAnalysis() // auto-trigger post-game analysis
    }

    fun offerDraw() {
        when (_gameMode.value) {
            GameMode.VS_BOT -> {
                val eval = _evaluation.value?.score ?: 0.0
                if (abs(eval) < 0.5) {
                    _gameOverReason.value = "Draw by Agreement"
                    _isGameActive.value = false
                    _isBotThinking.value = false
                    _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
                    _blackTimer.value = _blackTimer.value.copy(isRunning = false)
                    runGameAnalysis()
                } else {
                    println("Draw declined: engine has a clear advantage")
                }
            }
            GameMode.VS_FRIEND -> {
                // FIX #18: For local friend games, accept draw immediately
                _gameOverReason.value = "Draw by Agreement"
                _isGameActive.value = false
                _isBotThinking.value = false
                _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
                _blackTimer.value = _blackTimer.value.copy(isRunning = false)
                runGameAnalysis()
            }
            else -> {}
        }
    }

    fun updateEngineSettings(settings: EngineSettings) {
        val oldVersion = _engineSettings.value.version
        val oldSearchTime = _engineSettings.value.searchTimeSec
        _engineSettings.value = settings
        
        if (oldVersion != settings.version) {
            chessEngine.switchVersion(settings.version)
        }

        if (oldVersion != settings.version || oldSearchTime != settings.searchTimeSec) {
            chessEngine.setSkillLevel(_botLevel.value)
        }
        viewModelScope.launch {
            appPreferences.setChessEngineVersion(settings.version.name)
            appPreferences.setChessEngineSearchTime(settings.searchTimeSec)
            appPreferences.setChessEngineMultiPv(settings.multiPvLines)
        }
    }



    // FIX #17: removed duplicate setTimeFormat — unified version below at L682

    fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(100)
                if (!_isGameActive.value) continue
                
                val state = ChessLogic.parseFen(_fen.value)
                if (state.isWhiteTurn) {
                    val timer = _whiteTimer.value
                    if (timer.isRunning) {
                        val newTime = (timer.remainingTimeMs - 100).coerceAtLeast(0)
                        _whiteTimer.value = timer.copy(remainingTimeMs = newTime)
                        if (newTime <= 0) {
                            _gameOverReason.value = "Black wins on time"
                            _isGameActive.value = false
                            _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
                            _blackTimer.value = _blackTimer.value.copy(isRunning = false)
                        }
                    }
                } else {
                    val timer = _blackTimer.value
                    if (timer.isRunning) {
                        val newTime = (timer.remainingTimeMs - 100).coerceAtLeast(0)
                        _blackTimer.value = timer.copy(remainingTimeMs = newTime)
                        if (newTime <= 0) {
                            _gameOverReason.value = "White wins on time"
                            _isGameActive.value = false
                            _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
                            _blackTimer.value = _blackTimer.value.copy(isRunning = false)
                        }
                    }
                }
            }
        }
    }

    fun selectTheme(theme: Any) {
        _selectedTheme.value = theme as ChessTheme
        viewModelScope.launch {
            appPreferences.setChessTheme(theme.name)
        }
    }

    fun toggle3D(enabled: Boolean) {
        _selectedTheme.value = _selectedTheme.value.copy(is3D = enabled)
    }

    fun sharePGN() {
        val pgn = generatePGN()
        // Expose via a state flow so the UI layer can trigger the platform share sheet
        _sharePayload.value = pgn
    }

    fun shareInviteLink() {
        val link = _friendInviteInfo.value?.inviteUrl ?: return
        _sharePayload.value = link
    }

    private val _sharePayload = MutableStateFlow<String?>(null)
    val sharePayload: StateFlow<String?> = _sharePayload.asStateFlow()

    fun consumeSharePayload() { _sharePayload.value = null }

    private fun generatePGN(): String {
        // FIX #20: Use current date and UCI history for valid PGN
        val today = try {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val y = now.year
            val m = now.monthNumber.toString().padStart(2, '0')
            val d = now.dayOfMonth.toString().padStart(2, '0')
            "$y.$m.$d"
        } catch (e: Exception) { "????.??.??" }

        val sb = StringBuilder()
        sb.append("[Event \"OmniMind Match\"]\n")
        sb.append("[Site \"OmniMind App\"]\n")
        sb.append("[Date \"$today\"]\n")
        sb.append("[White \"${if (_isPlayerWhite.value) "Player" else "Bot"}\"]\n")
        sb.append("[Black \"${if (_isPlayerWhite.value) "Bot" else "Player"}\"]\n")
        sb.append("[Result \"${_gameOverReason.value ?: "*"}\"]\n\n")

        // Use SAN move history for readable PGN
        val history = _moveHistory.value
        for (i in history.indices step 2) {
            val moveNum = (i / 2) + 1
            sb.append("$moveNum. ${history[i]} ")
            if (i + 1 < history.size) sb.append("${history[i + 1]} ")
        }
        return sb.toString().trimEnd()
    }

    fun practiceMistake(moveIndex: Int) {
        val analysis = _gameAnalysisResult.value ?: return
        val mistake = analysis.moveAnalyses.getOrNull(moveIndex) ?: return
        
        _isLearningMode.value = true

        // Reconstruct FEN before the mistake using UCI history (correct format)
        var tempFen = ChessLogic.getInitialFen()
        for (i in 0 until moveIndex.coerceAtMost(_uciHistory.size)) {
            val uci = _uciHistory[i]
            if (uci.length >= 4) {
                val promo = if (uci.length > 4) uci[4] else null
                tempFen = ChessLogic.applyMove(tempFen, uci.substring(0, 2), uci.substring(2, 4), promo)
            }
        }

        // Rebuild histories up to this point
        val targetUci = _uciHistory.take(moveIndex).toList()
        val newFenHistory = mutableListOf(ChessLogic.getInitialFen())
        var currentF = ChessLogic.getInitialFen()
        for (u in targetUci) {
            if (u.length >= 4) {
                val promo = if (u.length > 4) u[4] else null
                currentF = ChessLogic.applyMove(currentF, u.substring(0, 2), u.substring(2, 4), promo)
                newFenHistory.add(currentF)
            }
        }
        _fenHistory.clear()
        _fenHistory.addAll(newFenHistory)
        _uciHistory.clear()
        _uciHistory.addAll(targetUci)
        _redoStack.clear()

        _fen.value = tempFen
        _moveHistory.value = _moveHistory.value.take(moveIndex)
        _currentMoveIndex.value = moveIndex - 1
        chessEngine.setPosition(tempFen)
        _gameMode.value = GameMode.VS_BOT
        _isGameActive.value = true   // FIX #12: make position playable
        _isBotThinking.value = false // FIX #12
        _gameOverReason.value = null // FIX #12
        updateThreats()
        startTimer()
        println("Practicing mistake at move ${moveIndex + 1}: ${mistake.move}")
    }

    fun fetchLichessProfile(username: String) {
        fetchDualPlatformProfile(username, "Both")
    }

    fun setGameMode(mode: GameMode) {
        // FIX #4: Don't call resetGame() here — startGame() handles full reset
        analysisJob?.cancel()
        _gameMode.value = mode
        if (mode == GameMode.PUZZLE) {
            loadPuzzle(_currentPuzzleIndex.value)
        } else if (mode == GameMode.THEORY) {
            loadLesson(_currentLessonIndex.value)
        }
    }

    fun setPlayerColor(white: Boolean) {
        // FIX #3: Don't call resetGame() here — used from lobby only before startGame()
        _isPlayerWhite.value = white
    }

    /** Flip board perspective during a live game without resetting. FIX #3 */
    fun flipBoard() {
        _isPlayerWhite.value = !_isPlayerWhite.value
    }

    fun setBotLevel(level: Int) {
        _botLevel.value = level
        chessEngine.setSkillLevel(level)
        viewModelScope.launch {
            appPreferences.setChessBotLevel(level)
        }
    }

    fun toggleThreatMap() {
        _showThreatMap.value = !_showThreatMap.value
    }

    fun toggleEngineAnalysis() {
        _showEngineAnalysis.value = !_showEngineAnalysis.value
    }

    fun setPremove(from: String, to: String) {
        _premove.value = Pair(from, to)
    }

    fun clearPremove() {
        _premove.value = null
    }

    fun addFriend(username: String) {
        val newFriend = ChessFriend(
            user = ChessUser("f_${Random.nextInt(1000)}", username, 1400, 1380, 1350, 1420,
                1530,"US"),
            isOnline = true
        )
        _friends.value = _friends.value + newFriend
    }

    fun setTimeFormat(format: String, customInitialMs: Long? = null, customIncrementMs: Long? = null) {
        _activeTimeFormat.value = format
        val (initMs, incMs) = when {
            customInitialMs != null && customIncrementMs != null -> customInitialMs to customIncrementMs
            format == "1+0" -> 60_000L to 0L
            format == "2+1" -> 120_000L to 1_000L
            format == "3+0" -> 180_000L to 0L
            format == "3+2" -> 180_000L to 2_000L
            format == "5+0" -> 300_000L to 0L
            format == "5+3" -> 300_000L to 3_000L
            format == "10+0" -> 600_000L to 0L
            format == "10+5" -> 600_000L to 5_000L
            format == "15+10" -> 900_000L to 10_000L
            format == "30+0" -> 1800_000L to 0L
            format == "30+20" -> 1800_000L to 20_000L
            else -> 600_000L to 0L
        }
        _whiteTimer.value = ChessTimer(initMs, incMs, initMs)
        _blackTimer.value = ChessTimer(initMs, incMs, initMs)
    }

    val scrambleChess960Pieces = MutableStateFlow(true)

    fun toggleScrambleChess960(enable: Boolean) {
        scrambleChess960Pieces.value = enable
        resetGame()
    }

    fun setGameVariant(variant: GameVariant) {
        _gameVariant.value = variant
        resetGame()
    }

    fun resetGame(initialFen: String? = null) {
        analysisJob?.cancel()
        timerJob?.cancel()
        val startFen = initialFen ?: when (_gameVariant.value) {
            GameVariant.CHESS960 -> if (scrambleChess960Pieces.value) generateChess960Fen() else ChessLogic.getInitialFen()
            GameVariant.FREESTYLE -> if (scrambleChess960Pieces.value) generateChess960Fen() else ChessLogic.getInitialFen()
            else -> ChessLogic.getInitialFen()
        }
        _fen.value = startFen
        // FIX #28: Clear all history lists explicitly
        _fenHistory.clear()
        _uciHistory.clear()
        _redoStack.clear()
        _moveHistory.value = emptyList()
        _moveAnalysis.value = emptyList()
        _gameAnalysisResult.value = null
        _premove.value = null
        _visualArrows.value = emptyList()
        _gameOverReason.value = null
        _currentMoveIndex.value = -1 // FIX #5: reset index
        _isBotThinking.value = false  // FIX #39: reset bot thinking flag
        _isGameActive.value = false
        val currentSession = _activeGameSession.value
        setTimeFormat(currentSession?.timeFormat ?: _activeTimeFormat.value)
        updateThreats()
        chessEngine.setPosition(startFen)
    }

    fun startMatch() {
        if (_engineDownloadProgress.value != null && _engineDownloadProgress.value!! < 1f) {
            _socketError.value = "Stockfish engine is still downloading..."
            return
        }
        resetGame()
        _isGameActive.value = true
        _whiteTimer.value = _whiteTimer.value.copy(isRunning = true)
    }

    fun createFriendChallenge(
        timeFormat: String = "5+3",
        variant: String = "CLASSIC",
        isRated: Boolean = false,
        colorChoice: String = "white"
    ) {
        val gameId = "OMNI_" + Clock.System.now().toEpochMilliseconds().toString().takeLast(8)
        val hostName = _currentUserRating.value.username.ifBlank { "Host Player" }
        val inviteInfo = com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketService.createInviteLink(
            gameId = gameId,
            timeFormat = timeFormat,
            variant = variant,
            isRated = isRated,
            colorChoice = colorChoice,
            hostName = hostName
        )
        _friendInviteInfo.value = inviteInfo
        _isWaitingForFriend.value = true
        _gameMode.value = GameMode.VS_FRIEND
        _onlineGameId.value = gameId

        val isHostWhite = when (colorChoice.lowercase()) {
            "white" -> true
            "black" -> false
            else -> (0..1).random() == 0
        }
        
        // Sync variant and ensure board is reset for a fresh game
        _gameVariant.value = try { GameVariant.valueOf(variant.uppercase()) } catch(e: Exception) { GameVariant.CLASSIC }
        setPlayerColor(isHostWhite)
        setTimeFormat(timeFormat)
        resetGame() 

        // Immediately write game room row to Supabase REST database
        viewModelScope.launch {
            socketService.persistGameRoom(
                gameId = gameId,
                hostName = hostName,
                timeFormat = timeFormat,
                variant = variant,
                isRated = isRated,
                colorChoice = colorChoice,
                initialFen = _fen.value
            )
        }

        startOnlineGame(gameId, onConnected = {
            socketService.sendHandshake(
                ChessSocketMessage.FriendHandshake(
                    gameId = gameId,
                    hostName = hostName,
                    hostRating = _currentUserRating.value.blitzRating ?: 1200,
                    timeFormat = timeFormat,
                    variant = variant,
                    isRated = isRated,
                    colorChoice = colorChoice,
                    isHost = true,
                    initialFen = _fen.value
                )
            )
        })
    }

    fun joinFriendChallenge(inviteInput: String) {
        val parsed = com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketService.parseInviteLink(inviteInput) ?: return
        _onlineGameId.value = parsed.gameId
        _friendInviteInfo.value = parsed
        _gameMode.value = GameMode.VS_FRIEND
        _friendOpponentName.value = parsed.hostName

        // Sync settings from invite and reset board before connecting
        _gameVariant.value = try { GameVariant.valueOf(parsed.variant.uppercase()) } catch(e: Exception) { GameVariant.CLASSIC }
        setTimeFormat(parsed.timeFormat)

        viewModelScope.launch {
            // Fetch game room from DB to check exact host color choice if parsed invite is bare link
            val roomState = socketService.fetchGameState(parsed.gameId)
            val effectiveColorChoice = roomState?.color_choice ?: parsed.colorChoice
            val isGuestWhite = when (effectiveColorChoice.lowercase()) {
                "white" -> false // host chosen white -> guest gets black
                "black" -> true  // host chosen black -> guest gets white
                else -> false    // default: host white, guest black
            }
            setPlayerColor(isGuestWhite)
            resetGame()

            val initialFenToUse = roomState?.current_fen?.ifBlank { null } ?: _fen.value
            _fen.value = initialFenToUse
            chessEngine.setPosition(initialFenToUse)
            if (roomState != null && roomState.move_history.isNotEmpty()) {
                _uciHistory.clear()
                _uciHistory.addAll(roomState.move_history)
            }

            // Immediately mark game as active in DB so host polling auto-detects guest join instantly
            socketService.updateGameState(
                parsed.gameId,
                initialFenToUse,
                roomState?.move_history ?: emptyList(),
                whiteRemainingMs = _whiteTimer.value.remainingTimeMs,
                blackRemainingMs = _blackTimer.value.remainingTimeMs
            )

            startOnlineGame(parsed.gameId, onConnected = {
                socketService.sendPlayerJoined(
                    guestName = _currentUserRating.value.username.ifBlank { "Guest Player" },
                    guestRating = _currentUserRating.value.blitzRating ?: 1200
                )
                _isWaitingForFriend.value = false
                _isGameActive.value = true
                val fenState = ChessLogic.parseFen(_fen.value)
                _whiteTimer.value = _whiteTimer.value.copy(isRunning = fenState.isWhiteTurn)
                _blackTimer.value = _blackTimer.value.copy(isRunning = !fenState.isWhiteTurn)
                startTimer()
            })
        }
    }

    fun startOnlineGame(gameId: String, onConnected: (suspend () -> Unit)? = null) {
        _onlineGameId.value = gameId

        // Optimized DB state polling loop (1000ms interval to prevent UI stutter while ensuring reliable game sync)
        viewModelScope.launch {
            while (_onlineGameId.value == gameId) {
                kotlinx.coroutines.delay(1000)
                if (_onlineGameId.value != gameId) break
                val state = socketService.fetchGameState(gameId) ?: continue

                // Check if host is waiting and guest joined
                if (_isWaitingForFriend.value && state.status == "active") {
                    _isWaitingForFriend.value = false
                    _isGameActive.value = true
                    val fenState = ChessLogic.parseFen(_fen.value)
                    _whiteTimer.value = _whiteTimer.value.copy(isRunning = fenState.isWhiteTurn)
                    _blackTimer.value = _blackTimer.value.copy(isRunning = !fenState.isWhiteTurn)
                    startTimer()
                }

                // Sync game state if DB state (FEN or move history) differs from local FEN/history
                if (state.current_fen.isNotBlank() && (state.current_fen != _fen.value || state.move_history.size > _uciHistory.size)) {
                    val initialFen = _fenHistory.firstOrNull() ?: ChessLogic.getInitialFen()
                    var replayFen = initialFen
                    val newSanList = mutableListOf<String>()

                    state.move_history.forEach { uci ->
                        if (uci.length >= 4) {
                            val from = uci.substring(0, 2)
                            val to = uci.substring(2, 4)
                            val prom = if (uci.length > 4) uci[4] else null
                            val san = ChessLogic.moveToSan(replayFen, from, to, prom)
                            newSanList.add(san)
                            replayFen = ChessLogic.applyMove(replayFen, from, to, prom)
                        }
                    }

                    _fen.value = state.current_fen
                    chessEngine.setPosition(state.current_fen)
                    _uciHistory.clear()
                    _uciHistory.addAll(state.move_history)
                    _moveHistory.value = newSanList
                    _currentMoveIndex.value = newSanList.size - 1

                    state.white_remaining_ms?.let { ms -> _whiteTimer.value = _whiteTimer.value.copy(remainingTimeMs = ms) }
                    state.black_remaining_ms?.let { ms -> _blackTimer.value = _blackTimer.value.copy(remainingTimeMs = ms) }

                    val isWhiteTurnNow = state.current_fen.contains(" w ")
                    _whiteTimer.value = _whiteTimer.value.copy(isRunning = isWhiteTurnNow)
                    _blackTimer.value = _blackTimer.value.copy(isRunning = !isWhiteTurnNow)

                    val checkGameOver = ChessLogic.isGameOver(state.current_fen)
                    if (checkGameOver.isGameOver) {
                        _gameOverReason.value = checkGameOver.reason
                        _isGameActive.value = false
                    }
                }
            }
        }

        viewModelScope.launch {
            socketService.connect(gameId, onConnected = {
                // Check if there's an existing state in the DB to recover from
                val existingState = socketService.fetchGameState(gameId)
                if (existingState != null && existingState.status == "active") {
                    _fen.value = existingState.current_fen
                    chessEngine.setPosition(existingState.current_fen)
                    _uciHistory.clear()
                    _uciHistory.addAll(existingState.move_history)
                    existingState.white_remaining_ms?.let { ms ->
                        _whiteTimer.value = _whiteTimer.value.copy(remainingTimeMs = ms)
                    }
                    existingState.black_remaining_ms?.let { ms ->
                        _blackTimer.value = _blackTimer.value.copy(remainingTimeMs = ms)
                    }
                    val fenState = ChessLogic.parseFen(existingState.current_fen)
                    _whiteTimer.value = _whiteTimer.value.copy(isRunning = fenState.isWhiteTurn)
                    _blackTimer.value = _blackTimer.value.copy(isRunning = !fenState.isWhiteTurn)
                    _isGameActive.value = true
                    _isWaitingForFriend.value = false
                    startTimer()
                }
                onConnected?.invoke()
            })
        }
        viewModelScope.launch {
            socketService.incomingMessages.collect { message ->
                when (message) {
                    is ChessSocketMessage.Move -> {
                        // Sync opponent's clock if they sent remainingMs
                        if (message.remainingMs > 0) {
                            val isOpponentWhite = !_isPlayerWhite.value
                            if (isOpponentWhite) {
                                _whiteTimer.value = _whiteTimer.value.copy(remainingTimeMs = message.remainingMs)
                            } else {
                                _blackTimer.value = _blackTimer.value.copy(remainingTimeMs = message.remainingMs)
                            }
                        }
                        makeMove(message.from, message.to, message.promotion?.firstOrNull(), isRemoteMove = true)
                    }
                    is ChessSocketMessage.PlayerJoined -> {
                        _isWaitingForFriend.value = false
                        _friendOpponentName.value = message.guestName
                        _isGameActive.value = true
                        val state = ChessLogic.parseFen(_fen.value)
                        _whiteTimer.value = _whiteTimer.value.copy(isRunning = state.isWhiteTurn)
                        _blackTimer.value = _blackTimer.value.copy(isRunning = !state.isWhiteTurn)
                        startTimer()
                    }
                    is ChessSocketMessage.FriendHandshake -> {
                        if (message.isHost) {
                            // Guest receives host's handshake → sync all game settings
                            _friendOpponentName.value = message.hostName
                            setTimeFormat(message.timeFormat)
                            message.initialFen?.let { fen ->
                                if (fen.isNotBlank()) {
                                    updateFen(fen)
                                }
                            }
                            // Sync color: if host is white → guest is black, and vice versa
                            val guestIsWhite = when (message.colorChoice.lowercase()) {
                                "white" -> false  // host took white, guest gets black
                                "black" -> true   // host took black, guest gets white
                                else -> _isPlayerWhite.value  // keep what was set at join time
                            }
                            setPlayerColor(guestIsWhite)
                        } else {
                            _friendOpponentName.value = message.hostName
                        }
                    }
                    is ChessSocketMessage.Resign -> {
                        _gameOverReason.value = "${message.player} resigned. Opponent wins!"
                        _isGameActive.value = false
                    }
                    is ChessSocketMessage.RematchRequest -> {
                        resetGame()
                        _isGameActive.value = true
                    }
                    is ChessSocketMessage.GameState -> {
                        updateFen(message.fen)
                    }
                    is ChessSocketMessage.Chat -> { }
                    is ChessSocketMessage.Error -> {
                        _socketError.value = message.message
                    }
                }
            }
        }
    }

    fun updateFen(newFen: String) {
        _fen.value = newFen
        chessEngine.setPosition(newFen)
        updateThreats()
        analyzePosition()
        fetchOpeningExplorer(newFen)
        updateLocalOpeningInfo(newFen)
    }

    fun loadPgn(pgnText: String) {
        viewModelScope.launch {
            val importResult = ChessLogic.importPgn(pgnText)
            resetGame(importResult.initialFen)
            
            _fenHistory.clear()
            var currentFen = importResult.initialFen
            _fenHistory.add(currentFen)

            val sanList = mutableListOf<String>()
            val uciList = mutableListOf<String>()

            for (move in importResult.moves) {
                val from = move.first
                val to = move.second
                val promo = move.third

                val moveCode = "$from$to${promo ?: ""}"
                val sanMove = ChessLogic.moveToSan(currentFen, from, to, promo)

                currentFen = ChessLogic.applyMove(currentFen, from, to, promo)
                _fenHistory.add(currentFen)
                sanList.add(sanMove.ifBlank { moveCode })
                uciList.add(moveCode)
            }

            _moveHistory.value = sanList
            _uciHistory.clear()
            _uciHistory.addAll(uciList)
            if (sanList.isNotEmpty()) {
                goToMove(sanList.size - 1)
            } else {
                _fen.value = currentFen
                chessEngine.setPosition(currentFen)
                updateThreats()
                analyzePosition()
            }
        }
    }

    fun importPgnFile(attachment: MediaAttachmentUi) {
        viewModelScope.launch {
            val pgnText = withContext(Dispatchers.IO) {
                mediaProcessor.readAsText(attachment.path)
            }
            if (pgnText.isNotBlank()) {
                loadPgn(pgnText)
            }
        }
    }

    fun importBoardImage(attachment: MediaAttachmentUi) {
        // viewModelScope.launch {
            // try {
            //     val imageObj = withContext(Dispatchers.IO) {
            //         // mediaProcessor.readAsImage(attachment.path)
            //     }
            //     val prompt = """
            //         Analyze this chessboard photo or chess diagram.
            //         Extract the exact Forsyth-Edwards Notation (FEN) for the position on the board.
            //         Return ONLY the valid FEN string (e.g. rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1) without formatting or explanations.
            //     """.trimIndent()
            //     val result = aiService?.analyzeImage(imageObj, prompt)
            //     val output = result?.getOrNull()?.trim() ?: ""
            //     val fenRegex = Regex("""([rnbqkpRNBQKP1-8]{1,8}/){7}[rnbqkpRNBQKP1-8]{1,8}\s+[wb]\s+([KQkq\-]+)\s+([a-h][36]|\-)\s+\d+\s+\d+""")
            //     val fenFound = fenRegex.find(output)?.value?.trim()
            //         ?: output.lines().firstOrNull { it.contains("/") && it.split("/").size == 8 }?.trim()

            //     if (!fenFound.isNullOrBlank()) {
            //         updateFen(fenFound)
            //     }
            // } catch (e: Exception) {
            //     // Log or handle gracefully
            // }
        // }
    }

    private fun updateLocalOpeningInfo(fenStr: String) {
        viewModelScope.launch {
            _localOpeningInfo.value = openingRepository.getOpeningByFen(fenStr)
        }
    }

    private val _isOpeningMastersDatabase = MutableStateFlow(false)
    val isOpeningMastersDatabase: StateFlow<Boolean> = _isOpeningMastersDatabase.asStateFlow()

    fun toggleOpeningDatabaseSource(isMasters: Boolean) {
        _isOpeningMastersDatabase.value = isMasters
        fetchOpeningExplorer(_fen.value)
    }

    fun fetchOpeningExplorer(fenStr: String) {
        if (_isBotThinking.value) return // FIX #32: skip during bot thinking to avoid rate limiting
        viewModelScope.launch {
            try {
                val encodedFen = fenStr.encodeURLQueryComponent()
                val baseUrl = if (_isOpeningMastersDatabase.value) "https://explorer.lichess.ovh/masters" else "https://explorer.lichess.ovh/lichess"
                val queryParams = if (_isOpeningMastersDatabase.value) "?fen=$encodedFen&moves=12&topGames=5" else "?fen=$encodedFen&speeds=blitz,rapid,classical&ratings=1600,1800,2000,2200,2500&moves=12&topGames=5"
                val response: HttpResponse = httpClient.get("$baseUrl$queryParams")

                if (response.status.value == 200) {
                    val body = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    val parsed = json.decodeFromString<LichessOpeningData>(body)
                    _openingExplorerData.value = parsed
                }
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }


    private fun updateThreats() {
        _threatMap.value = ChessLogic.computeThreatMap(_fen.value)
    }

    private val _gameOverReason = MutableStateFlow<String?>(null)
    val gameOverReason: StateFlow<String?> = _gameOverReason

    fun dismissGameOverDialog() {
        _gameOverReason.value = null
    }

    fun generateChess960Fen(): String {
        return ChessLogic.generateChess960Fen()
    }

    fun makeMove(from: String, to: String, promotion: Char? = null, isRemoteMove: Boolean = false) {
        if (_gameOverReason.value != null || !_isGameActive.value) return

        val state = ChessLogic.parseFen(_fen.value)
        val isPlayersTurn = (state.isWhiteTurn == _isPlayerWhite.value)

        // Block local user taps if not the player's turn in VS_FRIEND online mode (unless executing an incoming remote move)
        if (_gameMode.value == GameMode.VS_FRIEND && !isPlayersTurn && !isRemoteMove) return

        // Allow move if it is player's turn OR if bot is making its turn move
        if (_gameMode.value == GameMode.VS_BOT && !isPlayersTurn && !_isBotThinking.value) return

        val currentVariant = _gameVariant.value
        if (!ChessLogic.isValidMove(_fen.value, from, to, currentVariant)) {
             println("[StockfishDebug] Invalid move attempted: $from -> $to")
             return
        }

        val moveCode = "$from$to${promotion ?: ""}"
        val sanMove = ChessLogic.moveToSan(_fen.value, from, to, promotion)

        // Handle active clock changes — switch clocks based on whose turn just ended
        if (state.isWhiteTurn) {
            val timer = _whiteTimer.value
            _whiteTimer.value = timer.copy(
                remainingTimeMs = timer.remainingTimeMs + timer.incrementMs,
                isRunning = false
            )
            _blackTimer.value = _blackTimer.value.copy(isRunning = true)
        } else {
            val timer = _blackTimer.value
            _blackTimer.value = timer.copy(
                remainingTimeMs = timer.remainingTimeMs + timer.incrementMs,
                isRunning = false
            )
            _whiteTimer.value = _whiteTimer.value.copy(isRunning = true)
        }

        val oldFen = _fen.value
        val newFen = ChessLogic.applyMove(oldFen, from, to, promotion)
        _fen.value = newFen
        println("[StockfishDebug] Updated _fen from '$oldFen' -> '$newFen'")
        recordNewMove(moveCode, sanMove, oldFen)
        chessEngine.setPosition(newFen)

        // If online game and this was a local move, send move to socket & REST DB
        if (_gameMode.value == GameMode.VS_FRIEND && _onlineGameId.value != null && !isRemoteMove && isPlayersTurn) {
            val gid = _onlineGameId.value!!
            val remaining = if (_isPlayerWhite.value) _whiteTimer.value.remainingTimeMs else _blackTimer.value.remainingTimeMs
            viewModelScope.launch {
                socketService.sendMove(from, to, promotion?.toString(), remainingMs = remaining)
                socketService.updateGameState(
                    gid, 
                    newFen, 
                    _uciHistory.toList(),
                    whiteRemainingMs = _whiteTimer.value.remainingTimeMs,
                    blackRemainingMs = _blackTimer.value.remainingTimeMs
                )
            }
        }

        updateThreats()

        val checkGameOver = ChessLogic.isGameOver(newFen)
        if (checkGameOver.isGameOver) {
            _gameOverReason.value = checkGameOver.reason
            _isGameActive.value = false
            _isBotThinking.value = false // FIX #39
            _whiteTimer.value = _whiteTimer.value.copy(isRunning = false)
            _blackTimer.value = _blackTimer.value.copy(isRunning = false)

            if (_gameMode.value == GameMode.VS_FRIEND && _onlineGameId.value != null) {
                viewModelScope.launch {
                    val pgn = generatePGN()
                    val resultText = when {
                        checkGameOver.reason.contains("White wins") -> "1-0"
                        checkGameOver.reason.contains("Black wins") -> "0-1"
                        checkGameOver.reason.contains("Draw") -> "1/2-1/2"
                        else -> "*"
                    }
                    val savedGame = com.creativeblisslabs.omnimind.core.chess.SavedGame(
                        id = _onlineGameId.value!!,
                        whitePlayer = (if (_isPlayerWhite.value) _currentUserRating.value.username else _friendOpponentName.value) ?: "White",
                        blackPlayer = (if (_isPlayerWhite.value) _friendOpponentName.value else _currentUserRating.value.username) ?: "Black",
                        whiteRating = if (_isPlayerWhite.value) _currentUserRating.value.blitzRating else 1200,
                        blackRating = if (_isPlayerWhite.value) 1200 else _currentUserRating.value.blitzRating,
                        date = Clock.System.now().toEpochMilliseconds(),
                        result = resultText,
                        pgn = pgn,
                        mode = GameMode.VS_FRIEND,
                        variant = _gameVariant.value,
                        event = "Online Match"
                    )
                    socketService.saveGameToHistory(savedGame)
                    socketService.sendResign("Game Over") // reuse resign logic to mark finished
                }
            }

            runGameAnalysis() // FIX #10: auto-trigger analysis on game over
            return
        }

        // Handle bot response if mode is VS_BOT
        val nextState = ChessLogic.parseFen(newFen)
        val botIsNext = (_gameMode.value == GameMode.VS_BOT) &&
                ((nextState.isWhiteTurn && !_isPlayerWhite.value) || (!nextState.isWhiteTurn && _isPlayerWhite.value))

        if (botIsNext) {
            analysisJob?.cancel()
            _isBotThinking.value = true
            viewModelScope.launch {
                try {
                    delay(100) // Fast realistic pause
                    val searchDepth = (_botLevel.value / 3).coerceIn(2, 8)
                    println("[StockfishDebug] Requesting bestMove at depth $searchDepth level ${_botLevel.value} for FEN $newFen")
                    
                    // Force set position again just to be absolutely sure the engine has it
                    chessEngine.setPosition(newFen)
                    
                    chessEngine.getBestMove(searchDepth, _botLevel.value) { botMove ->
                        println("[StockfishDebug] Received bestMove callback: '$botMove'")
                        viewModelScope.launch(Dispatchers.Main) {
                            if (botMove.isNotBlank() && botMove.length >= 4) {
                                val bFrom = botMove.substring(0, 2)
                                val bTo = botMove.substring(2, 4)
                                val bProm = if (botMove.length > 4) botMove[4] else null
                                println("[StockfishDebug] Executing move $bFrom -> $bTo (prom: $bProm)")
                                makeMove(bFrom, bTo, bProm)
                            } else {
                                println("[StockfishDebug] Received blank or invalid move: '$botMove'")
                                // Fallback: if engine fails, maybe it's mate or draw not caught by logic yet
                                val finalCheck = ChessLogic.isGameOver(newFen)
                                if (finalCheck.isGameOver) {
                                    _gameOverReason.value = finalCheck.reason
                                    _isGameActive.value = false
                                }
                            }
                            _isBotThinking.value = false
                        }
                    }
                } catch (e: Exception) {
                    println("[StockfishDebug] Exception in bot thinking: ${e.message}")
                    _isBotThinking.value = false
                }
            }
        } else {
            makePremoveIfAny()
            analyzePosition()
        }
    }

    /** Internal: trigger the engine to make the first move (when player picks Black). FIX #40 */
    private fun triggerBotMove() {
        if (_isBotThinking.value) return
        analysisJob?.cancel()
        _isBotThinking.value = true
        viewModelScope.launch {
            try {
                delay(150) // natural pause before opening move
                val searchDepth = (_botLevel.value / 3).coerceIn(2, 8)
                chessEngine.getBestMove(searchDepth, _botLevel.value) { botMove ->
                    _isBotThinking.value = false
                    if (botMove.isNotBlank() && botMove.length >= 4) {
                        val bFrom = botMove.substring(0, 2)
                        val bTo = botMove.substring(2, 4)
                        val bProm = if (botMove.length > 4) botMove[4] else null
                        makeMove(bFrom, bTo, bProm)
                    }
                }
            } catch (e: Exception) {
                _isBotThinking.value = false
            }
        }
    }

    fun makePremoveIfAny() {
        val pm = _premove.value ?: return
        _premove.value = null
        if (ChessLogic.isValidMove(_fen.value, pm.first, pm.second, _gameVariant.value)) {
            makeMove(pm.first, pm.second)
        }
    }

    private val _activePuzzleSolution = MutableStateFlow<List<String>>(emptyList())
    val activePuzzleSolution: StateFlow<List<String>> = _activePuzzleSolution

    // Interactive Puzzle Solver logic
    fun loadPuzzlePosition(fen: String, solution: List<String> = emptyList()) {
        _puzzleFeedback.value = ""
        _fen.value = fen
        _activePuzzleSolution.value = solution
        val state = ChessLogic.parseFen(fen)
        _isPlayerWhite.value = state.isWhiteTurn
        _moveHistory.value = emptyList()
        _uciHistory.clear()
        chessEngine.setPosition(fen)
        updateThreats()
        analyzePosition()
    }

    fun loadPuzzleByTheme(themeTitle: String) {
        val formattedTheme = themeTitle.replace(" ", "").lowercase()
        viewModelScope.launch {
            try {
                _isLichessLoading.value = true
                val response = httpClient.get("https://lichess.org/api/puzzle/next?angle=$formattedTheme")
                if (response.status.value in 200..299) {
                    val jsonStr = response.body<String>()
                    val obj = Json.parseToJsonElement(jsonStr).jsonObject
                    val puzzleObj = obj["puzzle"]?.jsonObject
                    val gameObj = obj["game"]?.jsonObject
                    
                    val fen = gameObj?.get("fen")?.jsonPrimitive?.content ?: ChessLogic.getInitialFen()
                    val rating = puzzleObj?.get("rating")?.jsonPrimitive?.int ?: 1500
                    val solArray = puzzleObj?.get("solution")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val themes = puzzleObj?.get("themes")?.jsonArray?.map { it.jsonPrimitive.content } ?: listOf(themeTitle)
                    val puzzleId = puzzleObj?.get("id")?.jsonPrimitive?.content ?: "theme_puzzle"

                    val fetchedPuzzle = LichessPuzzleData(
                        id = puzzleId,
                        fen = fen,
                        solution = solArray,
                        rating = rating,
                        themes = themes,
                        gameUrl = "https://lichess.org/training/$puzzleId",
                        playerWhite = "White",
                        playerBlack = "Black",
                        initialPly = 0
                    )
                    _lichessPuzzle.value = fetchedPuzzle
                    loadPuzzlePosition(fen, solArray)
                    _isLichessLoading.value = false
                    return@launch
                }
            } catch (e: Exception) {
                _isLichessLoading.value = false
            }

            // Fallback to local puzzle matching
            val matchingIndex = _puzzles.value.indexOfFirst { 
                it.instruction.contains(themeTitle, ignoreCase = true) || it.hints.any { hint -> hint.contains(themeTitle, ignoreCase = true) } 
            }
            if (matchingIndex >= 0) {
                loadPuzzle(matchingIndex)
            } else if (_puzzles.value.isNotEmpty()) {
                loadPuzzle((0 until _puzzles.value.size).random())
            }
        }
    }

    fun loadPuzzle(index: Int) {
        _puzzleFeedback.value = ""
        val puzzleList = _puzzles.value
        if (index in puzzleList.indices) {
            _currentPuzzleIndex.value = index
            val puzzle = puzzleList[index]
            _fen.value = puzzle.fen
            _activePuzzleSolution.value = puzzle.solution
            val state = ChessLogic.parseFen(puzzle.fen)
            _isPlayerWhite.value = state.isWhiteTurn
            _moveHistory.value = emptyList()
            _uciHistory.clear()
            chessEngine.setPosition(puzzle.fen)
            updateThreats()
            analyzePosition()
        }
    }

    fun startPuzzleStorm() {
        _gameMode.value = GameMode.PUZZLE_STORM
        _puzzleStormScore.value = 0
        _puzzleStormTimeLeft.value = 180
        _puzzleFeedback.value = ""
        loadNextPuzzle()
        
        puzzleStormJob?.cancel()
        puzzleStormJob = viewModelScope.launch {
            while (_puzzleStormTimeLeft.value > 0) {
                delay(1000)
                _puzzleStormTimeLeft.value -= 1
            }
            _puzzleFeedback.value = "Game Over! Score: ${_puzzleStormScore.value}"
        }
    }

    fun startPuzzleStreak() {
        _gameMode.value = GameMode.PUZZLE_STREAK
        _puzzleStreak.value = 0
        _puzzleFeedback.value = ""
        loadNextPuzzle()
    }

    private fun loadNextPuzzle() {
        val nextIdx = (0 until _puzzles.value.size).random()
        loadPuzzle(nextIdx)
    }

    fun submitPuzzleMove(from: String, to: String) {
        val puzzle = _puzzles.value.getOrNull(_currentPuzzleIndex.value) ?: return
        val currentMoveIndex = _moveHistory.value.size
        val expectedMove = puzzle.solution.getOrNull(currentMoveIndex)

        val playerMove = "$from$to"
        // FIX #15: use startsWith to handle promotion moves (5-char) vs 4-char player input
        if (expectedMove != null && (expectedMove == playerMove || expectedMove.startsWith(playerMove))) {
            val newFen = ChessLogic.applyMove(_fen.value, from, to)
            _fen.value = newFen
            _moveHistory.value = _moveHistory.value + playerMove
            chessEngine.setPosition(newFen)
            updateThreats()

            val nextMoveIndex = _moveHistory.value.size
            if (nextMoveIndex >= puzzle.solution.size) {
                _puzzleFeedback.value = "Success! Puzzle Solved!"
                
                when (_gameMode.value) {
                    GameMode.PUZZLE_STORM -> {
                        _puzzleStormScore.value += 1
                        viewModelScope.launch {
                            delay(1000)
                            loadNextPuzzle()
                        }
                    }
                    GameMode.PUZZLE_STREAK -> {
                        _puzzleStreak.value += 1
                        viewModelScope.launch {
                            delay(1000)
                            loadNextPuzzle()
                        }
                    }
                 else -> {
    val currentRating = _currentUserRating.value.puzzleRating ?: 0
    _currentUserRating.value = _currentUserRating.value.copy(
        puzzleRating = currentRating + 15
    )
}
                }
            } else {
                _puzzleFeedback.value = "Correct! Keep going..."
                val responseMove = puzzle.solution.getOrNull(nextMoveIndex)
                if (responseMove != null && responseMove.length >= 4) {
                    viewModelScope.launch {
                        delay(500)
                        val rFrom = responseMove.substring(0, 2)
                        val rTo = responseMove.substring(2, 4)
                        val responseFen = ChessLogic.applyMove(_fen.value, rFrom, rTo)
                        _fen.value = responseFen
                        _moveHistory.value = _moveHistory.value + responseMove
                        chessEngine.setPosition(responseFen)
                        updateThreats()
                    }
                }
            }
        } else {
            _puzzleFeedback.value = "Incorrect move. Try again!"
            if (_gameMode.value == GameMode.PUZZLE_STREAK) {
                _puzzleFeedback.value = "Incorrect! Streak ended at ${_puzzleStreak.value}."
                // Potentially reset or stay on same puzzle to learn
            }
        }
    }

    // Theory & Lessons Interactive Logic
    fun loadLesson(index: Int) {
        val lessonList = _lessons.value
        if (index in lessonList.indices) {
            _currentLessonIndex.value = index
            _lessonStepIndex.value = 0
            val lesson = lessonList[index]
            _fen.value = lesson.fen
            _fenHistory.clear()
            _uciHistory.clear()
            _moveHistory.value = emptyList()
            _redoStack.clear()

            var simFen = lesson.fen
            val tempEntries = mutableListOf<RedoEntry>()
            lesson.keyMoves.forEach { moveStr ->
                if (moveStr.length >= 4) {
                    val from = moveStr.substring(0, 2)
                    val to = moveStr.substring(2, 4)
                    val promChar = if (moveStr.length >= 5) moveStr[4] else null
                    val san = try { ChessLogic.moveToSan(simFen, from, to, promChar) } catch (e: Exception) { moveStr }
                    val nextFen = try { ChessLogic.applyMove(simFen, from, to, promChar) } catch (e: Exception) { simFen }
                    tempEntries.add(RedoEntry(fen = nextFen, san = san, uci = moveStr))
                    simFen = nextFen
                }
            }
            _redoStack.addAll(tempEntries.reversed())
            updateRedoState()

            chessEngine.setPosition(lesson.fen)
            updateThreats()
        }
    }
    fun undoLastMove() {
        val currentList = _moveHistory.value
        if (_fenHistory.isNotEmpty() && currentList.isNotEmpty()) {
            val previousFen = _fenHistory.removeAt(_fenHistory.size - 1)
            val lastSan = currentList.last()
            val lastUci = if (_uciHistory.isNotEmpty()) _uciHistory.removeAt(_uciHistory.size - 1) else ""

            // FIX #6b: store full entry for redo
            _redoStack.add(RedoEntry(fen = _fen.value, san = lastSan, uci = lastUci))
            updateRedoState()

            _fen.value = previousFen
            _moveHistory.value = currentList.dropLast(1)
            _currentMoveIndex.value = _moveHistory.value.size - 1 // FIX #5
            chessEngine.setPosition(previousFen)
            updateThreats()
        }
    }

    fun showPuzzleHint() {
        val puzzleList = _puzzles.value
        val puzzle = puzzleList.getOrNull(_currentPuzzleIndex.value)
        if (puzzle != null && puzzle.solution.isNotEmpty()) {
            val firstMove = puzzle.solution.first()
            if (firstMove.length >= 4) {
                val from = firstMove.substring(0, 2)
                val to = firstMove.substring(2, 4)
                _visualArrows.value = listOf(Pair(from, to))
            }
        }
    }

    fun redoNextMove() {
        if (_redoStack.isNotEmpty()) {
            val entry = _redoStack.removeAt(_redoStack.size - 1)
            // FIX #6: restore full move state from RedoEntry
            _fenHistory.add(_fen.value)
            _uciHistory.add(entry.uci)
            _moveHistory.value = _moveHistory.value + entry.san
            _fen.value = entry.fen
            _currentMoveIndex.value = _moveHistory.value.size - 1
            updateRedoState()
            chessEngine.setPosition(entry.fen)
            updateThreats()
        }
    }

    private val _currentMoveIndex = MutableStateFlow(-1)
    val currentMoveIndex: StateFlow<Int> = _currentMoveIndex

    // Call this helper whenever a new move is successfully made
    private fun recordNewMove(uciMove: String, sanMove: String, oldFen: String) {
        _fenHistory.add(oldFen)
        _uciHistory.add(uciMove)
        _moveHistory.value = _moveHistory.value + sanMove
        _currentMoveIndex.value = _moveHistory.value.size - 1
        _redoStack.clear()

        viewModelScope.launch {
            try {
                val pgn = generatePGN()
                val opponent = when (_gameMode.value) {
                    GameMode.VS_BOT -> "Stockfish Level ${_botLevel.value}"
                    GameMode.VS_FRIEND -> _friendOpponentName.value ?: "Friend Challenge"
                    GameMode.PUZZLE -> "Chess Puzzle"
                    else -> "Analysis Board"
                }
                appPreferences.saveLastChessGame(
                    fen = _fen.value,
                    pgn = pgn,
                    title = "vs $opponent",
                    mode = _gameMode.value.name
                )
            } catch (_: Exception) {}
        }
    }

    fun goToMove(index: Int) {
        _currentMoveIndex.value = index
        val startFen = _fenHistory.firstOrNull() ?: ChessLogic.getInitialFen()
        var tempFen = startFen

        if (index >= 0) {
            for (i in 0..index.coerceAtMost(_uciHistory.size - 1)) {
                val m = _uciHistory[i]
                if (m.length >= 4) {
                    val promotion = if (m.length > 4) m[4] else null
                    tempFen = ChessLogic.applyMove(tempFen, m.substring(0, 2), m.substring(2, 4), promotion)
                }
            }
        }
        _fen.value = tempFen
        updateThreats()
        analyzePosition()
    }

    fun stepForward() {
        if (_currentMoveIndex.value < _moveHistory.value.size - 1) {
            goToMove(_currentMoveIndex.value + 1)
        }
    }

    fun stepBackward() {
        if (_currentMoveIndex.value >= 0) {
            goToMove(_currentMoveIndex.value - 1)
        }
    }
    fun nextLessonStep() {
        val lesson = _lessons.value.getOrNull(_currentLessonIndex.value) ?: return
        val steps = lesson.keyMoves
        val currentStep = _lessonStepIndex.value
        if (currentStep < steps.size) {
            val moveCode = steps[currentStep]
            if (moveCode.length >= 4) {
                val from = moveCode.substring(0, 2)
                val to = moveCode.substring(2, 4)
                val newFen = ChessLogic.applyMove(_fen.value, from, to)
                _fen.value = newFen
                _moveHistory.value = _moveHistory.value + moveCode
                _lessonStepIndex.value = currentStep + 1
                chessEngine.setPosition(newFen)
                updateThreats()
            }
        }
    }

    fun prevLessonStep() {
        val lesson = _lessons.value.getOrNull(_currentLessonIndex.value) ?: return
        val currentStep = _lessonStepIndex.value
        if (currentStep > 0) {
            val targetStep = currentStep - 1
            _lessonStepIndex.value = targetStep
            // Replay from initial lesson FEN up to targetStep
            var currentFen = lesson.fen
            val moves = lesson.keyMoves.take(targetStep)
            for (mv in moves) {
                if (mv.length >= 4) {
                    currentFen = ChessLogic.applyMove(currentFen, mv.substring(0, 2), mv.substring(2, 4))
                }
            }
            _fen.value = currentFen
            _moveHistory.value = moves
            chessEngine.setPosition(currentFen)
            updateThreats()
        }
    }

    fun resetLesson() {
        val lesson = _lessons.value.getOrNull(_currentLessonIndex.value) ?: return
        _lessonStepIndex.value = 0
        _fen.value = lesson.fen
        _moveHistory.value = emptyList()
        chessEngine.setPosition(lesson.fen)
        updateThreats()
    }

    fun analyzeGame() {
        runGameAnalysis()
    }

    fun runGameAnalysis() {
        val size = _moveHistory.value.size
        if (size == 0) return

        viewModelScope.launch {
            _analysisProgress.value = 0.01f
            val history = _moveHistory.value
            val analysisResults = mutableListOf<MoveAnalysis>()
            var currentFenAnalysis = ChessLogic.getInitialFen()
            
            val graph = mutableListOf<Double>()
            var lastEval = 0.3
            val counts = mutableMapOf<MoveQuality, Int>().apply {
                MoveQuality.entries.forEach { put(it, 0) }
            }

            // FIX #27: Use UCI history for correct from/to square extraction
            val uciMoves = _uciHistory.toList()
            val sanMoves = _moveHistory.value
            for (i in uciMoves.indices) {
                _analysisProgress.value = (i.toFloat() / uciMoves.size.toFloat()).coerceAtLeast(0.01f)
                val uci = uciMoves[i]
                val san = sanMoves.getOrElse(i) { uci }
                if (uci.length < 4) continue

                val result = withTimeoutOrNull(600) {
                    suspendCancellableCoroutine<EngineEvaluation> { continuation ->
                        chessEngine.setPosition(currentFenAnalysis)
                        chessEngine.analyze(8) { eval ->
                            if (continuation.isActive) continuation.resume(eval)
                        }
                    }
                } ?: EngineEvaluation(lastEval, emptyList(), 0)

                val currentEval = result.score
                val bestMove = result.bestLine.firstOrNull()
                val isBest = bestMove?.take(4) == uci.take(4)

                val quality = ChessLogic.determineMoveQuality(
                    scoreBefore = lastEval,
                    scoreAfter = currentEval,
                    isBestMove = isBest,
                    mateBefore = null,
                    mateAfter = result.mate
                )

                analysisResults.add(MoveAnalysis(san, quality, currentEval - lastEval, bestMove))
                lastEval = currentEval
                graph.add(currentEval.coerceIn(-9.9, 9.9))
                counts[quality] = (counts[quality] ?: 0) + 1

                try {
                    val promo = if (uci.length > 4) uci[4] else null
                    currentFenAnalysis = ChessLogic.applyMove(currentFenAnalysis, uci.substring(0, 2), uci.substring(2, 4), promo)
                } catch (e: Exception) { break }
            }
            
            val moveAccuracies = mutableListOf<Double>()
            for (analysis in analysisResults) {
                val evalLoss = kotlin.math.abs(analysis.evaluationChange).coerceAtLeast(0.0)
                val winLoss = (1.0 - 1.0 / (1.0 + 10.0.pow(-evalLoss * 80.0 / 400.0))).coerceAtLeast(0.0)
                val moveAcc = (100.0 * (1.0 - 1.5 * winLoss)).coerceIn(0.0, 100.0)
                moveAccuracies.add(moveAcc)
            }

            // Add a safe fallback list for move timestamps/durations if not tracked elsewhere
            val moveTimes = listOf<Long>() 

            val detailedMoveRecords = mutableListOf<DetailedMoveRecord>()
            var ply = 0
            while (ply < sanMoves.size) {
                val whiteSan = sanMoves[ply]
                val whiteUci = uciMoves.getOrNull(ply)
                val whiteQual = analysisResults.getOrNull(ply)?.quality ?: MoveQuality.BEST
                val whiteTime = moveTimes.getOrNull(ply)?.toDouble() ?: 2.5                
                val whiteEvalScore = graph.getOrNull(ply)

                var blackSan: String? = null
                var blackUci: String? = null
                var blackQual: MoveQuality? = null
                var blackTime: Double? = null
                var blackEvalScore: Double? = null

                if (ply + 1 < sanMoves.size) {
                    blackSan = sanMoves[ply + 1]
                    blackUci = uciMoves.getOrNull(ply + 1)
                    blackQual = analysisResults.getOrNull(ply + 1)?.quality ?: MoveQuality.BEST
                    blackTime = moveTimes.getOrNull(ply + 1)?.toDouble() ?: 2.1                    
                    blackEvalScore = graph.getOrNull(ply + 1)
                }

                detailedMoveRecords.add(
                    DetailedMoveRecord(
                        moveNumber = (ply / 2) + 1,
                        whiteMoveSan = whiteSan ?: "", 
                        whiteMoveUci = whiteUci ?: "",
                        whiteTimeSec = whiteTime,
                        whiteEval = whiteEvalScore,
                        whiteQuality = whiteQual,
                        blackMoveSan = blackSan ?: "",
                        blackMoveUci = blackUci ?: "",
                        blackTimeSec = blackTime ?: 2.1,
                        blackEval = blackEvalScore,
                        blackQuality = blackQual ?: MoveQuality.BEST
                    )
                )
                ply += 2
            }

            var wAccSum = 0.0; var wAccCount = 0
            var bAccSum = 0.0; var bAccCount = 0
            // Phase-split accuracy tracking (opening=moves 1-10 each side, middlegame=11-25, endgame=26+)
            var wOpenAcc = 0.0; var wOpenCount = 0
            var bOpenAcc = 0.0; var bOpenCount = 0
            var wMidAcc = 0.0; var wMidCount = 0
            var bMidAcc = 0.0; var bMidCount = 0
            var wEndAcc = 0.0; var wEndCount = 0
            var bEndAcc = 0.0; var bEndCount = 0
            // ACPL: track centipawn loss
            var wCpLossSum = 0.0; var wCpCount = 0
            var bCpLossSum = 0.0; var bCpCount = 0
            var wBrilliant = 0; var bBrilliant = 0
            var wGreat = 0; var bGreat = 0
            var wBest = 0; var bBest = 0
            var wInaccuracy = 0; var bInaccuracy = 0
            var wMistake = 0; var bMistake = 0
            var wBlunder = 0; var bBlunder = 0

            for (idx in analysisResults.indices) {
                val acc = moveAccuracies.getOrElse(idx) { 85.0 }
                val qual = analysisResults[idx].quality
                val evalChange = analysisResults[idx].evaluationChange
                val cpLoss = (kotlin.math.abs(evalChange) * 100.0).coerceIn(0.0, 1000.0)
                val isWhiteMove = idx % 2 == 0
                // Move number for this player (1-indexed)
                val playerMoveNum = if (isWhiteMove) (idx / 2) + 1 else (idx / 2) + 1
                if (isWhiteMove) {
                    wAccSum += acc
                    wAccCount++
                    wCpLossSum += cpLoss; wCpCount++
                    when {
                        playerMoveNum <= 10 -> { wOpenAcc += acc; wOpenCount++ }
                        playerMoveNum <= 25 -> { wMidAcc += acc; wMidCount++ }
                        else -> { wEndAcc += acc; wEndCount++ }
                    }
                    when (qual) {
                        MoveQuality.BRILLIANT -> wBrilliant++
                        MoveQuality.GREAT -> wGreat++
                        MoveQuality.BEST, MoveQuality.EXCELLENT, MoveQuality.GOOD, MoveQuality.BOOK -> wBest++
                        MoveQuality.INACCURACY -> wInaccuracy++
                        MoveQuality.MISTAKE -> wMistake++
                        MoveQuality.BLUNDER -> wBlunder++
                        else -> {}
                    }
                } else {
                    bAccSum += acc
                    bAccCount++
                    bCpLossSum += cpLoss; bCpCount++
                    when {
                        playerMoveNum <= 10 -> { bOpenAcc += acc; bOpenCount++ }
                        playerMoveNum <= 25 -> { bMidAcc += acc; bMidCount++ }
                        else -> { bEndAcc += acc; bEndCount++ }
                    }
                    when (qual) {
                        MoveQuality.BRILLIANT -> bBrilliant++
                        MoveQuality.GREAT -> bGreat++
                        MoveQuality.BEST, MoveQuality.EXCELLENT, MoveQuality.GOOD, MoveQuality.BOOK -> bBest++
                        MoveQuality.INACCURACY -> bInaccuracy++
                        MoveQuality.MISTAKE -> bMistake++
                        MoveQuality.BLUNDER -> bBlunder++
                        else -> {}
                    }
                }
            }

            val finalWhiteAcc = if (wAccCount > 0) (wAccSum / wAccCount).coerceIn(35.0, 99.8) else 85.0
            val finalBlackAcc = if (bAccCount > 0) (bAccSum / bAccCount).coerceIn(35.0, 99.8) else 80.0
            val finalAccuracy = (finalWhiteAcc + finalBlackAcc) / 2.0

            // Returns fallback base rating from user profile or default 1200 baseline
            val userBaseRating = _currentUserRating.value.rapidRating?.takeIf { it > 0 }
                ?: _currentUserRating.value.blitzRating?.takeIf { it > 0 }
                ?: _currentUserRating.value.bulletRating?.takeIf { it > 0 }
                ?: 1200

            val whiteBase = if (_isPlayerWhite.value) userBaseRating else (_botLevel.value * 110 + 600)
            val blackBase = if (!_isPlayerWhite.value) userBaseRating else (_botLevel.value * 110 + 600)

            val whiteElo = (whiteBase + (finalWhiteAcc - 60.0) * 18.0).toInt().coerceIn(600, 3200)
            val blackElo = (blackBase + (finalBlackAcc - 60.0) * 18.0).toInt().coerceIn(600, 3200)

            // Adjust rating depending on outcome
            val currentSession = _activeGameSession.value
            val ratingDelta = if (currentSession?.isRated == true) {
                if (finalAccuracy > 80.0) 12 else -8
            } else 0

            val isPlayerWhite = _isPlayerWhite.value
            val userName = _userProfile.value.username.ifBlank { "Player" }
            val botLvl = _botLevel.value

            val commentText = when {
                graph.any { it > 2.0 } && graph.firstOrNull() ?: 0.0 < -1.0 -> "White turned the game around by exploiting Black's positional error."
                graph.lastOrNull() ?: 0.0 > 3.0 -> "White played an accurate game with dominant board control throughout."
                graph.lastOrNull() ?: 0.0 < -3.0 -> "Black capitalized on crucial tactical opportunities to secure victory."
                else -> "A closely contested game with high tactical complexity on both sides."
            }

            val dynamicReviewSummary = com.creativeblisslabs.omnimind.chess.shared.GameReviewSummary(
                coachComment = commentText,
                whitePlayerName = if (isPlayerWhite) userName else "Stockfish (Lv $botLvl)",
                blackPlayerName = if (!isPlayerWhite) userName else "Stockfish (Lv $botLvl)",
                whiteAccuracy = (kotlin.math.round(finalWhiteAcc * 10) / 10.0),
                blackAccuracy = (kotlin.math.round(finalBlackAcc * 10) / 10.0),
                whiteEstimatedElo = whiteElo,
                blackEstimatedElo = blackElo,
                whiteBrilliants = wBrilliant,
                blackBrilliants = bBrilliant,
                whiteGreats = wGreat,
                blackGreats = bGreat,
                whiteBests = wBest,
                blackBests = bBest,
                whiteInaccuracies = wInaccuracy,
                blackInaccuracies = bInaccuracy,
                whiteMistakes = wMistake,
                blackMistakes = bMistake,
                whiteBlunders = wBlunder,
                blackBlunders = bBlunder,
                whiteAcpl = if (wCpCount > 0) (wCpLossSum / wCpCount).toInt() else 0,
                blackAcpl = if (bCpCount > 0) (bCpLossSum / bCpCount).toInt() else 0,
                whiteOpeningAccuracy = if (wOpenCount > 0) (wOpenAcc / wOpenCount).toInt() else null,
                blackOpeningAccuracy = if (bOpenCount > 0) (bOpenAcc / bOpenCount).toInt() else null,
                whiteMiddlegameAccuracy = if (wMidCount > 0) (wMidAcc / wMidCount).toInt() else null,
                blackMiddlegameAccuracy = if (bMidCount > 0) (bMidAcc / bMidCount).toInt() else null,
                whiteEndgameAccuracy = if (wEndCount > 0) (wEndAcc / wEndCount).toInt() else null,
                blackEndgameAccuracy = if (bEndCount > 0) (bEndAcc / bEndCount).toInt() else null,
                evalGraph = graph,
                moveRecords = detailedMoveRecords
            )

            _moveAnalysis.value = analysisResults
            _gameAnalysisResult.value = GameAnalysisResult(
                accuracy = finalAccuracy,
                estimatedElo = if (isPlayerWhite) whiteElo else blackElo,
                counts = counts,
                evaluationGraph = graph,
                moveAnalyses = analysisResults,
                reviewSummary = dynamicReviewSummary
            )

            _analysisProgress.value = 1f
            
            // Auto-save the analyzed game to history
            saveCurrentGame(dynamicReviewSummary)

            delay(500)
            _analysisProgress.value = 0f

        if (currentSession?.isRated == true) {
    val format = _activeTimeFormat.value
    _currentUserRating.value = _currentUserRating.value.let { user ->
        when {
            format.contains("1") || format.contains("2") -> {
                user.copy(bulletRating = user.bulletRating?.plus(ratingDelta))
            }
            format.contains("3") || format.contains("5") -> {
                user.copy(blitzRating = user.blitzRating?.plus(ratingDelta))
            }
            else -> {
                user.copy(rapidRating = user.rapidRating?.plus(ratingDelta))
            }
        }
    }
}
        }
    }

    private var analysisJob: Job? = null

    private fun saveCurrentGame(summary: com.creativeblisslabs.omnimind.chess.shared.GameReviewSummary) {
        val session = _activeGameSession.value ?: return
        val currentPgn = generatePGN()
        val resultStr = when {
            _gameOverReason.value?.contains("White wins") == true -> "1-0"
            _gameOverReason.value?.contains("Black wins") == true -> "0-1"
            _gameOverReason.value?.contains("Draw") == true -> "1/2-1/2"
            else -> "*"
        }

        val now = Clock.System.now().toEpochMilliseconds()
        val newGame = com.creativeblisslabs.omnimind.core.chess.SavedGame(
            id = "local_$now",
            whitePlayer = summary.whitePlayerName,
            blackPlayer = summary.blackPlayerName,
            whiteRating = if (_isPlayerWhite.value) _currentUserRating.value.rapidRating else null,
            blackRating = if (!_isPlayerWhite.value) _currentUserRating.value.rapidRating else null,
            date = now,
            result = resultStr,
            pgn = currentPgn,
            mode = session.mode,
            variant = session.variant,
            whiteAccuracy = summary.whiteAccuracy,
            blackAccuracy = summary.blackAccuracy,
            event = session.label
        )

        _gameHistory.value = listOf(newGame) + _gameHistory.value
        
        // Persist to Supabase
        viewModelScope.launch {
            socketService.saveGameToHistory(newGame)
        }
    }

    fun loadGameFromHistory(game: com.creativeblisslabs.omnimind.core.chess.SavedGame) {
        viewModelScope.launch {
            loadPgn(game.pgn)
            runGameAnalysis()
        }
    }

    fun deleteGameFromHistory(gameId: String) {
        _gameHistory.value = _gameHistory.value.filter { it.id != gameId }
        viewModelScope.launch {
            socketService.deleteGameFromHistory(gameId)
        }
    }

    fun analyzePosition() {
        if (_isBotThinking.value || (_isGameActive.value && _gameMode.value == GameMode.VS_BOT)) return
        val currentSession = _activeGameSession.value
        if (currentSession?.isRated == true) {
            _evaluation.value = null
            _visualArrows.value = emptyList()
            return
        }
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch(Dispatchers.Default) {
            val settings = _engineSettings.value
            val searchDepth = (settings.searchTimeSec * 2).coerceIn(8, 22)
            val fenToAnalyze = _fen.value
            // ── CRITICAL FIX: Tell the engine which position to evaluate before starting analysis ──
            chessEngine.setPosition(fenToAnalyze)
            chessEngine.analyze(searchDepth) { eval ->
                _evaluation.value = eval
                val arrows = mutableListOf<Pair<String, String>>()
                eval.bestLine.firstOrNull()?.let { move ->
                    if (move.length >= 4) {
                        arrows.add(move.substring(0, 2) to move.substring(2, 4))
                    }
                }
                if (settings.multiPvLines > 1) {
                    eval.multiPV.drop(1).take(settings.multiPvLines - 1).forEach { pv ->
                        pv.bestLine.firstOrNull()?.let { move ->
                            if (move.length >= 4) {
                                arrows.add(move.substring(0, 2) to move.substring(2, 4))
                            }
                        }
                    }
                }
                _visualArrows.value = arrows
            }
        }
    }

    // ---- Lichess API functions ----
    fun fetchLichessPuzzleOfDay() {
        viewModelScope.launch {
            _isLichessLoading.value = true
            try {
                val response: HttpResponse = httpClient.get("https://lichess.org/api/puzzle/daily")
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    val parsed = json.decodeFromString<LichessPuzzleResponse>(body)
                    
                    var targetFen = if (parsed.game.fen.isNotBlank()) parsed.game.fen else ChessLogic.getInitialFen()
                    if (parsed.game.pgn.isNotBlank()) {
                        val moves = parsed.game.pgn.split(" ").filter { it.isNotBlank() && !it.contains(".") }
                        var currentF = ChessLogic.getInitialFen()
                        for (san in moves) {
                            val moveTriple = ChessLogic.sanToMove(currentF, san)
                            if (moveTriple != null) {
                                val (from, to, promo) = moveTriple
                                try {
                                    currentF = ChessLogic.applyMove(currentF, from, to, promo)
                                } catch (_: Exception) {}
                            }
                        }
                        targetFen = currentF
                    }

                    val opponentSetupMove = parsed.puzzle.solution.firstOrNull()
                    val playerSolution = if (parsed.puzzle.solution.size > 1) parsed.puzzle.solution.drop(1) else parsed.puzzle.solution
                    if (opponentSetupMove != null && opponentSetupMove.length >= 4) {
                        val promo = if (opponentSetupMove.length > 4) opponentSetupMove[4] else null
                        try {
                            targetFen = ChessLogic.applyMove(targetFen, opponentSetupMove.substring(0, 2), opponentSetupMove.substring(2, 4), promo)
                        } catch (_: Exception) {}
                    }

                    val pData = LichessPuzzleData(
                        id = parsed.puzzle.id,
                        fen = targetFen,
                        solution = playerSolution,
                        rating = parsed.puzzle.rating,
                        themes = parsed.puzzle.themes,
                        gameUrl = "https://lichess.org/${parsed.game.id}",
                        playerWhite = parsed.game.players.firstOrNull { it.color == "white" }?.name ?: "White",
                        playerBlack = parsed.game.players.firstOrNull { it.color == "black" }?.name ?: "Black",
                        initialPly = parsed.game.pgn.split(" ").size
                    )
                    _lichessPuzzle.value = pData
                    loadPuzzlePosition(pData.fen, pData.solution)
                }
            } catch (e: Exception) {
                _lichessFetchError.value = "Puzzle fetch failed: ${e.message}"
            } finally {
                _isLichessLoading.value = false
            }
        }
    }

    fun fetchLichessLeaderboard() {
        viewModelScope.launch {
            _isLichessLoading.value = true
            _lichessFetchError.value = null
            try {
                val categories = listOf("bullet", "blitz", "rapid", "classical")
                val combinedList = mutableListOf<LichessLeaderEntry>()

                categories.forEach { cat ->
                    // Official Lichess API Endpoint: https://lichess.org/api/player/top/10/{cat}
                    val response: HttpResponse = httpClient.get("https://lichess.org/api/player/top/10/$cat") {
                        headers { append("Accept", "application/json") }
                    }
                    if (response.status.value in 200..299) {
                        val jsonStr = response.bodyAsText()
                        val jsonElement = Json.parseToJsonElement(jsonStr).jsonObject
                        val usersArray = jsonElement["users"]?.jsonArray

                        usersArray?.forEach { userEl ->
                            val userObj = userEl.jsonObject
                            val userId = userObj["id"]?.jsonPrimitive?.content ?: "user_id"
                            val username = userObj["username"]?.jsonPrimitive?.content ?: "Player"
                            val title = userObj["title"]?.jsonPrimitive?.content
                            val perfsObj = userObj["perfs"]?.jsonObject
                            val rating = perfsObj?.get(cat)?.jsonObject?.get("rating")?.jsonPrimitive?.int ?: 0

                            combinedList.add(
                                LichessLeaderEntry(
                                    id = userId,
                                    username = username,
                                    rating = rating,
                                    category = cat,
                                    title = title
                                )
                            )
                        }
                    }
                }
                _lichessLeaderboard.value = combinedList.sortedByDescending { it.rating }
            } catch (e: Exception) {
                _lichessFetchError.value = "Leaderboard fetch failed: ${e.message}"
            } finally {
                _isLichessLoading.value = false
            }
        }
    }

    fun fetchLichessLiveGames() {
        viewModelScope.launch {
            try {
                val response: HttpResponse = httpClient.get("https://lichess.org/api/tv/channels") {
                    headers { append("Accept", "application/json") }
                }
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    val channels = json.decodeFromString<Map<String, LichessTvChannel>>(body)
                    _lichessLiveGames.value = channels.entries.map { (channel, data) ->
                        LichessGame(
                            id = data.gameId,
                            channel = channel,
                            white = data.user?.name ?: "White",
                            black = "Opponent",
                            rating = data.rating ?: 0,
                            speed = data.speed ?: "rapid"
                        )
                    }
                }
            } catch (e: Exception) {
                _lichessFetchError.value = "Live games fetch failed: ${e.message}"
            }
        }
    }

    fun fetchLichessStreamEvents() {
        viewModelScope.launch {
            try {
                val response: HttpResponse = httpClient.get("https://lichess.org/api/broadcast?nb=10") {
                    headers { append("Accept", "application/x-ndjson") }
                }
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    val events = mutableListOf<LichessStreamEvent>()

                    body.lines().forEach { line ->
                        if (line.isBlank()) return@forEach
                        try {
                            val bc = json.decodeFromString<LichessBroadcast>(line)
                            events.add(LichessStreamEvent(
                                id = bc.tour.id,
                                name = bc.tour.name,
                                description = bc.tour.description ?: "",
                                url = "https://lichess.org/broadcast/${bc.tour.slug ?: bc.tour.id}/${bc.tour.id}",
                                isLive = bc.rounds.any { it.ongoing }
                            ))
                        } catch (_: Exception) {
                            try {
                                val parsed = json.decodeFromString<List<LichessBroadcast>>(line)
                                parsed.forEach { bc ->
                                    events.add(LichessStreamEvent(
                                        id = bc.tour.id,
                                        name = bc.tour.name,
                                        description = bc.tour.description ?: "",
                                        url = "https://lichess.org/broadcast/${bc.tour.slug ?: bc.tour.id}/${bc.tour.id}",
                                        isLive = bc.rounds.any { it.ongoing }
                                    ))
                                }
                            } catch (_: Exception) { /* skip malformed line */ }
                        }
                    }

                    if (events.isNotEmpty()) {
                        _lichessStreamEvents.value = events
                    }
                }
            } catch (e: Exception) {
                _lichessFetchError.value = "Events fetch failed: ${e.message?.take(80)}"
            }
        }
    }

    // Fetch Chess.com Leaderboards (e.g. Daily, Blitz, Bullet)
    fun fetchChessComLeaderboards() {
        viewModelScope.launch {
            try {
                // Chess.com Leaderboards API: https://www.chess.com/pub/leaderboards
                val response: HttpResponse = httpClient.get("https://www.chess.com/pub/leaderboards") {
                    headers { append("Accept", "application/json") }
                }
                if (response.status.value in 200..299) {
                    // Parse or map response into entries if displaying dual platform leaderboards
                }
            } catch (e: Exception) {
                // Handle error safely
            }
        }
    }

    fun refreshAllLichessData() {
        fetchLichessPuzzleOfDay()
        fetchChessComDailyPuzzle()
        fetchLichessLeaderboard()
        fetchLichessLiveGames()
        fetchLichessStreamEvents()
    }

    fun fetchChessComDailyPuzzle() {
        viewModelScope.launch {
            try {
                // Chess.com Daily Puzzle API: https://api.chess.com/pub/puzzle
                val response: HttpResponse = httpClient.get("https://api.chess.com/pub/puzzle")
                if (response.status.value in 200..299) {
                    val body = response.bodyAsText()
                    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                    val parsed = json.decodeFromString<ChessComDailyPuzzle>(body)
                    _chessComPuzzle.value = parsed
                    
                    // If we're in puzzle mode and want to load this, we can. 
                    // Usually we might want to offer the user to switch between Lichess and Chess.com puzzles.
                }
            } catch (e: Exception) {
                // Fail silently or log
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chessEngine.stop()
        timerJob?.cancel()
        httpClient.close()
    }

    companion object
}
