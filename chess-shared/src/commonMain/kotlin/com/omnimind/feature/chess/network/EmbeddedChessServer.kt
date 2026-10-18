package com.creativeblisslabs.omnimind.feature.chess.network

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StunCandidatePayload(
    val stunServer: String = "stun:stun.l.google.com:19302",
    val roomId: String,
    val senderIp: String? = null,
    val port: Int = 8080
)

class EmbeddedChessServer(private val client: HttpClient) {
    private var webSocketSession: DefaultClientWebSocketSession? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _p2pMessages = MutableSharedFlow<ChessSocketMessage>()
    val p2pMessages: StateFlow<ChessSocketMessage?> = _p2pMessages
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun startHostRoom(gameId: String, port: Int = 8080): String {
        val stunPayload = StunCandidatePayload(roomId = gameId, port = port)
        return ChessSocketService.createInviteLink(
            gameId = gameId,
            timeFormat = "5+3",
            variant = "CLASSIC",
            isRated = false,
            colorChoice = "white",
            hostName = "Host"
        ).inviteUrl
    }

    suspend fun connectToPeer(inviteUrlOrCode: String) {
        val parsed = ChessSocketService.parseInviteLink(inviteUrlOrCode) ?: return
        val gameId = parsed.gameId
        
        try {
            client.webSocket(host = "chess-server.omnimind.com", path = "/game/$gameId") {
                webSocketSession = this
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val message = Json.decodeFromString<ChessSocketMessage>(text)
                        _p2pMessages.emit(message)
                    }
                }
            }
        } catch (e: Exception) {
            _p2pMessages.emit(ChessSocketMessage.Error("P2P Connection Error: ${e.message}"))
        }
    }

    suspend fun broadcastMove(from: String, to: String, promotion: String? = null, remainingMs: Long = 0L) {
        val msg = ChessSocketMessage.Move(from, to, promotion, remainingMs)
        send(msg)
    }

    suspend fun broadcastResign(playerName: String) {
        send(ChessSocketMessage.Resign(playerName))
    }

    suspend fun broadcastRematch(gameId: String) {
        send(ChessSocketMessage.RematchRequest(gameId))
    }

    private suspend fun send(message: ChessSocketMessage) {
        try {
            webSocketSession?.send(Frame.Text(Json.encodeToString(message)))
        } catch (e: Exception) {
            _p2pMessages.emit(ChessSocketMessage.Error("Failed to transmit move: ${e.message}"))
        }
    }

    fun stopServer() {
        scope.cancel()
        webSocketSession = null
    }
}
