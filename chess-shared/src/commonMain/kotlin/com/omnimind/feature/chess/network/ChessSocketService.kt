package com.creativeblisslabs.omnimind.feature.chess.network

import io.ktor.client.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed class ChessSocketMessage {
    @Serializable
    data class Move(val from: String, val to: String, val promotion: String? = null, val remainingMs: Long = 0L) : ChessSocketMessage()
    @Serializable
    data class GameState(val fen: String, val moveHistory: List<String>) : ChessSocketMessage()
    @Serializable
    data class Chat(val sender: String, val message: String) : ChessSocketMessage()
    @Serializable
    data class Error(val message: String) : ChessSocketMessage()
    @Serializable
    data class FriendHandshake(
        val gameId: String,
        val hostName: String,
        val hostRating: Int,
        val timeFormat: String = "5+3",
        val variant: String = "CLASSIC",
        val isRated: Boolean = false,
        val colorChoice: String = "white",
        val isHost: Boolean = true,
        val initialFen: String? = null
    ) : ChessSocketMessage()
    @Serializable
    data class PlayerJoined(
        val guestName: String,
        val guestRating: Int
    ) : ChessSocketMessage()
    @Serializable
    data class Resign(val player: String) : ChessSocketMessage()
    @Serializable
    data class RematchRequest(val gameId: String) : ChessSocketMessage()
}

data class FriendInviteInfo(
    val gameId: String,
    val timeFormat: String,
    val variant: String,
    val isRated: Boolean,
    val colorChoice: String,
    val hostName: String,
    val inviteUrl: String
) {
    /** Human-shareable 8-character code — the part after OMNI_ (or the last 8 chars). */
    val shortCode: String get() = if (gameId.startsWith("OMNI_")) gameId.removePrefix("OMNI_") else gameId.takeLast(8)
}

/**
 * ChessSocketService — public API unchanged; internally delegates to [ChessRelayService]
 * which uses Supabase Realtime as a FREE managed WebSocket relay.
 *
 * ⚠️ Before this works cross-device you must:
 * 1. Create a FREE Supabase project at https://supabase.com
 * 2. Set SUPABASE_PROJECT_REF and SUPABASE_ANON_KEY in [ChessRelayService]
 */
class ChessSocketService(client: HttpClient) {

    private val relay = ChessRelayService(client)

    val incomingMessages = relay.incomingMessages

    suspend fun connect(gameId: String, onConnected: (suspend () -> Unit)? = null) =
        relay.connect(gameId, onConnected)

    suspend fun sendMove(from: String, to: String, promotion: String? = null, remainingMs: Long = 0L) =
        relay.sendMove(from, to, promotion, remainingMs)

    suspend fun sendHandshake(handshake: ChessSocketMessage.FriendHandshake) {
        relay.persistGameRoom(
            gameId = handshake.gameId,
            hostName = handshake.hostName,
            timeFormat = handshake.timeFormat,
            variant = handshake.variant,
            isRated = handshake.isRated,
            colorChoice = handshake.colorChoice,
            initialFen = handshake.initialFen
        )
        relay.sendHandshake(handshake)
    }

    suspend fun persistGameRoom(
        gameId: String,
        hostName: String,
        timeFormat: String,
        variant: String,
        isRated: Boolean,
        colorChoice: String,
        initialFen: String? = null
    ) = relay.persistGameRoom(gameId, hostName, timeFormat, variant, isRated, colorChoice, initialFen)

    suspend fun sendPlayerJoined(guestName: String, guestRating: Int) =
        relay.sendPlayerJoined(guestName, guestRating)

    suspend fun sendResign(playerName: String) =
        relay.sendResign(playerName)

    suspend fun sendRematch(gameId: String) =
        relay.sendRematch(gameId)

    suspend fun updateGameState(gameId: String, fen: String, moveHistory: List<String>, whiteRemainingMs: Long? = null, blackRemainingMs: Long? = null) =
        relay.updateGameState(gameId, fen, moveHistory, whiteRemainingMs, blackRemainingMs)

