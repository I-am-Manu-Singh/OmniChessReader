package com.creativeblisslabs.omnimind.core.util

import kotlin.math.max
import kotlin.math.min

object AudioConverter {

    /**
     * Converts raw PCM FloatArray to WAV bytes.
     * Essential for cloud AI services that don't accept raw PCM.
     */
    fun pcmToWav(pcmData: FloatArray, sampleRate: Int = 16000): ByteArray {
        val bitsPerSample = 16
        val channels = 1
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmData.size * 2
        val totalSize = 36 + dataSize

        val header = ByteArray(44)
        
        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeInt(header, 4, totalSize)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Subchunk1Size
        writeShort(header, 20, 1.toShort()) // AudioFormat (PCM)
        writeShort(header, 22, channels.toShort())
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, blockAlign.toShort())
        writeShort(header, 34, bitsPerSample.toShort())

        // data chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, dataSize)

        val wavData = ByteArray(44 + dataSize)
        for (i in 0 until 44) {
            wavData[i] = header[i]
        }

        var offset = 44
        for (f in pcmData) {
            val sample = (max(-1.0f, min(1.0f, f)) * 32767).toInt().toShort()
            wavData[offset++] = (sample.toInt() and 0xFF).toByte()
            wavData[offset++] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        return wavData
    }

    private fun writeInt(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShort(data: ByteArray, offset: Int, value: Short) {
        data[offset] = (value.toInt() and 0xFF).toByte()
        data[offset + 1] = ((value.toInt() shr 8) and 0xFF).toByte()
    }
}
