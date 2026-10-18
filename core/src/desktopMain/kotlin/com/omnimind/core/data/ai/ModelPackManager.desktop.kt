package com.creativeblisslabs.omnimind.core.data.ai

import org.koin.core.context.GlobalContext

actual fun createModelPackManager(): ModelPackManager {
    val prefs = GlobalContext.get().get<com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences>()
    return DefaultModelPackManager(prefs)
}

actual fun createOnDeviceAiRuntime(): OnDeviceAiRuntime = object : OnDeviceAiRuntime {
    private val ttsProvider = createOnDeviceTtsProvider()

    override suspend fun isReady(type: LocalModelType): Boolean {
        // Desktop TTS implementation is currently a stub
        return false 
    }

    override suspend fun generate(prompt: String, systemPrompt: String, history: List<Pair<String, String>>) = 
        Result.success("On-device LLM is not yet optimized for Desktop. Please use Cloud mode.")
    
    override suspend fun summarize(text: String) = Result.success(text.take(200))
    override suspend fun generateTags(content: String) = Result.success(emptyList<String>())
    override suspend fun generateSubtasks(taskTitle: String) = Result.success(emptyList<String>())
    override suspend fun analyzeImage(image: Any, prompt: String) = Result.success("Not supported on Desktop")
    override suspend fun processAudio(pcmData: FloatArray, prompt: String) = Result.success("Not supported on Desktop")
    
    override suspend fun transcribe(pcmData: FloatArray): Result<String> = Result.failure(Exception("Not implemented"))
    override suspend fun transcribeWithMetadata(pcmData: FloatArray): Result<TranscriptionResult> = Result.failure(Exception("Not implemented"))
    
    
    override suspend fun generateAudio(text: String): Result<ByteArray> {
        return ttsProvider.generateAudio(text)
    }

    override suspend fun generateVideo(prompt: String): Result<String> = Result.failure(Exception("Not supported"))

    override suspend fun translateText(text: String, targetLanguage: String, sourceLanguage: String?): Result<String> = Result.failure(Exception("On-device translation pending implementation on Desktop"))

    override suspend fun shutdown() {}
}
