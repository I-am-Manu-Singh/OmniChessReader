package com.creativeblisslabs.omnimind.feature.chess.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

/**
 * ChessRelayService — Uses Supabase Realtime as a FREE managed WebSocket relay.
 */

class ChessRelayService(private val client: HttpClient) {

    companion object {
        private const val SUPABASE_PROJECT_REF = "ycqrpkpfwpgshhgssfuc"
        private const val SUPABASE_ANON_KEY     = "sb_publishable_LKPXbrpYpEZ9Yizr-4TZwA_L5ad0JQp"

        private const val REST_URL  = "https://$SUPABASE_PROJECT_REF.supabase.co/rest/v1"
        private const val WS_URL    = "wss://$SUPABASE_PROJECT_REF.supabase.co/realtime/v1/websocket" +
                                      "?vsn=1.0.0&apikey=$SUPABASE_ANON_KEY"

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun channelName(gameId: String) = "chess-$gameId"
    }

    @Serializable
    data class ChessGameRow(
        val id: String,
        val host_name: String,
        val time_format: String = "5+3",
        val variant: String = "CLASSIC",
        val is_rated: Boolean = false,
        val color_choice: String = "white",
        val current_fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        val move_history: List<String> = emptyList(),
        val status: String = "waiting",
        val white_remaining_ms: Long? = null,
        val black_remaining_ms: Long? = null
    )

    @Serializable
    data class SavedGameRow(
        val id: String,
        val white_player: String,
        val black_player: String,
        val white_rating: Int? = null,
        val black_rating: Int? = null,
        val date: Long,
        val result: String,
        val pgn: String,
        val mode: String,
        val variant: String,
        val white_accuracy: Double? = null,
        val black_accuracy: Double? = null,
        val event: String? = null
    )

    private var session: DefaultClientWebSocketSession? = null
    private var currentGameId: String? = null

