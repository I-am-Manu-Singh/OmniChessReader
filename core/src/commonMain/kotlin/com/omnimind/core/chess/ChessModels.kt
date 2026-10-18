package com.creativeblisslabs.omnimind.core.chess

import kotlinx.serialization.Serializable

enum class GameMode {
    VS_BOT, VS_FRIEND, ONLINE, TRAINING, TOURNAMENT, PUZZLE, THEORY, PUZZLE_STREAK, PUZZLE_STORM
}

@Serializable
enum class GameVariant(val label: String, val description: String) {
    CLASSIC("Classic", "Standard chess rules"),
    CHESS960("Chess960", "Random starting position, same rules"),
    FREESTYLE("Freestyle", "Analysis mode — no move restrictions")
}

@Serializable
data class SavedGame(
    val id: String = "",
    val whitePlayer: String,
    val blackPlayer: String,
    val whiteRating: Int? = null,
    val blackRating: Int? = null,
    val date: Long, // Timestamp
    val result: String, // "1-0", "0-1", "1/2-1/2", "*"
    val pgn: String,
    val mode: GameMode,
    val variant: GameVariant,
    val whiteAccuracy: Double? = null,
    val blackAccuracy: Double? = null,
    val event: String? = null
)
