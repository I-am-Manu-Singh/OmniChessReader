package com.creativeblisslabs.omnimind.core.media

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AndroidSpeechSupport(private val context: Context) {
    private var tts: TextToSpeech? = null

    fun speak(text: String, language: String? = null, speed: Float? = null, pitch: Float? = null) {
        if (text.isBlank()) return
        val engine = tts ?: TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureEngine(tts, language, speed, pitch)
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "omnimind_tts")
            }
        }.also { tts = it }
        
        if (engine.isSpeaking) engine.stop()
        configureEngine(engine, language, speed, pitch)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "omnimind_tts")
    }

    private fun configureEngine(engine: TextToSpeech?, language: String?, speed: Float?, pitch: Float?) {
        engine?.apply {
            language?.let { 
                val locale = Locale.forLanguageTag(it)
                if (isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                    setLanguage(locale)
                }
            } ?: setLanguage(Locale.getDefault())
            
            speed?.let { setSpeechRate(it) } ?: setSpeechRate(1.0f)
            pitch?.let { setPitch(it) } ?: setPitch(1.0f)
        }
    }

    fun startVoiceInput(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onError(error: Int) { recognizer.destroy() }
            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotBlank()) onResult(text)
                recognizer.destroy()
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) = Unit
            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