    suspend fun fetchGameState(gameId: String) =
        relay.fetchGameState(gameId)

    suspend fun disconnect() =
        relay.disconnect()

    suspend fun saveGameToHistory(game: com.creativeblisslabs.omnimind.core.chess.SavedGame) =
        relay.saveGameToHistory(game)

    suspend fun fetchGameHistory() =
        relay.fetchGameHistory()

    suspend fun deleteGameFromHistory(gameId: String) =
        relay.deleteGameFromHistory(gameId)


    companion object {
        fun createInviteLink(
            gameId: String,
            timeFormat: String,
            variant: String,
            isRated: Boolean,
            colorChoice: String,
            hostName: String
        ): FriendInviteInfo {
            val encodedTime = timeFormat.replace("+", "%2B")
            val link = "https://omnimind.app/chess/invite?gameId=$gameId&time=$encodedTime&variant=$variant&rated=$isRated&color=$colorChoice&host=$hostName"
            return FriendInviteInfo(
                gameId = gameId,
                timeFormat = timeFormat,
                variant = variant,
                isRated = isRated,
                colorChoice = colorChoice,
                hostName = hostName,
                inviteUrl = link
            )
        }

        fun parseInviteLink(input: String): FriendInviteInfo? {
            val clean = input.trim()
            if (clean.isBlank()) return null

            var gameId = clean
            var timeFormat = "5+3"
            var variant = "CLASSIC"
            var isRated = false
            var color = "random"
            var host = "Friend"

            if (clean.contains("/chess/invite")) {
                // Handle both https://omnimind.app/chess/invite and omnimind://chess/invite formats
                val urlString = if (clean.startsWith("omnimind://")) {
                    clean.replace("omnimind://chess", "https://omnimind.app/chess")
                } else clean

                val gameIdMatch = Regex("""gameId=([A-Za-z0-9_-]+)""").find(urlString)
                if (gameIdMatch != null) gameId = gameIdMatch.groupValues[1]

                val timeMatch = Regex("""time=([A-Za-z0-9%2B\+]+)""").find(urlString)
                if (timeMatch != null) {
                    timeFormat = timeMatch.groupValues[1].replace("%2B", "+")
                }

                val variantMatch = Regex("""variant=([A-Za-z0-9]+)""").find(urlString)
                if (variantMatch != null) variant = variantMatch.groupValues[1]

                val ratedMatch = Regex("""rated=(true|false)""").find(urlString)
                if (ratedMatch != null) isRated = ratedMatch.groupValues[1].toBoolean()

                val colorMatch = Regex("""color=([A-Za-z]+)""").find(urlString)
                if (colorMatch != null) color = colorMatch.groupValues[1]

                val hostMatch = Regex("""host=([A-Za-z0-9_-]+)""").find(urlString)
                if (hostMatch != null) host = hostMatch.groupValues[1]

                // Fallback: If gameId was not found in params but it's a path like /invite/XYZ
                if (gameId == clean && clean.contains("/invite/")) {
                    val pathId = clean.substringAfter("/invite/").substringBefore("?")
                    if (pathId.isNotBlank()) gameId = pathId
                }

            } else if (clean.startsWith("omnimind://") || clean.startsWith("http")) {
                // Bare deep-link URL without params — extract last path segment
                val lastPart = clean.substringAfterLast("/").substringBefore("?")
                if (lastPart.isNotBlank()) gameId = lastPart

            } else if (clean.matches(Regex("[A-Za-z0-9]{4,12}"))) {
                // SHORT CODE: pure alphanumeric (4-12 chars) — treat as the suffix after OMNI_
                // The host's time format / variant will arrive via FriendHandshake over the socket.
                gameId = "OMNI_$clean"

            } else if (clean.startsWith("OMNI_")) {
                // Full gameId like OMNI_12345678 entered directly
                gameId = clean
            }

            return createInviteLink(
                gameId = gameId,
                timeFormat = timeFormat,
                variant = variant,
                isRated = isRated,
                colorChoice = color,
                hostName = host
            )
        }
    }
}
