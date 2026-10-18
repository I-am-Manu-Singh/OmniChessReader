package com.creativeblisslabs.omnimind.core.data.ai

import kotlinx.coroutines.flow.Flow

enum class AiProvider {
    LOCAL,
    CLOUD,
    AUTO,
    CLOUD_ONLY,
    LOCAL_FIRST
}

enum class LocalModelType {
    LLM,
    CHESS_ENGINE,
    TRANSLATION_PACK
}
enum class LocalModelProfile(
    val id: String, // Added unique ID for model differentiation
    val label: String,
    val description: String,
    val type: LocalModelType,
    val recommendedForMobile: Boolean,
    val suggestedPackUrl: String,
    val defaultFileSize: String // Renamed to avoid property shadowing conflicts in ModelPackInfo
) {
    Gemma4(
        "gemma_4",
        "Gemma 4 (On-Device LLM)",
        "Google Gemma 4 E2B mobile LLM model bundle (~1.5GB - 2GB). Optimized for chat, text reasoning, and summarization.",
        LocalModelType.LLM,
        true,
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-web.task",
        ""
    ),
    
    Stockfish17(
        "stockfish_17",
        "Stockfish 17 Chess Engine",
        "Hint: World-class grandmaster level local chess AI engine binary.",
        LocalModelType.CHESS_ENGINE,
        true,
        "https://github.com/official-stockfish/Stockfish/releases/download/sf_17.1/stockfish-android-armv8.tar",
        ""
    ),
    TranslationPackEN(
        "mlkit_translate_en",
        "English Translation Pack",
        "Offline ML Kit pack for English translation.",
        LocalModelType.TRANSLATION_PACK,
        true,
        "mlkit://translate/en",
        "30 MB"
    ),
    TranslationPackES(
        "mlkit_translate_es",
        "Spanish Translation Pack",
        "Offline ML Kit pack for Spanish translation.",
        LocalModelType.TRANSLATION_PACK,
        true,
        "mlkit://translate/es",
        "30 MB"
    ),
    TranslationPackFR(
        "mlkit_translate_fr",
        "French Translation Pack",
        "Offline ML Kit pack for French translation.",
        LocalModelType.TRANSLATION_PACK,
        true,
        "mlkit://translate/fr",
        "30 MB"
    ),
    TranslationPackDE(
        "mlkit_translate_de",
        "German Translation Pack",
        "Offline ML Kit pack for German translation.",
        LocalModelType.TRANSLATION_PACK,
        true,
        "mlkit://translate/de",
        "30 MB"
    ),
    TranslationPackHI(
        "mlkit_translate_hi",
        "Hindi Translation Pack",
        "Offline ML Kit pack for Hindi translation.",
        LocalModelType.TRANSLATION_PACK,
        true,
        "mlkit://translate/hi",
        "30 MB"
    ),
    TranslationPackZH(
        "mlkit_translate_zh",
        "Chinese Translation Pack",
        "Offline ML Kit pack for Chinese translation.",
        LocalModelType.TRANSLATION_PACK,
        true,
        "mlkit://translate/zh",
        "30 MB"
    ),
}
data class ModelPackInfo(
    val profile: LocalModelProfile,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0,
    val isImporting: Boolean = false,
    val importProgress: Int = 0,
    val statusMessage: String = "",
    val filePath: String = "",
    val lastError: String = "",
    val fileSize: String = "" // Populated dynamically with actual on-disk file size (e.g. "41.2 MB")
) {
    // Unique ID shorthand targeting the underlying implementation layout
    val id: String get() = profile.id

    // Smart helper: Returns actual disk usage if downloaded; otherwise falls back to estimated baseline
    val displaySize: String
        get() = if (isDownloaded && fileSize.isNotBlank()) fileSize else profile.defaultFileSize
}
interface ModelPackManager {
    val packState: Flow<ModelPackInfo>
    val allPacks: Flow<List<ModelPackInfo>>
    suspend fun refresh()
    suspend fun download(profile: LocalModelProfile)
    suspend fun deletePack(profile: LocalModelProfile)
    suspend fun checkCompatibility(profile: LocalModelProfile): Result<Boolean>
    suspend fun importLocalPack(uri: String, profile: LocalModelProfile)
    suspend fun checkForUpdates(): Result<Boolean>
}
