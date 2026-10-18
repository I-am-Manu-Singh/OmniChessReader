package com.creativeblisslabs.omnimind.core.media

import kotlinx.coroutines.flow.Flow
import org.koin.core.scope.Scope

interface MediaProcessor {
    suspend fun readAsBase64(path: String): String
    fun getMimeType(path: String): String
    suspend fun readAsText(path: String, password: String? = null): String
    fun readAsTextFlow(path: String, password: String? = null): Flow<String>
    
    // Efficient media handling for AI models
    suspend fun readAsPcm(path: String): FloatArray // For audio
    fun decodeBase64ToPcm(base64: String): FloatArray
    // suspend fun readAsImage(path: String): Any // Returns platform-specific Image/MPImage
    fun encodeBase64(bytes: ByteArray): String
    fun saveGeneratedBytes(bytes: ByteArray, fileName: String): String
    suspend fun extractBookCover(path: String, bookId: String): String? = null
}

expect fun Scope.createMediaProcessor(): MediaProcessor
