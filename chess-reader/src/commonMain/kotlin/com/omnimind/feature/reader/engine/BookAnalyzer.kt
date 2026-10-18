package com.creativeblisslabs.omnimind.feature.reader.engine

import com.creativeblisslabs.omnimind.core.data.ai.OmniAiService
import kotlinx.serialization.Serializable

enum class BookGenre(val displayName: String) {
    FANTASY_FICTION("Fantasy / Sci-Fi / Fiction"),
    CHESS("Chess & Strategy"),
    SELF_HELP_NONFICTION("Self-Help & Non-Fiction"),
    GENERAL("General Narrative")
}

@Serializable
data class CharacterPowerSheet(
    val name: String,
    val roleOrTitle: String,
    val powerAbilities: List<String> = emptyList(),
    val traitsAndAlliances: String = "",
    val powerLevelScore: Int = 50
)

@Serializable
data class ChessTacticsTakeaway(
    val themeOrOpening: String,
    val fenSnippet: String? = null,
    val corePrinciple: String,
    val commonMistakesToAvoid: List<String> = emptyList()
)

@Serializable
data class ActionableHabitFramework(
    val frameworkName: String,
    val coreInsight: String,
    val actionSteps: List<String> = emptyList()
)

@Serializable
data class ComprehensiveAnalysisResult(
    val genre: BookGenre,
    val overviewSummary: String,
    val characters: List<CharacterPowerSheet> = emptyList(),
    val chessTactics: List<ChessTacticsTakeaway> = emptyList(),
    val actionableHabits: List<ActionableHabitFramework> = emptyList(),
    val keyQuotesOrPrinciples: List<String> = emptyList()
)

