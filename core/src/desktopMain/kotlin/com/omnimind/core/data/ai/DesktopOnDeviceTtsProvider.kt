package com.creativeblisslabs.omnimind.core.data.ai

import java.io.File
import java.util.UUID

class DesktopOnDeviceTtsProvider : OnDeviceTtsProvider {
    override suspend fun generateAudio(text: String): Result<ByteArray> {
        return runCatching {
            val os = System.getProperty("os.name").lowercase()
            val tempFile = File.createTempFile("tts_", ".wav")
            
            val process = when {
                os.contains("mac") -> {
                    // Use macOS 'say' command to generate an AIFF and convert or just use it
                    ProcessBuilder("say", "-o", tempFile.absolutePath, "--data-format=LEI16@22050", text).start()
                }
                os.contains("win") -> {
                    // Use PowerShell for SAPI.SpVoice
                    val script = "Add-Type -AssemblyName System.Speech; \$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; \$synth.SetOutputToWaveFile('${tempFile.absolutePath}'); \$synth.Speak('${text.replace("'", "''")}'); \$synth.Dispose()"
                    ProcessBuilder("powershell", "-Command", script).start()
                }
                else -> {
                    // Fallback for Linux (espeak)
                    ProcessBuilder("espeak", "-w", tempFile.absolutePath, text).start()
                }
            }
            
            val exitCode = process.waitFor()
            if (exitCode == 0 && tempFile.exists()) {
                val bytes = tempFile.readBytes()
                tempFile.delete()
                bytes
            } else {
                throw Exception("Desktop TTS process failed with exit code $exitCode")
            }
        }
    }
}

actual fun createOnDeviceTtsProvider(): OnDeviceTtsProvider = DesktopOnDeviceTtsProvider()
