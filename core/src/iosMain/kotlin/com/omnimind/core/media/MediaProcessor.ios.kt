package com.creativeblisslabs.omnimind.core.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.koin.core.scope.Scope
import platform.Foundation.*
import platform.posix.memcpy
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.AVFoundation.*
import platform.CoreMedia.*

class IosMediaProcessor : MediaProcessor {
    override suspend fun readAsBase64(path: String): String {
        val data = NSData.dataWithContentsOfFile(path) ?: return ""
        return data.base64EncodedStringWithOptions(0u)
    }

    override fun getMimeType(path: String): String {
        val ext = path.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readAsText(path: String, password: String?): String {
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) ?: ""
    }

    override fun readAsTextFlow(path: String, password: String?): Flow<String> = flow {
        emit(readAsText(path, password))
    }

    override suspend fun readAsPcm(path: String): FloatArray {
        // Basic implementation for audio processing
        return FloatArray(0) 
    }

    override fun decodeBase64ToPcm(base64: String): FloatArray {
        return FloatArray(0)
    }

    // override suspend fun readAsImage(path: String): Any {
        // return UIImage.imageWithContentsOfFile(path) ?: Any()
    // }

    override fun encodeBase64(bytes: ByteArray): String {
        val data = bytes.toNSData()
        return data.base64EncodedStringWithOptions(0u)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun saveGeneratedBytes(bytes: ByteArray, fileName: String): String {
        val data = bytes.toNSData()
        val paths = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        val documentsDirectory = paths.first() as NSURL
        val fileURL = documentsDirectory.URLByAppendingPathComponent(fileName)!!
        data.writeToURL(fileURL, true)
        return fileURL.path ?: ""
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}

actual fun Scope.createMediaProcessor(): MediaProcessor = IosMediaProcessor()
