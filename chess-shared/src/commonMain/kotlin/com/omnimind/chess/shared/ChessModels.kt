package com.creativeblisslabs.omnimind.chess.shared

import com.creativeblisslabs.omnimind.core.chess.MoveAnalysis
import com.creativeblisslabs.omnimind.core.chess.MoveQuality
import com.creativeblisslabs.omnimind.core.chess.EngineVersion
import com.creativeblisslabs.omnimind.core.chess.GameMode
import com.creativeblisslabs.omnimind.core.chess.GameVariant
import kotlinx.serialization.Serializable

data class ChessTimer(
    val initialTimeMs: Long,
    val incrementMs: Long,
    val remainingTimeMs: Long,
    val isRunning: Boolean = false
)

data class EngineSettings(
    val version: EngineVersion = EngineVersion.SF17,
    val searchTimeSec: Int = 6,
    val multiPvLines: Int = 2,
    val cpuThreads: Int = 1
)

data class GameSession(
    val timeFormat: String = "5+3",
    val mode: GameMode = GameMode.VS_BOT,
    val variant: GameVariant = GameVariant.CLASSIC,
    val playerIsWhite: Boolean = true,
    val botLevel: Int = 10,
    val label: String = "Casual",
    val isRated: Boolean = false
)


data class ChessUser(
    val id: String,
    val username: String,
    val rapidRating: Int?,        // Changed from Int to Int?
    val blitzRating: Int?,        // Changed from Int to Int?
    val bulletRating: Int?,       // Changed from Int to Int?
    val classicalRating: Int?,    // Changed from Int to Int?
    val puzzleRating: Int?,       // Changed from Int to Int?
    val country: String = "WORLD",
    val title: String? = null,
    val avatarUrl: String? = null,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val blitzGames: Int = 0,
    val bulletGames: Int = 0,
    val rapidGames: Int = 0,
    val playTimeHours: Double = 0.0,
    val profileUrl: String = ""
)


data class ChessTournament(
    val id: String,
    val title: String,
    val startTime: Long,
    val playersCount: Int,
    val status: String,
    val standings: List<Pair<String, Double>> = emptyList(),
    val isLiveShow: Boolean = false
)

data class DetailedMoveRecord(
    val moveNumber: Int,
    val whiteMoveSan: String,
    val whiteMoveUci: String,
    val whiteTimeSec: Double = 1.0,
    val whiteEval: Double? = null,
    val whiteQuality: MoveQuality = MoveQuality.BEST,
    val blackMoveSan: String? = null,
    val blackMoveUci: String? = null,
    val blackTimeSec: Double? = null,
    val blackEval: Double? = null,
    val blackQuality: MoveQuality? = null,
    val variationLines: List<String> = emptyList()
)

data class GameReviewSummary(
    val coachComment: String = "",
    val whitePlayerName: String = "",
    val blackPlayerName: String = "",
    val whiteAccuracy: Double = 0.0,
    val blackAccuracy: Double = 0.0,
    val whiteEstimatedElo: Int = 0,
    val blackEstimatedElo: Int = 0,
    val whiteBrilliants: Int = 0,
    val blackBrilliants: Int = 0,
    val whiteGreats: Int = 0,
    val blackGreats: Int = 0,
    val whiteBests: Int = 0,
    val blackBests: Int = 0,
    val whiteInaccuracies: Int = 0,
    val blackInaccuracies: Int = 0,
    val whiteMistakes: Int = 0,
    val blackMistakes: Int = 0,
    val whiteMisses: Int = 0,
    val blackMisses: Int = 0,
    val whiteBlunders: Int = 0,
    val blackBlunders: Int = 0,
    // Average Centipawn Loss (like lichess ACPL)
    val whiteAcpl: Int = 0,
    val blackAcpl: Int = 0,
    // Phase accuracy percentages (null if phase not reached)
    val whiteOpeningAccuracy: Int? = null,
    val blackOpeningAccuracy: Int? = null,
    val whiteMiddlegameAccuracy: Int? = null,
    val blackMiddlegameAccuracy: Int? = null,
    val whiteEndgameAccuracy: Int? = null,
    val blackEndgameAccuracy: Int? = null,
    val evalGraph: List<Double> = emptyList(),
    val moveRecords: List<DetailedMoveRecord> = emptyList()
)

data class GameAnalysisResult(
    val accuracy: Double,
    val estimatedElo: Int,
    val ratingDelta: Int = 0,
    val counts: Map<MoveQuality, Int>,
    val evaluationGraph: List<Double>,
    val moveAnalyses: List<MoveAnalysis> = emptyList(),
    val reviewSummary: GameReviewSummary = GameReviewSummary()
)

data class ChessTheme(
    val name: String,
    val lightColor: Long,
    val darkColor: Long,
    val is3D: Boolean = false
)

data class ChessPuzzle(
    val id: String,
    val fen: String,
    val solution: List<String>,
    val instruction: String,
    val hints: List<String>,
    val rating: Int
)

data class LessonTopic(
    val title: String,
    val category: String,
    val description: String,
    val fen: String,
    val keyMoves: List<String> = emptyList(),
    val explanations: List<String> = emptyList(),
    val id: String = "",
    val eco: String = "",
    val chapterCount: Int = 0
)

@Serializable
data class OpeningPosition(
    val eco: String,
    val fen: String,
    val name: String
)

@Serializable
data class OpeningCategory(
    val name: String,
    val positions: List<OpeningPosition>
)

@Serializable
data class EndgamePosition(
    val eco: String,
    val fen: String,
    val name: String
)