    private val _incomingMessages = MutableSharedFlow<ChessSocketMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    suspend fun connect(gameId: String, onConnected: (suspend () -> Unit)? = null) {
        currentGameId = gameId
        try {
            client.webSocket(WS_URL) {
                session = this
                subscribeToChannel(channelName(gameId))
                onConnected?.invoke()
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) handleIncomingFrame(frame.readText())
                    }
                } catch (e: Exception) {
                    _incomingMessages.emit(ChessSocketMessage.Error(e.message ?: "Connection lost"))
                } finally {
                    session = null
                }
            }
        } catch (e: Exception) {
            _incomingMessages.emit(
                ChessSocketMessage.Error("Cannot connect to relay room $gameId: ${e.message}")
            )
        }
    }

    suspend fun sendMove(from: String, to: String, promotion: String? = null, remainingMs: Long = 0L) =
        broadcast(ChessSocketMessage.Move(from, to, promotion, remainingMs))

    suspend fun sendHandshake(handshake: ChessSocketMessage.FriendHandshake) =
        broadcast(handshake)

    suspend fun sendPlayerJoined(guestName: String, guestRating: Int) =
        broadcast(ChessSocketMessage.PlayerJoined(guestName, guestRating))

    suspend fun sendResign(playerName: String) {
        currentGameId?.let { markGameFinished(it) }
        broadcast(ChessSocketMessage.Resign(playerName))
    }

    suspend fun sendRematch(gameId: String) =
        broadcast(ChessSocketMessage.RematchRequest(gameId))

    suspend fun disconnect() {
        session?.close()
        session = null
    }

    suspend fun persistGameRoom(
        gameId: String,
        hostName: String,
        timeFormat: String,
        variant: String,
        isRated: Boolean,
        colorChoice: String,
        initialFen: String? = null
    ) {
        try {
            val row = ChessGameRow(
                id = gameId,
                host_name = hostName,
                time_format = timeFormat,
                variant = variant,
                is_rated = isRated,
                color_choice = colorChoice,
                current_fen = initialFen ?: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                status = "waiting"
            )
            client.post("$REST_URL/chess_games") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ChessGameRow.serializer(), row))
            }
        } catch (_: Exception) { }
    }

    suspend fun updateGameState(gameId: String, fen: String, moveHistory: List<String>, whiteRemainingMs: Long? = null, blackRemainingMs: Long? = null) {
        try {
            val body = buildJsonObject {
                put("current_fen", fen)
                put("move_history", buildJsonArray { moveHistory.forEach { add(it) } })
                put("status", "active")
                whiteRemainingMs?.let { put("white_remaining_ms", it) }
                blackRemainingMs?.let { put("black_remaining_ms", it) }
            }
            client.patch("$REST_URL/chess_games?id=eq.$gameId") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        } catch (_: Exception) { }
    }

    suspend fun markGameFinished(gameId: String) {
        try {
            val body = """{"status":"finished"}"""
            client.patch("$REST_URL/chess_games?id=eq.$gameId") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        } catch (_: Exception) { }
    }

    suspend fun saveGameToHistory(game: com.creativeblisslabs.omnimind.core.chess.SavedGame) {
        try {
            val row = SavedGameRow(
                id = game.id,
                white_player = game.whitePlayer,
                black_player = game.blackPlayer,
                white_rating = game.whiteRating,
                black_rating = game.blackRating,
                date = game.date,
                result = game.result,
                pgn = game.pgn,
                mode = game.mode.name,
                variant = game.variant.name,
                white_accuracy = game.whiteAccuracy,
                black_accuracy = game.blackAccuracy,
                event = game.event
            )
            client.post("$REST_URL/chess_history") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SavedGameRow.serializer(), row))
            }
        } catch (_: Exception) { }
    }

    suspend fun fetchGameHistory(): List<com.creativeblisslabs.omnimind.core.chess.SavedGame> {
        return try {
            val resp = client.get("$REST_URL/chess_history?order=date.desc&limit=50") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }
            val rows = json.decodeFromString<List<SavedGameRow>>(resp.bodyAsText())
            rows.map { row ->
                com.creativeblisslabs.omnimind.core.chess.SavedGame(
                    id = row.id,
                    whitePlayer = row.white_player,
                    blackPlayer = row.black_player,
                    whiteRating = row.white_rating,
                    blackRating = row.black_rating,
                    date = row.date,
                    result = row.result,
                    pgn = row.pgn,
                    mode = com.creativeblisslabs.omnimind.core.chess.GameMode.valueOf(row.mode),
                    variant = com.creativeblisslabs.omnimind.core.chess.GameVariant.valueOf(row.variant),
                    whiteAccuracy = row.white_accuracy,
                    blackAccuracy = row.black_accuracy,
                    event = row.event
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun deleteGameFromHistory(gameId: String) {
        try {
            client.delete("$REST_URL/chess_history?id=eq.$gameId") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }
        } catch (_: Exception) { }
    }

    suspend fun fetchGameState(gameId: String): ChessGameRow? {
        return try {
            val resp = client.get("$REST_URL/chess_games?id=eq.$gameId&select=*") {
                header("apikey", SUPABASE_ANON_KEY)
                header("Authorization", "Bearer $SUPABASE_ANON_KEY")
            }
            val list = json.decodeFromString<List<ChessGameRow>>(resp.bodyAsText())
            list.firstOrNull()
        } catch (_: Exception) { null }
    }

    private suspend fun DefaultClientWebSocketSession.subscribeToChannel(channel: String) {
        val joinMsg = """{"event":"phx_join","topic":"realtime:$channel",
            "payload":{"config":{"broadcast":{"self":false},"presence":{"key":""}}},"ref":"1"}"""
        send(Frame.Text(joinMsg))
    }

    private suspend fun broadcast(message: ChessSocketMessage) {
        val gameId = currentGameId ?: return
        val payload = json.encodeToString(ChessSocketMessage.serializer(), message)
        val broadcastMsg = """{"event":"broadcast","topic":"realtime:${channelName(gameId)}",
            "payload":{"type":"chess_move","payload":$payload},"ref":null}"""
        session?.send(Frame.Text(broadcastMsg))
    }

    private suspend fun handleIncomingFrame(text: String) {
        try {
            val root = json.parseToJsonElement(text).jsonObject
            val event = root["event"]?.jsonPrimitive?.content ?: return
            if (event != "broadcast") return
            val payload = root["payload"]?.jsonObject ?: return
            val type = payload["type"]?.jsonPrimitive?.content ?: return
            if (type != "chess_move") return
            val innerPayload = payload["payload"] ?: return
            val chessMsg = json.decodeFromJsonElement(ChessSocketMessage.serializer(), innerPayload)
            _incomingMessages.emit(chessMsg)
        } catch (_: Exception) { }
    }
}
