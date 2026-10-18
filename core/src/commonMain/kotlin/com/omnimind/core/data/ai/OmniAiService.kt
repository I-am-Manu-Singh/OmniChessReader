package com.creativeblisslabs.omnimind.core.data.ai

import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import com.creativeblisslabs.omnimind.core.data.ai.AiProvider
import com.creativeblisslabs.omnimind.core.media.MediaProcessor
import kotlinx.coroutines.flow.first

class OmniAiService(
    private val cloud: GeminiService,
    private val prefs: AppPreferences,
    private val runtime: OnDeviceAiRuntime,
    private val mediaProcessor: MediaProcessor
) {
    suspend fun generateContent(
        apiKey: String,
        prompt: String,
        systemPrompt: String = "",
        history: List<Pair<String, String>> = emptyList(),
        attachments: List<GeminiService.AttachmentData> = emptyList()
    ): Result<String> = if (apiKey.isNotBlank()) {
        cloud.generateContent(apiKey, prompt, systemPrompt, history, attachments)
    } else {
        generateContent(prompt, systemPrompt, history, attachments)
    }

    suspend fun generateContent(
        prompt: String,
        systemPrompt: String = "",
        history: List<Pair<String, String>> = emptyList(),
        attachments: List<GeminiService.AttachmentData> = emptyList()
    ): Result<String> {
        val provider = prefs.aiProviderValue()
        val apiKey = prefs.geminiApiKeyValue()
        val localReady = runtime.isReady()
        
        // Use local if explicitly LOCAL or if (LOCAL_FIRST or AUTO) and local LLM is ready
        val shouldTryLocal = provider == AiProvider.LOCAL || 
                            ((provider == AiProvider.LOCAL_FIRST || provider == AiProvider.AUTO) && localReady)

        // if (shouldTryLocal) {
        //     val enrichedPrompt = buildString {
        //         if (attachments.isNotEmpty()) {
        //             appendLine("--- ATTACHED CONTEXT & MEDIA ---")
        //             attachments.forEachIndexed { idx, att ->
        //                 when {
        //                     att.mimeType.startsWith("image/") -> {
        //                         if (runtime.isReady(LocalModelType.VISION_CLASSIFIER)) {
        //                             val visionRes = runtime.analyzeImage(att.base64Data, "").getOrNull()
        //                             if (!visionRes.isNullOrBlank()) {
        //                                 appendLine("[Attachment #${idx + 1} Image Analysis]: $visionRes")
        //                             }
        //                         }
        //                     }
        //                     else -> {
        //                         // Text, code, markdown or document attachment
        //                         val decoded = runCatching {
        //                             @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
        //                             kotlin.io.encoding.Base64.decode(att.base64Data).decodeToString()
        //                         }.getOrDefault("")
        //                         if (decoded.isNotBlank()) {
        //                             appendLine("[Attachment #${idx + 1} File Content (${att.mimeType})]:")
        //                             appendLine(decoded.take(4000))
        //                         }
        //                     }
        //                 }
        //             }
        //             appendLine("--- END ATTACHMENTS ---")
        //             appendLine()
        //         }
        //         append(prompt)
        //     }

        //     val localRes = runtime.generate(enrichedPrompt, systemPrompt, history)
        //     if (localRes.isSuccess) return localRes

        //     // If local model failed, try Cloud Gemini fallback if API key exists
        //     if (apiKey.isNotBlank()) {
        //         val cloudRes = cloud.generateContent(apiKey, prompt, systemPrompt, history, attachments)
        //         if (cloudRes.isSuccess) return cloudRes
        //     }

        //     val err = localRes.exceptionOrNull()?.message ?: "Local LLM is not installed or failed to initialize."
        //     return Result.failure(Exception(err))
        // }

        // Fallback to Cloud if API key exists
        if (apiKey.isNotBlank()) {
            val cloudRes = cloud.generateContent(apiKey, prompt, systemPrompt, history, attachments)
            if (cloudRes.isSuccess) return cloudRes
        }

        return Result.failure(Exception("No LLM model installed. Please download a model pack in Settings or set a Gemini API key."))
    }

    suspend fun summarize(text: String): Result<String> = generateContent("Summarize the following content concisely:\n\n$text")
    suspend fun summarize(apiKey: String, text: String): Result<String> = generateContent(apiKey, "Summarize the following content concisely:\n\n$text")
    suspend fun generateTags(content: String): Result<List<String>> =
        generateContent("Generate 3-5 relevant tags for this content. Return ONLY a JSON array of strings.\n\n$content")
            .map { parseStringArrayJson(it) }
    suspend fun generateTags(apiKey: String, content: String): Result<List<String>> =
        generateContent(apiKey, "Generate 3-5 relevant tags for this content. Return ONLY a JSON array of strings.\n\n$content")
            .map { parseStringArrayJson(it) }
    suspend fun generateFlashcards(apiKey: String, content: String, count: Int = 10): Result<List<Pair<String, String>>> =
        generateContent(
            apiKey,
            """Generate $count flashcard Q&A pairs from this content. 
Return ONLY a JSON array of objects with "question" and "answer" fields.
Example: [{"question": "What is AI?", "answer": "Artificial Intelligence"}]

Content:
$content"""
        ).map { parseFlashcardsJson(it) }
    suspend fun generateFlashcards(content: String, count: Int = 10): Result<List<Pair<String, String>>> =
        generateContent(
            """Generate $count flashcard Q&A pairs from this content.
Return ONLY a JSON array of objects with "question" and "answer" fields.
Example: [{"question": "What is AI?", "answer": "Artificial Intelligence"}]

Content:
$content"""
        ).map { parseFlashcardsJson(it) }
    suspend fun generateSubtasks(taskTitle: String): Result<List<String>> =
        generateContent("Break down this task into 3-6 actionable subtasks. Return ONLY a JSON array of strings.\n\nTask: $taskTitle")
            .map { parseStringArrayJson(it) }
    suspend fun generateSubtasks(apiKey: String, taskTitle: String): Result<List<String>> =
        generateContent(apiKey, "Break down this task into 3-6 actionable subtasks. Return ONLY a JSON array of strings.\n\nTask: $taskTitle")
            .map { parseStringArrayJson(it) }
    suspend fun generateKeyPoints(content: String): Result<List<String>> =
        generateContent("Extract 5-8 key points from this text. Return ONLY a JSON array of strings.\n\n$content")
            .map { parseStringArrayJson(it) }
    suspend fun generateKeyPoints(apiKey: String, content: String): Result<List<String>> =
        generateContent(apiKey, "Extract 5-8 key points from this text. Return ONLY a JSON array of strings.\n\n$content")
            .map { parseStringArrayJson(it) }
    suspend fun rewriteNote(content: String, style: String = "professional"): Result<String> =
        generateContent("Rewrite the following text in a $style style while preserving all key information:\n\n$content")
    suspend fun rewriteNote(apiKey: String, content: String, style: String = "professional"): Result<String> =
        generateContent(apiKey, "Rewrite the following text in a $style style while preserving all key information:\n\n$content")
    suspend fun answerQuestion(question: String, context: String): Result<String> =
        generateContent("Answer this question based on the provided context:\n\nQuestion: $question\n\nContext:\n$context")
    suspend fun answerQuestion(apiKey: String, question: String, context: String): Result<String> =
        generateContent(apiKey, "Answer this question based on the provided context:\n\nQuestion: $question\n\nContext:\n$context")
    suspend fun generateDailyBriefing(tasksCount: Int, eventsCount: Int, dueFlashcards: Int): Result<String> =
        generateContent(
            """Generate a motivating and concise daily briefing for an AI productivity app user.
Today's stats:
- Pending tasks: $tasksCount
- Calendar events today: $eventsCount
- Flashcards due for review: $dueFlashcards

Be friendly, specific, and actionable. Keep it under 100 words."""
        )
    suspend fun generateDailyBriefing(apiKey: String, tasksCount: Int, eventsCount: Int, dueFlashcards: Int): Result<String> =
        generateContent(
            apiKey,
            """Generate a motivating and concise daily briefing for an AI productivity app user.
Today's stats:
- Pending tasks: $tasksCount
- Calendar events today: $eventsCount
- Flashcards due for review: $dueFlashcards

Be friendly, specific, and actionable. Keep it under 100 words."""
        )
    suspend fun generateKnowledgeRelationships(topics: List<String>): Result<String> =
        generateContent("Analyze these topics and suggest relationships between them. Return as JSON array of objects with fields: \"from\", \"to\", \"relationship\". Topics: ${topics.joinToString(", ")}")
    suspend fun generateKnowledgeRelationships(apiKey: String, topics: List<String>): Result<String> =
        generateContent(apiKey, "Analyze these topics and suggest relationships between them. Return as JSON array of objects with fields: \"from\", \"to\", \"relationship\". Topics: ${topics.joinToString(", ")}")
    suspend fun generateSpendingInsight(categories: Map<String, Double>): Result<String> =
        generateContent(
            """Provide a brief, actionable financial insight based on this month's spending by category:
${categories.entries.joinToString("\n") { "- ${it.key}: $${it.value}" }}

Return ONLY a JSON object with "insight" (summary) and "suggestions" (array of strings).
Example: {"insight": "You spent 40% more on Food this month.", "suggestions": ["Consider meal prepping", "Reduce dining out"]}"""
        ).map { parseSpendingInsightJson(it) }
    suspend fun generateSpendingInsight(apiKey: String, categories: Map<String, Double>): Result<String> =
        generateContent(
            apiKey,
            """Provide a brief, actionable financial insight based on this month's spending by category:
${categories.entries.joinToString("\n") { "- ${it.key}: $${it.value}" }}

Return ONLY a JSON object with "insight" (summary) and "suggestions" (array of strings).
Example: {"insight": "You spent 40% more on Food this month.", "suggestions": ["Consider meal prepping", "Reduce dining out"]}"""
        ).map { parseSpendingInsightJson(it) }
    suspend fun expandNote(content: String): Result<String> =
        generateContent("Expand and elaborate on the following note with more details, examples, and context:\n\n$content")
    suspend fun expandNote(apiKey: String, content: String): Result<String> =
        generateContent(apiKey, "Expand and elaborate on the following note with more details, examples, and context:\n\n$content")
    suspend fun generateQuiz(content: String, questionCount: Int = 5): Result<String> =
        generateContent(
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
    suspend fun translateText(text: String, targetLanguage: String, sourceLanguage: String? = null): Result<String> {
        val aiProvider = prefs.aiProviderValue()
        val isLocal = aiProvider == AiProvider.LOCAL || aiProvider == AiProvider.LOCAL_FIRST || aiProvider == AiProvider.AUTO
        if (isLocal) {
            val localRes = runtime.translateText(text, targetLanguage, sourceLanguage)
            if (localRes.isSuccess) return localRes
        }

        val prompt = if (sourceLanguage != null) {
            "Translate the following text from $sourceLanguage to $targetLanguage:\n\n$text"
        } else {
            "Translate the following text to $targetLanguage:\n\n$text"
        }
        return generateContent(prompt)
    }

    suspend fun translateText(apiKey: String, text: String, targetLanguage: String, sourceLanguage: String? = null): Result<String> {
        val prompt = if (sourceLanguage != null) {
            "Translate the following text from $sourceLanguage to $targetLanguage:\n\n$text"
        } else {
            "Translate the following text to $targetLanguage:\n\n$text"
        }
        return generateContent(apiKey, prompt)
    }
    suspend fun generateEventFromText(text: String): Result<String> =
        generateContent("Extract calendar event details from this text and return ONLY a valid JSON object with fields: \"title\", \"description\", \"startTime\" (ISO 8601), \"endTime\" (ISO 8601), \"location\". Ensure dates are in ISO 8601 format. If time is not specified, default to 9 AM today. Text: $text")

    suspend fun suggestEvents(currentEvents: List<String>): Result<List<String>> =
        generateContent(
            """Based on these existing events, suggest 3-5 productive or wellness-focused events to fill gaps or enhance the schedule.
Return ONLY a JSON array of strings, where each string is a concise event title with a suggested time (e.g., "Deep Work - 2:00 PM").

Current Events:
${currentEvents.joinToString("\n")}"""
        ).map { parseStringArrayJson(it) }
    suspend fun generateEventFromText(apiKey: String, text: String): Result<String> =
        generateContent(apiKey, "Extract calendar event details from this text and return ONLY a valid JSON object with fields: \"title\", \"description\", \"startTime\" (ISO 8601), \"endTime\" (ISO 8601), \"location\". Ensure dates are in ISO 8601 format. If time is not specified, default to 9 AM today. Text: $text")
    suspend fun generateAutomationScript(naturalLanguage: String): Result<String> =
        generateContent(
            "Convert this natural language automation request into a structured automation rule. " +
            "Return ONLY a valid JSON object with fields: \"name\", \"triggerType\" (time/event/keyword), \"triggerValue\", \"actionType\" (generate_plan/summarize/create_tasks/remind), \"actionPrompt\".\n" +
            "Request: \"$naturalLanguage\""
        )
    suspend fun generateAutomationScript(apiKey: String, naturalLanguage: String): Result<String> =
        generateContent(
            apiKey,
            "Convert this natural language automation request into a structured automation rule. " +
            "Return ONLY a valid JSON object with fields: \"name\", \"triggerType\" (time/event/keyword), \"triggerValue\", \"actionType\" (generate_plan/summarize/create_tasks/remind), \"actionPrompt\".\n" +
            "Request: \"$naturalLanguage\""
        )
    suspend fun generateStudyPlan(topic: String, days: Int = 7): Result<String> =
        generateContent("Create a detailed $days-day study plan for learning: $topic. Include daily goals, resources, and practice exercises.")
    suspend fun generateStudyPlan(apiKey: String, topic: String, days: Int = 7): Result<String> =
        generateContent(apiKey, "Create a detailed $days-day study plan for learning: $topic. Include daily goals, resources, and practice exercises.")
    suspend fun generateProductivityTrends(stats: Map<String, Any>): Result<List<String>> =
        generateContent(
            """Analyze these productivity metrics and provide a concise trend analysis and 3 actionable recommendations:
${stats.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}

Return ONLY a JSON array of recommendation strings.
Example: ["Review high-priority tasks first", "Schedule a deep work block"]"""
        ).map { parseStringArrayJson(it) }
    suspend fun generateProductivityTrends(apiKey: String, stats: Map<String, Any>): Result<List<String>> =
        generateContent(
            apiKey,
            """Analyze these productivity metrics and provide a concise trend analysis and 3 actionable recommendations:
${stats.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}

Return ONLY a JSON array of recommendation strings.
Example: ["Review high-priority tasks first", "Schedule a deep work block"]"""
        ).map { parseStringArrayJson(it) }
    suspend fun draftEmailReply(emailContent: String, tone: String = "professional"): Result<String> =
        generateContent("Draft a $tone reply to the following email:\n\n$emailContent")
    suspend fun draftEmailReply(apiKey: String, emailContent: String, tone: String = "professional"): Result<String> =
        generateContent(apiKey, "Draft a $tone reply to the following email:\n\n$emailContent")
    suspend fun compareDocuments(docs: List<Pair<String, String>>): Result<String> {
        val apiKey = prefs.geminiApiKeyValue()
        if (apiKey.isBlank()) return Result.failure(Exception("Cloud API Key required for document comparison"))
        
        val prompt = buildString {
            appendLine("Compare these ${docs.size} documents and provide a comprehensive analysis including:")
            appendLine("1. A comparative matrix of key features/topics")
            appendLine("2. Shared themes and major differences")
            appendLine("3. Unique insights per document")
            appendLine("4. Synthesis and recommendations")
            appendLine()
            docs.forEachIndexed { index, (name, content) ->
                appendLine("Document ${index + 1}: $name")
                appendLine(content.take(3000))
                appendLine("---")
            }
        }
        
        return cloud.generateContent(apiKey, prompt)
    }

    suspend fun compareDocuments(apiKey: String, docs: List<Pair<String, String>>): Result<String> {
        val prompt = buildString {
            appendLine("Compare these ${docs.size} documents and provide a comprehensive analysis including:")
            appendLine("1. A comparative matrix of key features/topics")
            appendLine("2. Shared themes and major differences")
            appendLine("3. Unique insights per document")
            appendLine("4. Synthesis and recommendations")
            appendLine()
            docs.forEachIndexed { index, (name, content) ->
                appendLine("Document ${index + 1}: $name")
                appendLine(content.take(3000))
                appendLine("---")
            }
        }
        return cloud.generateContent(apiKey, prompt)
    }
    suspend fun searchSemanticRelevance(query: String, candidates: List<String>): Result<List<Int>> =
        generateContent(
            """Given this search query: "$query"
Return the indices (0-based) of the most relevant items from this list, ordered by relevance. 
Return ONLY a JSON array of integers.
Example: [2, 0, 5]

Items:
${candidates.mapIndexed { i, s -> "$i: ${s.take(100)}" }.joinToString("\n")}"""
        ).map { parseIntegerArrayJson(it) }
    suspend fun searchSemanticRelevance(apiKey: String, query: String, candidates: List<String>): Result<List<Int>> =
        generateContent(
            apiKey,
            """Given this search query: "$query"
Return the indices (0-based) of the most relevant items from this list, ordered by relevance.
Return ONLY a JSON array of integers.
Example: [2, 0, 5]

Items:
${candidates.mapIndexed { i, s -> "$i: ${s.take(100)}" }.joinToString("\n")}"""
        ).map { parseIntegerArrayJson(it) }

    // suspend fun analyzeImage(image: Any, prompt: String = ""): Result<String> {
    //     val aiProvider = prefs.aiProviderValue()
    //     return if ((aiProvider == AiProvider.LOCAL || aiProvider == AiProvider.LOCAL_FIRST) && 
    //         runtime.isReady(LocalModelType.VISION_CLASSIFIER)) {
    //         runtime.analyzeImage(image, prompt)
    //     } else {
    //         // Future: Implement cloud image analysis fallback
    //         Result.failure(Exception("Cloud image analysis pending implementation. Please ensure Vision model is downloaded in Settings for Local mode."))
    //     }
    // }

    @kotlinx.serialization.Serializable
    private data class FlashcardJson(val question: String, val answer: String)
    @kotlinx.serialization.Serializable
    private data class SpendingInsightJson(val insight: String, val suggestions: List<String>)

    private fun parseSpendingInsightJson(text: String): String {
        return try {
            val json = com.creativeblisslabs.omnimind.core.util.JsonUtils.extractJsonObject(text)
            val parsed = com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.decodeFromString<SpendingInsightJson>(json)
            "${parsed.insight}\n\n" + parsed.suggestions.joinToString("\n") { "- $it" }
        } catch (e: Exception) {
            text
        }
    }

    private fun parseStringArrayJson(text: String): List<String> {
        return try {
            val json = com.creativeblisslabs.omnimind.core.util.JsonUtils.extractJsonArray(text)
            com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.decodeFromString<List<String>>(json)
        } catch (e: Exception) {
            text.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("-").trim() }
        }
    }

    private fun parseFlashcardsJson(text: String): List<Pair<String, String>> {
        return try {
            val json = com.creativeblisslabs.omnimind.core.util.JsonUtils.extractJsonArray(text)
            com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.decodeFromString<List<FlashcardJson>>(json)
                .map { it.question to it.answer }
        } catch (e: Exception) {
            parseFlashcardsFallback(text)
        }
    }

    private fun parseFlashcardsFallback(text: String): List<Pair<String, String>> {
        val lines = text.lines()
        val cards = mutableListOf<Pair<String, String>>()
        var i = 0
        while (i < lines.size - 1) {
            val qLine = lines[i].trim()
            val aLine = lines.getOrNull(i + 1)?.trim().orEmpty()
            if (qLine.startsWith("Q:") && aLine.startsWith("A:")) {
                cards.add(qLine.removePrefix("Q:").trim() to aLine.removePrefix("A:").trim())
                i += 2
            } else {
                i++
            }
        }
        return cards
    }

    private fun parseIntegerArrayJson(text: String): List<Int> {
        return try {
            val json = com.creativeblisslabs.omnimind.core.util.JsonUtils.extractJsonArray(text)
            com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.decodeFromString<List<Int>>(json)
        } catch (e: Exception) {
            text.split(",").mapNotNull(String::trim).mapNotNull(String::toIntOrNull)
        }
    }
}

private suspend fun AppPreferences.aiProviderValue(): AiProvider =
    when (aiProvider.first()) {
        "cloud_only" -> AiProvider.CLOUD_ONLY
        "local_first" -> AiProvider.LOCAL_FIRST
        "local" -> AiProvider.LOCAL
        "cloud" -> AiProvider.CLOUD
        else -> AiProvider.AUTO
    }

private suspend fun AppPreferences.geminiApiKeyValue(): String = geminiApiKey.first()
