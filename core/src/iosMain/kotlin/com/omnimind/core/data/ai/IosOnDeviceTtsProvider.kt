package com.creativeblisslabs.omnimind.core.data.ai

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CompletableDeferred
import platform.AVFAudio.AVAudioFile
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

class IosOnDeviceTtsProvider : OnDeviceTtsProvider {
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun generateAudio(text: String): Result<ByteArray> {
        val deferred = CompletableDeferred<Result<ByteArray>>()
        
        try {
            val synthesizer = AVSpeechSynthesizer()
            val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
            
            val tempDir = NSTemporaryDirectory()
            val fileName = "tts_${NSFileManager.defaultManager.hash}.wav"
            val fileURL = NSURL.fileURLWithPath(tempDir).URLByAppendingPathComponent(fileName)!!
            
            var audioFile: AVAudioFile? = null
            
            synthesizer.writeUtterance(utterance) { buffer ->
                val pcmBuffer = buffer as? AVAudioPCMBuffer
                if (pcmBuffer != null) {
                    if (audioFile == null) {
                        audioFile = AVAudioFile(forWriting = fileURL, settings = pcmBuffer.format.settings, error = null)
                    }
                    audioFile?.writeFromBuffer(pcmBuffer, error = null)
                } else {
                    // Synthesis finished
                    audioFile = null // Close the file
                    val data = NSData.dataWithContentsOfURL(fileURL)
                    if (data != null) {
                        val bytes = ByteArray(data.length.toInt())
                        bytes.usePinned { pinned ->
                            memcpy(pinned.addressOf(0), data.bytes, data.length)
                        }
                        NSFileManager.defaultManager.removeItemAtURL(fileURL, null)
                        deferred.complete(Result.success(bytes))
                    } else {
                        deferred.complete(Result.failure(Exception("Failed to read generated audio file")))
                    }
                }
            }
        } catch (e: Exception) {
            deferred.complete(Result.failure(e))
        }

        return deferred.await()
    }
}

actual fun createOnDeviceTtsProvider(): OnDeviceTtsProvider = IosOnDeviceTtsProvider()
