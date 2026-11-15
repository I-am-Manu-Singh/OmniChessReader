package com.creativeblisslabs.omnichessreader.feature.chess.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Parameters
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * LichessBoardService
 * Implements the official Lichess Board API & OAuth2 PKCE flow for playing games inside the app:
 * 1. PKCE Authorization URL generation (challenge:write, board:play)
 * 2. Token exchange with code_verifier
 * 3. Persistent incoming event stream: GET /api/stream/event (ND-JSON)
 * 4. Real-time board state stream: GET /api/board/game/stream/{gameId} (ND-JSON)
 * 5. Move submission: POST /api/board/game/{gameId}/move/{move}
 * 6. Resign / Abort / Draw handling
 * 
 * Rules enforced:
 * - Allowed Time Controls: Rapid, Classical, Correspondence (Blitz only direct/bulk; Bullet forbidden).
 * - No engine assistance: strictly disable evaluation and suggestions during Board API games.
 */
@Serializable
data class LichessOAuthTokens(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long? = null
)

@Serializable
sealed class LichessStreamEvent {
    data class GameStart(
        val gameId: String,
        val fullId: String? = null,
        val fen: String? = null,
        val color: String? = null,
        val isMyTurn: Boolean = false,
        val opponentName: String? = null,
        val opponentRating: Int? = null,
        val speed: String? = null,
        val rated: Boolean = false
    ) : LichessStreamEvent()

    data class GameFinish(
        val gameId: String,
        val winner: String? = null,
        val status: String? = null
    ) : LichessStreamEvent()

    data class ChallengeReceived(
        val challengeId: String,
        val challengerName: String,
        val challengerRating: Int,
        val speed: String,
        val rated: Boolean,
        val color: String
    ) : LichessStreamEvent()

    data class RawEvent(val type: String, val rawJson: String) : LichessStreamEvent()
}

@Serializable
data class LichessGameState(
    val type: String,
    val gameId: String? = null,
    val moves: String = "",
    val wtime: Long? = null,
    val btime: Long? = null,
    val winc: Long? = null,
    val binc: Long? = null,
    val status: String? = null,
    val winner: String? = null,
    val initialFen: String = "startpos",
    val whitePlayer: String? = null,
    val blackPlayer: String? = null,
    val isMyTurn: Boolean = false
)

