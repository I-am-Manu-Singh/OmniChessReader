package com.creativeblisslabs.omnichessreader.feature.chess.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.Parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * LichessChallengeService
 * Handles instant game creation via Lichess Open Challenge API:
 * POST https://lichess.org/api/challenge/open
 * Requires NO credentials for open challenges, and supports user personal tokens (e.g. lip_*)
 * for private, rated, or board challenges.
 */
@Serializable
data class LichessOpenChallenge(
    val id: String,
    val url: String,
    val urlWhite: String,
    val urlBlack: String,
    val isRated: Boolean = false
)

class LichessChallengeService(
    private val httpClient: HttpClient,
    private val apiToken: String? = null
) {
    suspend fun createOpenChallenge(
        clockLimitMinutes: Int = 5,
        clockIncrementSeconds: Int = 3,
        rated: Boolean = false,
        colorChoice: String = "random"
    ): LichessOpenChallenge? {
        return try {
            val limitSec = clockLimitMinutes * 60
            val formParameters = Parameters.build {
                append("clock.limit", limitSec.toString())
                append("clock.increment", clockIncrementSeconds.toString())
                append("rated", rated.toString())
                if (colorChoice.lowercase() in listOf("white", "black")) {
                    append("userColor", colorChoice.lowercase())
                }
            }

            val response = httpClient.submitForm(
                url = "https://lichess.org/api/challenge/open",
                formParameters = formParameters
            ) {
                if (!apiToken.isNullOrBlank()) {
                    header("Authorization", "Bearer $apiToken")
                }
            }

            val bodyText = response.body<String>()
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(bodyText).jsonObject

            val id = root["id"]?.jsonPrimitive?.content ?: ""
            val url = root["url"]?.jsonPrimitive?.content ?: "https://lichess.org/game/$id"
            val urlWhite = root["urlWhite"]?.jsonPrimitive?.content ?: "$url?color=white"
            val urlBlack = root["urlBlack"]?.jsonPrimitive?.content ?: "$url?color=black"

            if (id.isNotBlank()) {
                LichessOpenChallenge(
                    id = id,
                    url = url,
                    urlWhite = urlWhite,
                    urlBlack = urlBlack,
                    isRated = rated
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

