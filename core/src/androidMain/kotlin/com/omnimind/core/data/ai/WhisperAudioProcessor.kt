package com.creativeblisslabs.omnimind.core.data.ai

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.sin

/**
 * Mel Spectrogram pre-processor for Whisper TFLite models.
 * Implements FFT, Hanning Window, and Log-Mel filterbank conversion.
 */
class WhisperAudioProcessor(
    private val sampleRate: Int = 16000,
    private val nFft: Int = 400,
    private val hopLength: Int = 160,
    private val nMels: Int = 80
) {
    private val window = FloatArray(nFft) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (nFft - 1)))).toFloat()
    }

    private val melFilters: Array<FloatArray> by lazy {
        createMelFilters(sampleRate, nFft, nMels)
    }

    fun extractFeatures(pcm: FloatArray): Array<FloatArray> {
        // 1. Padding (Pad with zeros or reflect - Whisper usually pads to 30s)
        // For simplicity, we process the available audio and pad if needed.
        val frames = (pcm.size - nFft) / hopLength + 1
        if (frames <= 0) return Array(nMels) { FloatArray(0) }

        val spectrogram = Array(frames) { frameIndex ->
            val start = frameIndex * hopLength
            val frame = FloatArray(nFft)
            for (i in 0 until nFft) {
                if (start + i < pcm.size) {
                    frame[i] = pcm[start + i] * window[i]
                }
            }
            
            // 2. Simple DFT/FFT (Real to Magnitude)
            computeMagnitude(frame)
        }

        // 3. Mel Filterbank
        val melSpectrogram = Array(nMels) { m ->
            FloatArray(frames) { f ->
                var sum = 0f
                for (i in 0 until (nFft / 2 + 1)) {
                    sum += spectrogram[f][i] * melFilters[m][i]
                }
                
                // 4. Log scale
                val logMel = log10(maxOf(sum, 1e-10f))
                logMel
            }
        }
        
        // 5. Normalization (Specific to Whisper Tiny)
        val maxVal = melSpectrogram.flatten().maxOrNull() ?: 0f
        for (m in 0 until nMels) {
            for (f in 0 until frames) {
                melSpectrogram[m][f] = maxOf(melSpectrogram[m][f], maxVal - 8.0f)
                melSpectrogram[m][f] = (melSpectrogram[m][f] + 4.0f) / 4.0f
            }
        }

        return melSpectrogram
    }

    private fun computeMagnitude(frame: FloatArray): FloatArray {
        val n = frame.size
        val half = n / 2 + 1
        val magnitude = FloatArray(half)
        
        // Very basic DFT implementation (Replace with efficient FFT if performance is an issue)
        for (k in 0 until half) {
            var real = 0.0
            var imag = 0.0
            for (t in 0 until n) {
                val angle = 2.0 * PI * k * t / n
                real += frame[t] * cos(angle)
                imag -= frame[t] * sin(angle)
            }
            magnitude[k] = sqrt(real * real + imag * imag).toFloat()
        }
        return magnitude
    }

    private fun createMelFilters(sampleRate: Int, nFft: Int, nMels: Int): Array<FloatArray> {
        val minFreq = 0f
        val maxFreq = sampleRate / 2f
        val minMel = freqToMel(minFreq)
        val maxMel = freqToMel(maxFreq)

        val melPoints = FloatArray(nMels + 2) { i ->
            melToFreq(minMel + i * (maxMel - minMel) / (nMels + 1))
        }

        val bins = IntArray(nMels + 2) { i ->
            (melPoints[i] * (nFft + 1) / sampleRate).toInt()
        }

        val filters = Array(nMels) { m ->
            FloatArray(nFft / 2 + 1) { i ->
                when {
                    i < bins[m] -> 0f
                    i < bins[m + 1] -> (i - bins[m]).toFloat() / (bins[m + 1] - bins[m])
                    i < bins[m + 2] -> (bins[m + 2] - i).toFloat() / (bins[m + 2] - bins[m + 1])
                    else -> 0f
                }
            }
        }
        return filters
    }

    private fun freqToMel(freq: Float) = 2595f * log10(1f + freq / 700f)
    private fun melToFreq(mel: Float) = 700f * (10f.pow(mel / 2595f) - 1f)
    
    private fun Array<FloatArray>.flatten(): FloatArray {
        var size = 0
        for (a in this) size += a.size
        val res = FloatArray(size)
        var pos = 0
        for (a in this) {
            a.copyInto(res, pos)
            pos += a.size
        }
        return res
    }
}