class LichessBoardService(
    private val httpClient: HttpClient,
    private var accessToken: String? = null
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun setAccessToken(token: String?) {
        this.accessToken = token
    }

    fun getAccessToken(): String? = accessToken

    /**
     * Constructs the PKCE Authorization URL for "Login with Lichess".
     * Scopes: challenge:write (create/accept challenges), board:play (stream & submit moves), preference:read.
     */
    fun createAuthorizationUrl(
        clientId: String = "omnichessreader",
        redirectUri: String = "omnichess://oauth/lichess",
        codeChallenge: String,
        state: String
    ): String {
        val scopes = "challenge:write board:play preference:read"
        val encodedRedirect = redirectUri.replace(":", "%3A").replace("/", "%2F")
        val encodedScopes = scopes.replace(" ", "%20").replace(":", "%3A")
        return "https://lichess.org/oauth" +
                "?response_type=code" +
                "&client_id=$clientId" +
                "&redirect_uri=$encodedRedirect" +
                "&scope=$encodedScopes" +
                "&code_challenge_method=S256" +
                "&code_challenge=$codeChallenge" +
                "&state=$state"
    }

    /**
     * Exchanges the authorization code + code_verifier for an OAuth access token.
     */
    suspend fun exchangeCodeForToken(
        code: String,
        codeVerifier: String,
        clientId: String = "omnichessreader",
        redirectUri: String = "omnichess://oauth/lichess"
    ): Result<LichessOAuthTokens> = withContext(Dispatchers.Default) {
        try {
            val response = httpClient.submitForm(
                url = "https://lichess.org/api/token",
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("client_id", clientId)
                    append("redirect_uri", redirectUri)
                    append("code", code)
                    append("code_verifier", codeVerifier)
                }
            )
            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            val token = root["access_token"]?.jsonPrimitive?.content
            if (!token.isNullOrBlank()) {
                accessToken = token
                Result.success(
                    LichessOAuthTokens(
                        accessToken = token,
                        tokenType = root["token_type"]?.jsonPrimitive?.content ?: "Bearer",
                        expiresIn = root["expires_in"]?.jsonPrimitive?.longOrNull
                    )
                )
            } else {
                Result.failure(Exception("No access_token in response: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streams incoming game start / challenge events in real-time (ND-JSON).
     * Endpoint: GET /api/stream/event
     */
    fun streamEvents(): Flow<LichessStreamEvent> = flow {
        val token = accessToken ?: return@flow
        httpClient.prepareGet("https://lichess.org/api/stream/event") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/x-ndjson")
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                if (line.isBlank()) continue
                try {
                    val root = json.parseToJsonElement(line).jsonObject
                    val type = root["type"]?.jsonPrimitive?.content ?: ""
                    when (type) {
                        "gameStart" -> {
                            val game = root["game"]?.jsonObject
                            val gameId = game?.get("id")?.jsonPrimitive?.content ?: ""
                            val fullId = game?.get("fullId")?.jsonPrimitive?.content
                            val fen = game?.get("fen")?.jsonPrimitive?.content
                            val color = game?.get("color")?.jsonPrimitive?.content
                            val isMyTurn = game?.get("isMyTurn")?.jsonPrimitive?.booleanOrNull ?: false
                            val opponent = game?.get("opponent")?.jsonObject
                            val opName = opponent?.get("username")?.jsonPrimitive?.content
                            val opRating = opponent?.get("rating")?.jsonPrimitive?.intOrNull
                            val speed = game?.get("speed")?.jsonPrimitive?.content
                            val rated = game?.get("rated")?.jsonPrimitive?.booleanOrNull ?: false

                            emit(
                                LichessStreamEvent.GameStart(
                                    gameId = gameId,
                                    fullId = fullId,
                                    fen = fen,
                                    color = color,
                                    isMyTurn = isMyTurn,
                                    opponentName = opName,
                                    opponentRating = opRating,
                                    speed = speed,
                                    rated = rated
                                )
                            )
                        }
                        "gameFinish" -> {
                            val game = root["game"]?.jsonObject
                            val gameId = game?.get("id")?.jsonPrimitive?.content ?: ""
                            val winner = game?.get("winner")?.jsonPrimitive?.content
                            val status = game?.get("status")?.jsonObject?.get("name")?.jsonPrimitive?.content
                            emit(LichessStreamEvent.GameFinish(gameId, winner, status))
                        }
                        "challenge" -> {
                            val c = root["challenge"]?.jsonObject
                            val cid = c?.get("id")?.jsonPrimitive?.content ?: ""
                            val challenger = c?.get("challenger")?.jsonObject
                            val cName = challenger?.get("name")?.jsonPrimitive?.content ?: "Player"
                            val cRating = challenger?.get("rating")?.jsonPrimitive?.intOrNull ?: 1500
                            val speed = c?.get("speed")?.jsonPrimitive?.content ?: "rapid"
                            val rated = c?.get("rated")?.jsonPrimitive?.booleanOrNull ?: false
                            val color = c?.get("color")?.jsonPrimitive?.content ?: "random"
                            emit(LichessStreamEvent.ChallengeReceived(cid, cName, cRating, speed, rated, color))
                        }
                        else -> {
                            emit(LichessStreamEvent.RawEvent(type, line))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Streams live board states and moves for an active game (ND-JSON).
     * Endpoint: GET /api/board/game/stream/{gameId}
     */
    fun streamGame(gameId: String): Flow<LichessGameState> = flow {
        val token = accessToken ?: return@flow
        httpClient.prepareGet("https://lichess.org/api/board/game/stream/$gameId") {
            header("Authorization", "Bearer $token")
            header("Accept", "application/x-ndjson")
        }.execute { response ->
            val channel = response.bodyAsChannel()
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: continue
                if (line.isBlank()) continue
                try {
                    val root = json.parseToJsonElement(line).jsonObject
                    val type = root["type"]?.jsonPrimitive?.content ?: ""
                    when (type) {
                        "gameFull" -> {
                            val state = root["state"]?.jsonObject
                            val moves = state?.get("moves")?.jsonPrimitive?.content ?: ""
                            val wtime = state?.get("wtime")?.jsonPrimitive?.longOrNull
                            val btime = state?.get("btime")?.jsonPrimitive?.longOrNull
                            val winc = state?.get("winc")?.jsonPrimitive?.longOrNull
                            val binc = state?.get("binc")?.jsonPrimitive?.longOrNull
                            val status = state?.get("status")?.jsonPrimitive?.content
                            val initialFen = root["initialFen"]?.jsonPrimitive?.content ?: "startpos"
                            val white = root["white"]?.jsonObject?.get("name")?.jsonPrimitive?.content
                            val black = root["black"]?.jsonObject?.get("name")?.jsonPrimitive?.content

                            emit(
                                LichessGameState(
                                    type = "gameFull",
                                    gameId = gameId,
                                    moves = moves,
                                    wtime = wtime,
                                    btime = btime,
                                    winc = winc,
                                    binc = binc,
                                    status = status,
                                    initialFen = initialFen,
                                    whitePlayer = white,
                                    blackPlayer = black
                                )
                            )
                        }
                        "gameState" -> {
                            val moves = root["moves"]?.jsonPrimitive?.content ?: ""
                            val wtime = root["wtime"]?.jsonPrimitive?.longOrNull
                            val btime = root["btime"]?.jsonPrimitive?.longOrNull
                            val winc = root["winc"]?.jsonPrimitive?.longOrNull
                            val binc = root["binc"]?.jsonPrimitive?.longOrNull
                            val status = root["status"]?.jsonPrimitive?.content
                            val winner = root["winner"]?.jsonPrimitive?.content

                            emit(
                                LichessGameState(
                                    type = "gameState",
                                    gameId = gameId,
                                    moves = moves,
                                    wtime = wtime,
                                    btime = btime,
                                    winc = winc,
                                    binc = binc,
                                    status = status,
                                    winner = winner
                                )
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Submits a move in UCI format (e.g. "e2e4", "e7e8q") via the Board API.
     * Endpoint: POST /api/board/game/{gameId}/move/{move}
     */
    suspend fun submitMove(gameId: String, uciMove: String): Result<Boolean> = withContext(Dispatchers.Default) {
        val token = accessToken ?: return@withContext Result.failure(Exception("No token configured"))
        try {
            val response = httpClient.post("https://lichess.org/api/board/game/$gameId/move/$uciMove") {
                header("Authorization", "Bearer $token")
            }
            if (response.status.value in 200..299) {
                Result.success(true)
            } else {
                Result.failure(Exception("Move submission failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resigns an ongoing game.
     * Endpoint: POST /api/board/game/{gameId}/resign
     */
    suspend fun resignGame(gameId: String): Boolean = withContext(Dispatchers.Default) {
        val token = accessToken ?: return@withContext false
        try {
            val response = httpClient.post("https://lichess.org/api/board/game/$gameId/resign") {
                header("Authorization", "Bearer $token")
            }
            response.status.value in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Accepts an incoming challenge.
     * Endpoint: POST /api/challenge/{challengeId}/accept
     */
    suspend fun acceptChallenge(challengeId: String): Boolean = withContext(Dispatchers.Default) {
        val token = accessToken ?: return@withContext false
        try {
            val response = httpClient.post("https://lichess.org/api/challenge/$challengeId/accept") {
                header("Authorization", "Bearer $token")
            }
            response.status.value in 200..299
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Creates an authorized open challenge with allowed time control rules (Rapid / Classical / Correspondence).
     * Rule: Open challenges via board:play strictly forbid Bullet.
     */
    suspend fun createCompliantChallenge(
        minutes: Int = 10,
        increment: Int = 0,
        rated: Boolean = false,
        colorChoice: String = "random"
    ): Result<String> = withContext(Dispatchers.Default) {
        val token = accessToken ?: return@withContext Result.failure(Exception("No token configured"))
        try {
            val validMinutes = minutes.coerceAtLeast(10) // Rapid / Classical constraint
            val limitSec = validMinutes * 60
            val response = httpClient.submitForm(
                url = "https://lichess.org/api/challenge/open",
                formParameters = Parameters.build {
                    append("clock.limit", limitSec.toString())
                    append("clock.increment", increment.toString())
                    append("rated", rated.toString())
                    if (colorChoice in listOf("white", "black")) {
                        append("userColor", colorChoice)
                    }
                }
            ) {
                header("Authorization", "Bearer $token")
            }
            val body = response.body<String>()
            val root = json.parseToJsonElement(body).jsonObject
            val url = root["url"]?.jsonPrimitive?.content
            if (!url.isNullOrBlank()) {
                Result.success(url)
            } else {
                Result.failure(Exception("Failed to get challenge URL: $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

