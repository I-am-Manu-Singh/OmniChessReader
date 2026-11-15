package com.creativeblisslabs.omnichessreader.core.media

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
    suspend fun renderPdfPageAsImage(path: String, pageNumber: Int, bookId: String): String? = null
    suspend fun getPdfPageCount(path: String): Int = 0
    suspend fun extractTableOfContents(path: String): List<TocItem> = emptyList()
    suspend fun extractPdfDiagrams(path: String): Map<Int, List<PdfDiagramPosition>> = emptyMap()
    suspend fun extractPdfPageText(path: String, pageNumber: Int): String = ""
}

@kotlinx.serialization.Serializable
data class PdfDiagramPosition(
    val pageNumber: Int,
    val placement: String,
    val label: String,
    val sideToMove: String = "w",
    val boxX0: Float = 0f,
    val boxY0: Float = 0f,
    val boxX1: Float = 0f,
    val boxY1: Float = 0f,
    val pageWidth: Float = 420f,
    val pageHeight: Float = 595f,
    val nearbyText: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@kotlinx.serialization.Serializable
data class TocItem(
    val title: String,
    val page: Int,
    val level: Int = 0,
    val href: String = ""
)

expect fun Scope.createMediaProcessor(): MediaProcessor