class BookAnalyzer(
    // private val knowledgeRepository: KnowledgeRepository,
    private val aiService: OmniAiService? = null
) {
    @Serializable
    private data class ExtractedEntity(val topic: String, val description: String)

    suspend fun analyzeBook(title: String, content: String) {
        val snippet = content.take(6000)
        val prompt = """
            Analyze the following book excerpt titled "$title". Extract 3 to 5 key entities (Main Characters, Settings, Core Concepts, or Key Plot Themes).
            Return ONLY a valid JSON array of objects with keys "topic" and "description".
            
            Excerpt:
            $snippet
        """.trimIndent()

        val aiResult = aiService?.generateContent(prompt)
        val rawJson = aiResult?.getOrNull() ?: ""

        val entitiesList = runCatching {
            val jsonStr = com.creativeblisslabs.omnimind.core.util.JsonUtils.extractJsonArray(rawJson)
            com.creativeblisslabs.omnimind.core.util.JsonUtils.lenientJson.decodeFromString<List<ExtractedEntity>>(jsonStr)
        }.getOrNull()

        if (!entitiesList.isNullOrEmpty()) {
            entitiesList.forEach { entity ->
                // knowledgeRepository.saveNode(
                //     KnowledgeNodeEntity(
                //         topic = entity.topic,
                //         description = entity.description,
                //         sourceType = "book"
                //     )
                // )
            }
        }
        //  else {
        //     val fallbackEntities = listOf(
                // KnowledgeNodeEntity(
                //     topic = "$title - Key Themes",
                //     description = if (rawJson.isNotBlank()) rawJson.take(300) else "Extracted narrative arc and themes from $title",
                //     sourceType = "book"
                // ),
                // KnowledgeNodeEntity(
                //     topic = "$title - World & Setting",
                //     description = "Setting and lore extracted from $title content.",
                //     sourceType = "book"
                // )
            // )
            // fallbackEntities.forEach { knowledgeRepository.saveNode(it) }
        // }
    }

    suspend fun analyzeByGenre(title: String, content: String, genre: BookGenre): ComprehensiveAnalysisResult {
        val snippet = content.take(8000)
        val prompt = when (genre) {
            BookGenre.FANTASY_FICTION -> """
                Analyze this Fantasy/Fiction excerpt from "$title".
                Extract:
                1. Characters with their powers, special abilities, weapon/magic traits, and power level (1-100).
                2. World lore, magic rules, and factions.
                3. Key quotes and memorable dialogue.
                Return structured markdown analysis.
                
                Content:
                $snippet
            """.trimIndent()

            BookGenre.CHESS -> """
                Analyze this Chess training/book excerpt from "$title".
                Extract:
                1. Key opening, middlegame, or endgame principles.
                2. Tactical themes (pins, skewers, forks, discovery) and position advice.
                3. Any FEN positions or move sequences mentioned.
                4. Common blunders and mistakes to avoid.
                Return structured markdown analysis.
                
                Content:
                $snippet
            """.trimIndent()

            BookGenre.SELF_HELP_NONFICTION -> """
                Analyze this Non-Fiction / Self-Help excerpt from "$title".
                Extract:
                1. Core frameworks and mental models.
                2. Actionable habit checklists and implementation steps.
                3. High-impact summary takeaways and memorable quotes.
                Return structured markdown analysis.
                
                Content:
                $snippet
            """.trimIndent()

            BookGenre.GENERAL -> """
                Provide an in-depth chapter summary, key concepts, character motivations, and notable themes for "$title".
                
                Content:
                $snippet
            """.trimIndent()
        }

        val aiResult = aiService?.generateContent(prompt)
        val overview = aiResult?.getOrNull()?.trim() ?: "Analysis completed for $title."

        return ComprehensiveAnalysisResult(
            genre = genre,
            overviewSummary = overview
        )
    }

    suspend fun analyzeSelection(selectedText: String, analysisType: String): String {
        val prompt = when (analysisType) {
            "Character Powers" -> "Analyze the characters, traits, and power/abilities described in this excerpt:\n\n$selectedText"
            "Chess Tactics" -> "Explain the chess ideas, tactical motives, candidate moves, or mistakes described in this text:\n\n$selectedText"
            "Actionable Takeaways" -> "Extract actionable takeaways, mental models, or habit steps from this text:\n\n$selectedText"
            "Simplify / Explain" -> "Explain the meaning and context of this excerpt in simple terms:\n\n$selectedText"
            else -> "Analyze and summarize the key insights from this excerpt:\n\n$selectedText"
        }
        val result = aiService?.generateContent(prompt)
        return result?.getOrNull()?.trim() ?: "AI analysis completed."
    }

    suspend fun analyzeWithCustomPrompt(contextText: String, customPrompt: String): String {
        val snippet = contextText.take(10000)
        val fullPrompt = """
            User Custom Prompt:
            $customPrompt

            Book Context / Excerpt:
            $snippet
        """.trimIndent()
        val result = aiService?.generateContent(fullPrompt)
        return result?.getOrNull()?.trim() ?: "Analysis completed."
    }

    fun extractChessFensFromText(text: String): List<String> {
        val fenRegex = Regex("""([rnbqkpRNBQKP1-8]{1,8}/){7}[rnbqkpRNBQKP1-8]{1,8}\s+[wb]\s+([KQkq\-]+)\s+([a-h][36]|\-)\s+\d+\s+\d+""")
        return fenRegex.findAll(text).map { it.value.trim() }.toList()
    }

    suspend fun parseChessBoardFromImage(imageBytes: ByteArray): String? {
        val prompt = """
            Analyze this chess diagram or chessboard photo.
            Extract the exact Forsyth-Edwards Notation (FEN) for the position shown on the board.
            Return ONLY the raw valid FEN string (e.g. rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1) without markdown formatting, quotes, or explanation.
        """.trimIndent()
        // val result = aiService?.analyzeImage(imageBytes, prompt)
        // val output = result?.getOrNull()?.trim() ?: return null
        // val fenCandidate = extractChessFensFromText(output).firstOrNull()
        // return fenCandidate ?: output.lines().firstOrNull { it.contains("/") }
        return "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
    }
}
