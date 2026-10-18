package com.creativeblisslabs.omnimind.core.data.ai

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.context.GlobalContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class AndroidOnDeviceTtsProvider(private val context: Context) : OnDeviceTtsProvider {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private suspend fun ensureInitialized(): Boolean = suspendCancellableCoroutine { cont ->
        if (isInitialized) {
            cont.resume(true)
            return@suspendCancellableCoroutine
        }
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                isInitialized = true
                cont.resume(true)
            } else {
                cont.resume(false)
            }
        }
    }

    override suspend fun generateAudio(text: String): Result<ByteArray> {
        if (!ensureInitialized()) return Result.failure(Exception("TTS Initialization failed"))
        
        val ttsEngine = tts ?: return Result.failure(Exception("TTS engine is null"))
        val file = File(context.cacheDir, "tts_${UUID.randomUUID()}.wav")
        
        return suspendCancellableCoroutine { cont ->
            val utteranceId = UUID.randomUUID().toString()
            ttsEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(uid: String?) {
                    if (uid == utteranceId) {
                        val bytes = file.readBytes()
                        file.delete()
                        cont.resume(Result.success(bytes))
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(uid: String?) {
                    if (uid == utteranceId) {
                        cont.resume(Result.failure(Exception("TTS Synthesis failed")))
                    }
                }
            })
            
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            ttsEngine.synthesizeToFile(text, params, file, utteranceId)
        }
    }
}

actual fun createOnDeviceTtsProvider(): OnDeviceTtsProvider {
    val context: Context = GlobalContext.get().get()
    return AndroidOnDeviceTtsProvider(context)
}