data class ChessFriend(
    val user: ChessUser,
    val isOnline: Boolean,
    val currentMatchId: String? = null
)

// ─────────────────────────────────────────
//  Lichess Live Data Models (UI layer)
// ─────────────────────────────────────────

data class LichessPuzzleData(
    val id: String,
    val fen: String,
    val solution: List<String>,
    val rating: Int,
    val themes: List<String>,
    val gameUrl: String,
    val playerWhite: String,
    val playerBlack: String,
    val initialPly: Int
)

data class LichessLeaderEntry(
    val id: String,
    val username: String,
    val rating: Int,
    val category: String,   // "bullet" | "blitz" | "rapid"
    val title: String? = null
)

data class LichessGame(
    val id: String,
    val channel: String,
    val white: String,
    val black: String,
    val rating: Int,
    val speed: String
)

data class LichessStreamEvent(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val isLive: Boolean
)

// ─────────────────────────────────────────
//  Lichess API JSON response shapes
// ─────────────────────────────────────────

@Serializable
data class LichessPuzzleResponse(
    val game: LichessPuzzleGame,
    val puzzle: LichessPuzzleInfo
)

@Serializable
data class LichessPuzzleGame(
    val id: String = "",
    val fen: String = "",
    val pgn: String = "",
    val players: List<LichessPuzzlePlayer> = emptyList()
)

@Serializable
data class LichessPuzzlePlayer(
    val color: String = "",
    val name: String = "",
    val rating: Int = 0
)

@Serializable
data class LichessPuzzleInfo(
    val id: String = "",
    val rating: Int = 0,
    val solution: List<String> = emptyList(),
    val themes: List<String> = emptyList()
)

@Serializable
data class LichessPlayerListResponse(
    val bullet: List<LichessPlayerItem> = emptyList(),
    val blitz: List<LichessPlayerItem> = emptyList(),
    val rapid: List<LichessPlayerItem> = emptyList(),
    val classical: List<LichessPlayerItem> = emptyList()
)

@Serializable
data class LichessPlayerItem(
    val id: String = "",
    val username: String = "",
    val title: String? = null,
    val perfs: LichessPerfs? = null
)

@Serializable
data class LichessPerfs(
    val bullet: LichessPerfRating? = null,
    val blitz: LichessPerfRating? = null,
    val rapid: LichessPerfRating? = null,
    val classical: LichessPerfRating? = null
)

@Serializable
data class LichessPerfRating(
    val rating: Int = 0,
    val games: Int = 0
)

@Serializable
data class LichessTvChannel(
    val gameId: String = "",
    val rating: Int? = null,
    val speed: String? = null,
    val user: LichessTvUser? = null
)

@Serializable
data class LichessTvUser(
    val id: String = "",
    val name: String = ""
)

@Serializable
data class LichessBroadcast(
    val tour: LichessBroadcastTour,
    val rounds: List<LichessBroadcastRound> = emptyList()
)

@Serializable
data class LichessBroadcastTour(
    val id: String = "",
    val slug: String = "",
    val name: String = "",
    val description: String? = null
)

@Serializable
data class LichessBroadcastRound(
    val id: String = "",
    val name: String = "",
    val ongoing: Boolean = false,
    val finished: Boolean = false
)

// ─────────────────────────────────────────
//  Chess.com Puzzle Models
// ─────────────────────────────────────────

@Serializable
data class ChessComDailyPuzzle(
    val title: String = "",
    val url: String = "",
    val publish_time: Long = 0,
    val fen: String = "",
    val pgn: String = "",
    val image: String = "",
    val rating: Int = 0
)

// ─────────────────────────────────────────
//  Lichess Opening Explorer Models
// ─────────────────────────────────────────

@Serializable
data class LichessOpeningData(
    val opening: OpeningInfo? = null,
    val moves: List<OpeningMove> = emptyList(),
    val topGames: List<OpeningTopGame> = emptyList(),
    val white: Int = 0,
    val draws: Int = 0,
    val black: Int = 0
) {
    val totalGames: Int get() = white + draws + black
    val whitePercent: Float get() = if (totalGames == 0) 0f else white.toFloat() / totalGames
    val drawPercent: Float get() = if (totalGames == 0) 0f else draws.toFloat() / totalGames
    val blackPercent: Float get() = if (totalGames == 0) 0f else black.toFloat() / totalGames
}

@Serializable
data class OpeningInfo(
    val eco: String = "",
    val name: String = ""
)

@Serializable
data class OpeningMove(
    val uci: String = "",
    val san: String = "",
    val white: Int = 0,
    val draws: Int = 0,
    val black: Int = 0,
    val averageRating: Int = 0
) {
    val totalGames: Int get() = white + draws + black
    val whitePercent: Float get() = if (totalGames == 0) 0f else white.toFloat() / totalGames
    val drawPercent: Float get() = if (totalGames == 0) 0f else draws.toFloat() / totalGames
    val blackPercent: Float get() = if (totalGames == 0) 0f else black.toFloat() / totalGames
}

@Serializable
data class OpeningTopGame(
    val id: String = "",
    val white: OpeningPlayer = OpeningPlayer(),
    val black: OpeningPlayer = OpeningPlayer(),
    val winner: String? = null,
    val year: Int = 0
)

@Serializable
data class OpeningPlayer(
    val name: String = "",
    val rating: Int = 0
)

// ─────────────────────────────────────────
//  Curated Opening Theory Entry (Fallback)
// ─────────────────────────────────────────

data class OpeningTheoryEntry(
    val eco: String,
    val name: String,
    val moves: String,
    val fen: String,
    val category: String // "e4", "d4", "Flank", "Defense"
)
