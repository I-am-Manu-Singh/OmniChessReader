package com.creativeblisslabs.omnimind.core.data.ai

interface OnDeviceTtsProvider {
    suspend fun generateAudio(text: String): Result<ByteArray>
}

expect fun createOnDeviceTtsProvider(): OnDeviceTtsProvider
