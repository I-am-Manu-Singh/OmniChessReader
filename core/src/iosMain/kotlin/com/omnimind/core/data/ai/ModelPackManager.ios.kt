package com.creativeblisslabs.omnimind.core.data.ai

import org.koin.core.context.GlobalContext

actual fun createModelPackManager(): ModelPackManager {
    val prefs = GlobalContext.get().get<com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences>()
    return DefaultModelPackManager(prefs)
}

actual fun createOnDeviceAiRuntime(): OnDeviceAiRuntime = object : OnDeviceAiRuntime {
    private val ttsProvider = createOnDeviceTtsProvider()

    override suspend fun isReady(type: LocalModelType): Boolean {
        return type == LocalModelType.AUDIO_TTS // System TTS is always "ready" on iOS
    }

    override suspend fun generate(prompt: String, systemPrompt: String, history: List<Pair<String, String>>) = Result.success("On-device LLM runtime is currently only enabled on Android. Please use Cloud mode for text on iOS.")

    override suspend fun translateText(text: String, targetLanguage: String, sourceLanguage: String?): Result<String> = Result.failure(Exception("On-device translation pending implementation on iOS"))

    override suspend fun shutdown() {}
}
