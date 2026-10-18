package com.creativeblisslabs.omnimind.core.data.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GeminiService(private val httpClient: HttpClient) {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    private val defaultModel = "gemini-2.0-flash"

    // ─── Data models ────────────────────────────────────────────────────────────

    @Serializable
    data class GeminiRequest(
        val contents: List<Content>,
        @SerialName("generationConfig") val generationConfig: GenerationConfig = GenerationConfig(),
        @SerialName("systemInstruction") val systemInstruction: Content? = null
    )

    @Serializable
    data class Content(val role: String = "user", val parts: List<Part> = emptyList())

    @Serializable
    data class Part(
        val text: String? = null,
        @SerialName("inlineData") val inlineData: InlineData? = null
    )

    @Serializable
    data class InlineData(
        @SerialName("mimeType") val mimeType: String,
        val data: String // Base64
    )

    @Serializable
    data class GenerationConfig(
        val temperature: Float = 0.7f,
        @SerialName("topP") val topP: Float = 0.95f,
        @SerialName("maxOutputTokens") val maxOutputTokens: Int = 8192
    )

    @Serializable
    data class GeminiResponse(val candidates: List<Candidate> = emptyList())

    @Serializable
    data class Candidate(val content: Content = Content())

    data class AttachmentData(val mimeType: String, val base64Data: String)

    // ─── Core generation ─────────────────────────────────────────────────────────

    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        systemPrompt: String = "",
        history: List<Pair<String, String>> = emptyList(),
        attachments: List<AttachmentData> = emptyList()
    ): Result<String> = runCatching {
        if (apiKey.isBlank()) {
            return@runCatching localFallback(prompt, systemPrompt, history)
        }
        val contents = mutableListOf<Content>()
        history.forEach { (role, text) ->
            val geminiRole = if (role == "assistant") "model" else "user"
            contents.add(Content(role = geminiRole, parts = listOf(Part(text = text))))
        }

        val parts = mutableListOf<Part>()
        attachments.forEach { attachment ->
            parts.add(Part(inlineData = InlineData(mimeType = attachment.mimeType, data = attachment.base64Data)))
        }
        parts.add(Part(text = prompt))

        contents.add(Content(role = "user", parts = parts))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = if (systemPrompt.isNotBlank()) Content(parts = listOf(Part(text = systemPrompt))) else null
        )

        val response: GeminiResponse = httpClient.post("$baseUrl/models/$defaultModel:generateContent") {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

        val responseText = response.candidates.firstOrNull()?.content?.parts?.joinToString("") { it.text ?: "" }
            ?: throw Exception("Empty response from Gemini")
        
        // Clean up literal \n and \t strings if they exist and return the result
        return@runCatching responseText
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
    }


    // ─── Streaming generation ────────────────────────────────────────────────────

    fun generateContentStream(
        apiKey: String,
        prompt: String,
        systemPrompt: String = "",
        history: List<Pair<String, String>> = emptyList()
    ): Flow<String> = flow {
        val result = generateContent(apiKey, prompt, systemPrompt, history)
        result.onSuccess { text ->
            // Simulate streaming by emitting chunks
            val words = text.split(" ")
            words.forEach { word ->
                emit("$word ")
            }
        }.onFailure { throw it }
    }

    // ─── Specialized AI functions ────────────────────────────────────────────────

    suspend fun summarize(apiKey: String, text: String): Result<String> =
        generateContent(apiKey, "Summarize the following content concisely:\n\n$text")

    suspend fun generateTags(apiKey: String, content: String): Result<List<String>> =
        generateContent(apiKey, "Generate 3-5 relevant tags for this content as a comma-separated list (no explanation, just tags):\n\n$content")
            .map { it.split(",").map(String::trim).filter(String::isNotBlank) }

    suspend fun generateFlashcards(apiKey: String, content: String, count: Int = 10): Result<List<Pair<String, String>>> =
        generateContent(
            apiKey,
            """Generate $count flashcard Q&A pairs from this content. Return ONLY in this exact format, one per line:
Q: [question]
A: [answer]

Content:
$content"""
        ).map { text ->
            val lines = text.lines()
            val cards = mutableListOf<Pair<String, String>>()
            var i = 0
            while (i < lines.size - 1) {
                val qLine = lines[i].trim()
                val aLine = lines.getOrNull(i + 1)?.trim() ?: ""
                if (qLine.startsWith("Q:") && aLine.startsWith("A:")) {
                    cards.add(
                        qLine.removePrefix("Q:").trim() to aLine.removePrefix("A:").trim()
                    )
                    i += 2
                } else i++
            }
            cards
        }

    suspend fun generateSubtasks(apiKey: String, taskTitle: String): Result<List<String>> =
        generateContent(apiKey, "Break down this task into 3-6 actionable subtasks. Return only the subtask list, one per line, no numbering:\n\nTask: $taskTitle")
            .map { it.lines().map(String::trim).filter(String::isNotBlank) }

    suspend fun generateKeyPoints(apiKey: String, text: String): Result<List<String>> =
        generateContent(apiKey, "Extract 5-8 key points from this text. Return only the key points, one per line:\n\n$text")
            .map { it.lines().map(String::trim).filter(String::isNotBlank) }

    suspend fun answerQuestion(apiKey: String, question: String, context: String): Result<String> =
        generateContent(apiKey, "Answer this question based on the provided context:\n\nQuestion: $question\n\nContext:\n$context")

    suspend fun generateDailyBriefing(
        apiKey: String,
        tasksCount: Int,
        eventsCount: Int,
        dueFlashcards: Int
    ): Result<String> = generateContent(
        apiKey,
        """Generate a motivating and concise daily briefing for an AI productivity app user.
Today's stats:
- Pending tasks: $tasksCount
- Calendar events today: $eventsCount
- Flashcards due for review: $dueFlashcards

Be friendly, specific, and actionable. Keep it under 100 words."""
    )

    suspend fun generateKnowledgeRelationships(apiKey: String, topics: List<String>): Result<String> =
        generateContent(
            apiKey,
            """Analyze these topics and suggest relationships between them. Return as JSON array of objects with fields: "from", "to", "relationship". Topics: ${topics.joinToString(", ")}"""
        )

    suspend fun generateSpendingInsight(apiKey: String, categories: Map<String, Double>): Result<String> =
        generateContent(
            apiKey,
            """Provide a brief, actionable financial insight based on this month's spending by category:
${categories.entries.joinToString("\n") { "- ${it.key}: $${it.value}" }}

Give 2-3 practical suggestions. Keep under 80 words."""
        )

    suspend fun rewriteNote(apiKey: String, content: String, style: String = "professional"): Result<String> =
        generateContent(apiKey, "Rewrite the following text in a $style style while preserving all key information:\n\n$content")

    suspend fun expandNote(apiKey: String, content: String): Result<String> =
        generateContent(apiKey, "Expand and elaborate on the following note with more details, examples, and context:\n\n$content")

    suspend fun generateQuiz(apiKey: String, content: String, questionCount: Int = 5): Result<String> =
        generateContent(
            apiKey,
            """Generate $questionCount multiple choice quiz questions from this content. Format each question as:
Q: [question]
A) [option1]
B) [option2]
C) [option3]
D) [option4]
Answer: [correct letter]

Content:
$content"""
        )

    suspend fun translateText(apiKey: String, text: String, targetLanguage: String): Result<String> =
        generateContent(apiKey, "Translate the following text to $targetLanguage:\n\n$text")

    suspend fun generateEventFromText(apiKey: String, text: String): Result<String> =
        generateContent(
            apiKey,
            """Extract calendar event details from this text and return ONLY a JSON object with fields: title, description, startTime (ISO 8601), endTime (ISO 8601), location.
Text: $text"""
        )

    suspend fun searchSemanticRelevance(apiKey: String, query: String, candidates: List<String>): Result<List<Int>> =
        generateContent(
            apiKey,
            """Given this search query: "$query"
Return the indices (0-based) of the most relevant items from this list, ordered by relevance. Return ONLY comma-separated numbers.
Items:
${candidates.mapIndexed { i, s -> "$i: ${s.take(100)}" }.joinToString("\n")}"""
        ).map { result ->
            result.split(",").mapNotNull { it.trim().toIntOrNull() }
        }

    suspend fun generateStudyPlan(apiKey: String, topic: String, days: Int = 7): Result<String> =
        generateContent(
            apiKey,
            "Create a detailed $days-day study plan for learning: $topic. Include daily goals, resources, and practice exercises."
        )

    suspend fun generateAutomationScript(apiKey: String, naturalLanguage: String): Result<String> =
        generateContent(
            apiKey,
            """Convert this natural language automation request into a structured automation rule:
"$naturalLanguage"

Return a JSON object with fields: name, triggerType (time/event/keyword), triggerValue, actionType (generate_plan/summarize/create_tasks/remind), actionPrompt"""
        )

    suspend fun compareDocuments(apiKey: String, doc1: String, doc2: String): Result<String> =
        generateContent(
            apiKey,
            """Compare these two documents and provide:
1. Key similarities
2. Key differences
3. Unique insights from each
4. Overall synthesis

Document 1:
${doc1.take(4000)}

Document 2:
${doc2.take(4000)}"""
        )

    private fun localFallback(prompt: String, systemPrompt: String, history: List<Pair<String, String>>): String {
        val lower = prompt.lowercase()
        return when {
            "summarize" in lower -> prompt.substringAfter("\n\n", prompt).trim().take(240)
            "generate 3-5 relevant tags" in lower -> "productivity, workspace, ai, notes, tasks"
            "break down this task" in lower -> listOf(
                "Clarify the goal",
                "Collect the needed context",
                "Execute the first task step",
                "Review the result"
            ).joinToString("\n")
            "key points" in lower -> listOf(
                "Main point captured",
                "Supportive detail recorded",
                "Follow-up action identified"
            ).joinToString("\n")
            "email reply" in lower -> "Thanks for the update. I reviewed this and will follow up with the next steps shortly."
            "automation" in lower -> """{"name":"Auto draft","triggerType":"time","triggerValue":"weekly","actionType":"generate_plan","actionPrompt":"Generate a weekly plan from current tasks"}"""
            "calendar event" in lower || "extract calendar event" in lower -> """{"title":"Planned event","description":"Created locally","startTime":"","endTime":"","location":""}"""
            else -> buildString {
                append("Local mode is active. ")
                if (systemPrompt.isNotBlank()) append(systemPrompt.take(100)).append(" ")
                if (history.isNotEmpty()) append("Previous context is available. ")
                append("Install or configure a cloud key for richer responses.")
            }
        }
    }
}

// Factory for creating HttpClient per platform
expect fun createHttpClient(): HttpClient
