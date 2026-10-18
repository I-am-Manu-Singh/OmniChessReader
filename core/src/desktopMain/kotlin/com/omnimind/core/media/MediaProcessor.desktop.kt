package com.creativeblisslabs.omnimind.core.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.Base64
import java.nio.file.Files
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.koin.core.scope.Scope
import javax.imageio.ImageIO

class DesktopMediaProcessor : MediaProcessor {
    override suspend fun readAsBase64(path: String): String {
        val file = File(path)
        return Base64.getEncoder().encodeToString(file.readBytes())
    }

    override fun getMimeType(path: String): String {
        val file = File(path)
        return Files.probeContentType(file.toPath()) ?: "application/octet-stream"
    }

    override suspend fun readAsPcm(path: String): FloatArray {
        val bytes = File(path).readBytes()
        val floatArray = FloatArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().let { shortBuffer ->
            for (i in floatArray.indices) {
                floatArray[i] = shortBuffer.get(i) / 32768.0f
            }
        }
        return floatArray
    }

    // override suspend fun readAsImage(path: String): Any {
        // return ImageIO.read(File(path))
    // }

    override suspend fun readAsText(path: String, password: String?): String {
        return File(path).readText()
    }

    override fun readAsTextFlow(path: String, password: String?): Flow<String> = flow {
        emit(File(path).readText())
    }

    override fun encodeBase64(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    override fun decodeBase64ToPcm(base64: String): FloatArray {
        val bytes = Base64.getDecoder().decode(base64)
        val floatArray = FloatArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().let { shortBuffer ->
            for (i in floatArray.indices) {
                floatArray[i] = shortBuffer.get(i) / 32768.0f
            }
        }
        return floatArray
    }

    override fun saveGeneratedBytes(bytes: ByteArray, fileName: String): String {
        val out = File(System.getProperty("java.io.tmpdir"), fileName)
        out.writeBytes(bytes)
        return out.absolutePath
    }
}

actual fun Scope.createMediaProcessor(): MediaProcessor = DesktopMediaProcessor()

