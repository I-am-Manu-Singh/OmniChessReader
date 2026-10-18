package com.creativeblisslabs.omnimind.core.media

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Base64
import android.webkit.MimeTypeMap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.koin.core.scope.Scope
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

class AndroidMediaProcessor(private val context: Context) : MediaProcessor {
    init {
        try {
            // Correct initialization class
            PDFBoxResourceLoader.init(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun readAsBase64(path: String): String {
        val file = File(path)
        return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    }

    override fun getMimeType(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    override suspend fun readAsPcm(path: String): FloatArray = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(path)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex < 0) return@withContext FloatArray(0)

            extractor.selectTrack(trackIndex)
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: return@withContext FloatArray(0)

            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            val pcmData = mutableListOf<Float>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false
            var outputSampleRate = 16000

            while (!isEOS) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isEOS = true
                    } else {
                        codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }

                var outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    outputSampleRate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }

                while (outputBufferIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputBufferIndex)!!
                    val format = codec.getOutputFormat(outputBufferIndex)
                    val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    outputSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    
                    val shortBuffer = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    
                    if (channelCount > 1) {
                        // Downmix to mono by averaging channels
                        while (shortBuffer.remaining() >= channelCount) {
                            var sum = 0f
                            for (i in 0 until channelCount) {
                                sum += shortBuffer.get().toFloat() / 32768.0f
                            }
                            pcmData.add(sum / channelCount)
                        }
                    } else {
                        while (shortBuffer.hasRemaining()) {
                            pcmData.add(shortBuffer.get().toFloat() / 32768.0f)
                        }
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val rawPcm = pcmData.toFloatArray()
            if (outputSampleRate != 16000) {
                resample(rawPcm, outputSampleRate, 16000)
            } else {
                rawPcm
            }
        } catch (e: Exception) {
            e.printStackTrace()
            FloatArray(0)
        }
    }

    private fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate || input.isEmpty()) return input
        val ratio = fromRate.toDouble() / toRate.toDouble()
        val outputSize = (input.size / ratio).toInt()
        val output = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            val center = i * ratio
            val left = center.toInt()
            val right = minOf(left + 1, input.size - 1)
            val fraction = (center - left).toFloat()
            output[i] = input[left] * (1f - fraction) + input[right] * fraction
        }
        return output
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        return -1
    }

    override fun decodeBase64ToPcm(base64: String): FloatArray {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val floatArray = FloatArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().let { shortBuffer ->
            for (i in 0 until minOf(floatArray.size, shortBuffer.limit())) {
                floatArray[i] = shortBuffer.get(i) / 32768.0f
            }
        }
        return floatArray
    }

    // override suspend fun readAsImage(path: String): Any {
        // val bitmap = BitmapFactory.decodeFile(path)
        // return BitmapImageBuilder(bitmap).build()
    // }

    override suspend fun readAsText(path: String, password: String?): String {
        val file = File(path)
        val lower = file.name.lowercase()
        return when {
            lower.endsWith(".pdf") -> runCatching { extractPdfText(file, password) }.getOrElse { 
                if (it.message?.contains("encrypted", true) == true || it is java.io.IOException) {
                    "Cannot decrypt PDF: Password required or incorrect."
                } else {
                    "PDF import failed: ${it.message ?: "unknown error"}"
                }
            }
            lower.endsWith(".epub") -> runCatching { extractEpubText(file) }.getOrElse { "EPUB import failed: ${it.message ?: "unknown error"}" }
            lower.endsWith(".mobi") || lower.endsWith(".azw") || lower.endsWith(".azw3") ->
                runCatching { extractMobiText(file) }.getOrElse { "MOBI parsing failed: ${it.message ?: "unknown error"}" }
            lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt") || getMimeType(path).startsWith("text/") -> file.readText()
            else -> runCatching { file.readText() }.getOrElse { "Binary or unsupported format" }
        }
    }

    override fun readAsTextFlow(path: String, password: String?): Flow<String> = flow {
        val file = File(path)
        val lower = file.name.lowercase()
        when {
            lower.endsWith(".pdf") -> {
                PDDocument.load(file, password ?: "").use { document ->
                    if (document.isEncrypted && password == null) {
                        throw IllegalArgumentException("Cannot decrypt PDF: Password required.")
                    }
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    for (i in 1..document.numberOfPages) {
                        stripper.startPage = i
                        stripper.endPage = i
                        val pageText = stripper.getText(document).trim()
                        if (pageText.isNotBlank()) {
                            emit(pageText + "\n\n")
                        }
                    }
                }
            }
            lower.endsWith(".epub") -> {
                val fullEpubText = extractEpubText(file)
                if (fullEpubText.isNotBlank()) {
                    val sections = fullEpubText.split("\n\n---\n\n")
                    for (section in sections) {
                        if (section.isNotBlank()) {
                            emit(section + "\n\n---\n\n")
                        }
                    }
                } else {
                    emit(file.readText())
                }
            }
            lower.endsWith(".mobi") || lower.endsWith(".azw") || lower.endsWith(".azw3") -> {
                val fullMobiText = extractMobiText(file)
                if (fullMobiText.isNotBlank()) {
                    emit(fullMobiText)
                } else {
                    emit(file.readText())
                }
            }
            else -> emit(readAsText(path, password))
        }
    }

    private fun extractPdfText(file: File, password: String?): String {
        return try {
            PDDocument.load(file, password ?: "").use { document ->
                if (document.isEncrypted && password == null) {
                    return "Cannot decrypt PDF: Password required."
                }
                val outlineTitles = mutableMapOf<Int, String>()
                try {
                    val outline = document.documentCatalog?.documentOutline
                    var item = outline?.firstChild
                    while (item != null) {
                        val title = item.title?.trim()
                        if (!title.isNullOrBlank()) {
                            val destPage = runCatching {
                                val page = item.findDestinationPage(document)
                                if (page != null) document.pages.indexOf(page) + 1 else -1
                            }.getOrDefault(-1)
                            if (destPage > 0) {
                                outlineTitles[destPage] = title
                            }
                        }
                        item = item.nextSibling
                    }
                } catch (_: Exception) {}

                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                val sb = StringBuilder()
                for (page in 1..document.numberOfPages) {
                    stripper.startPage = page
                    stripper.endPage = page
                    val pageText = stripper.getText(document).trim()
                    if (pageText.isNotBlank()) {
                        val outlineHeader = outlineTitles[page]
                        if (!outlineHeader.isNullOrBlank()) {
                            sb.append("# ").append(outlineHeader).append("\n\n")
                        }
                        sb.append(pageText).append("\n\n---\n\n")
                    }
                }
                sb.toString().trim()
            }
        } catch (e: Exception) {
            if (e.message?.contains("password", ignoreCase = true) == true) "Cannot decrypt PDF: Password incorrect."
            else "Error extracting PDF: ${e.message}"
        }
    }

    private fun extractEpubText(file: File): String {
        val zipEntries = mutableMapOf<String, ByteArray>()
        var title = ""
        var creator = ""
        var opfPath = ""

        ZipInputStream(FileInputStream(file)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name
                    val bytes = zip.readBytes()
                    zipEntries[name] = bytes
                    if (name.lowercase().endsWith(".opf")) {
                        opfPath = name
                        val raw = bytes.toString(Charsets.UTF_8)
                        title = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1)?.trim() ?: ""
                        creator = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1)?.trim() ?: ""
                    }
                }
                entry = zip.nextEntry
            }
        }

        // Try to read spine from OPF for correct reading order
        val spineFileNames = mutableListOf<String>()
        if (opfPath.isNotBlank()) {
            val opfRaw = zipEntries[opfPath]?.toString(Charsets.UTF_8) ?: ""
            val manifestMap = mutableMapOf<String, String>() // id -> href
            val manifestRegex = Regex("<item\\s+[^>]*id=[\"']([^\"']+)[\"'][^>]*href=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            manifestRegex.findAll(opfRaw).forEach { match ->
                manifestMap[match.groupValues[1]] = match.groupValues[2]
            }
            // Reverse attribute order check
            val manifestRegexRev = Regex("<item\\s+[^>]*href=[\"']([^\"']+)[\"'][^>]*id=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            manifestRegexRev.findAll(opfRaw).forEach { match ->
                manifestMap[match.groupValues[2]] = match.groupValues[1]
            }

            val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
            val spineRegex = Regex("<itemref\\s+[^>]*idref=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
            spineRegex.findAll(opfRaw).forEach { match ->
                val idref = match.groupValues[1]
                val href = manifestMap[idref]
                if (href != null) {
                    val fullPath = (opfDir + href).replace("\\", "/")
                    // Match key in zipEntries
                    val actualKey = zipEntries.keys.find { it.equals(fullPath, ignoreCase = true) || it.endsWith(href, ignoreCase = true) }
                    if (actualKey != null && !spineFileNames.contains(actualKey)) {
                        spineFileNames.add(actualKey)
                    }
                }
            }
        }

        val orderedKeys = if (spineFileNames.isNotEmpty()) {
            spineFileNames
        } else {
            zipEntries.keys.filter { k ->
                val lower = k.lowercase()
                lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")
            }.sorted()
        }

        val sections = mutableListOf<String>()
        for (key in orderedKeys) {
            val raw = zipEntries[key]?.toString(Charsets.UTF_8) ?: continue
            val formatted = transformHtmlToMarkdown(raw)
            if (formatted.isNotBlank()) {
                sections += formatted
            }
        }

        val header = if (title.isNotBlank()) "# $title\n**Author:** ${if (creator.isNotBlank()) creator else "Unknown"}\n\n---\n\n" else ""
        return header + (if (sections.isNotEmpty()) sections.joinToString("\n\n---\n\n") else file.readText())
    }

    private fun extractMobiText(file: File): String {
        val bytes = file.readBytes()
        if (bytes.size < 78) return "Invalid MOBI binary header."

        val recordCount = ((bytes[76].toInt() and 0xFF) shl 8) or (bytes[77].toInt() and 0xFF)
        if (recordCount <= 0) return "No MOBI records found."

        val isUtf8 = bytes.sliceArray(0 until minOf(2000, bytes.size)).let {
            val str = String(it, java.nio.charset.StandardCharsets.US_ASCII)
            str.contains("UTF-8", ignoreCase = true)
        }

        val textBlocks = mutableListOf<String>()
        var offset = 78 + (recordCount * 8)
        for (i in 0 until recordCount) {
            val endOffset = minOf(offset + 4096, bytes.size)
            if (offset >= endOffset) break
            val decompressed = decompressPalmDoc(bytes.copyOfRange(offset, endOffset), isUtf8)
            if (decompressed.isNotBlank()) textBlocks += decompressed
            offset = endOffset
        }

        val fullText = textBlocks.joinToString("\n\n")
        return if (fullText.isNotBlank()) transformHtmlToMarkdown(fullText) else "MOBI text extracted."
    }

    private fun decompressPalmDoc(bytes: ByteArray, isUtf8: Boolean = false): String {
        val out = java.io.ByteArrayOutputStream()
        var index = 0
        while (index < bytes.size) {
            val byte = bytes[index].toInt() and 0xFF
            index++
            when {
                byte == 0 -> out.write(' '.code)
                byte in 1..8 -> {
                    for (count in 0 until byte) {
                        if (index < bytes.size) {
                            out.write(bytes[index].toInt() and 0xFF)
                            index++
                        }
                    }
                }
                byte <= 0x7F -> out.write(byte)
                byte >= 0xC0 -> {
                    out.write(' '.code)
                    out.write(byte xor 0x80)
                }
                else -> {
                    if (index < bytes.size) {
                        val nextByte = bytes[index].toInt() and 0xFF
                        index++
                        val distance = ((byte and 0x3F) shl 5) or (nextByte shr 3)
                        val length = (nextByte and 0x07) + 3
                        val currentSize = out.size()
                        val rawBuf = out.toByteArray()
                        val start = currentSize - distance
                        if (start >= 0) {
                            for (k in 0 until length) {
                                if (start + k < currentSize) {
                                    out.write(rawBuf[start + k].toInt() and 0xFF)
                                }
                            }
                        }
                    }
                }
            }
        }
        val charset = if (isUtf8) "UTF-8" else "Windows-1252"
        return try {
            out.toString(charset)
        } catch (e: Exception) {
            out.toString()
        }
    }

    override suspend fun extractBookCover(path: String, bookId: String): String? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        val lower = file.name.lowercase()
        val coversDir = File(context.cacheDir, "covers").apply { mkdirs() }
        val coverFile = File(coversDir, "${bookId}.png")
        if (coverFile.exists() && coverFile.length() > 0) return@withContext coverFile.absolutePath

        when {
            lower.endsWith(".pdf") -> {
                runCatching {
                    val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    if (renderer.pageCount > 0) {
                        val page = renderer.openPage(0)
                        val width = (page.width * 1.5f).toInt().coerceAtMost(800)
                        val height = (page.height * 1.5f).toInt().coerceAtMost(1200)
                        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()
                        pfd.close()

                        java.io.FileOutputStream(coverFile).use { out ->
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                        }
                        bitmap.recycle()
                        coverFile.absolutePath
                    } else {
                        renderer.close()
                        pfd.close()
                        null
                    }
                }.getOrNull()
            }
            lower.endsWith(".epub") -> {
                runCatching {
                    var coverBytes: ByteArray? = null
                    ZipInputStream(FileInputStream(file)).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name.lowercase()
                            if (!entry.isDirectory && (name.contains("cover") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
                                coverBytes = zip.readBytes()
                                if (name.contains("cover")) break
                            }
                            entry = zip.nextEntry
                        }
                    }
                    if (coverBytes != null && coverBytes!!.isNotEmpty()) {
                        coverFile.writeBytes(coverBytes!!)
                        coverFile.absolutePath
                    } else null
                }.getOrNull()
            }
            lower.endsWith(".mobi") || lower.endsWith(".azw") || lower.endsWith(".azw3") -> {
                runCatching {
                    val bytes = file.readBytes()
                    // Search for JPEG magic bytes FF D8 FF
                    var foundIdx = -1
                    for (i in 0 until bytes.size - 3) {
                        if ((bytes[i].toInt() and 0xFF) == 0xFF && (bytes[i + 1].toInt() and 0xFF) == 0xD8 && (bytes[i + 2].toInt() and 0xFF) == 0xFF) {
                            foundIdx = i
                            break
                        }
                    }
                    if (foundIdx >= 0) {
                        // Find JPEG end marker FF D9
                        var endIdx = bytes.size
                        for (i in foundIdx + 10 until minOf(foundIdx + 1000000, bytes.size - 1)) {
                            if ((bytes[i].toInt() and 0xFF) == 0xFF && (bytes[i + 1].toInt() and 0xFF) == 0xD9) {
                                endIdx = i + 2
                                break
                            }
                        }
                        val jpegBytes = bytes.copyOfRange(foundIdx, endIdx)
                        coverFile.writeBytes(jpegBytes)
                        coverFile.absolutePath
                    } else null
                }.getOrNull()
            }
            else -> null
        }
    }

    private fun transformHtmlToMarkdown(html: String): String {
        return html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<h1[^>]*>([\\s\\S]*?)</h1>", RegexOption.IGNORE_CASE)) { 
                val inner = it.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                if (inner.isNotBlank()) "# $inner\n\n" else ""
            }
            .replace(Regex("<h2[^>]*>([\\s\\S]*?)</h2>", RegexOption.IGNORE_CASE)) { 
                val inner = it.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                if (inner.isNotBlank()) "## $inner\n\n" else ""
            }
            .replace(Regex("<h3[^>]*>([\\s\\S]*?)</h3>", RegexOption.IGNORE_CASE)) { 
                val inner = it.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                if (inner.isNotBlank()) "### $inner\n\n" else ""
            }
            .replace(Regex("<img[^>]*src=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)) {
                val src = it.groupValues[1].trim()
                if (src.isNotBlank()) "\n\n![Image]($src)\n\n" else ""
            }
            .replace(Regex("<p[^>]*>([\\s\\S]*?)</p>", RegexOption.IGNORE_CASE)) { "${it.groupValues[1]}\n\n" }
            .replace(Regex("<div[^>]*>([\\s\\S]*?)</div>", RegexOption.IGNORE_CASE)) { "${it.groupValues[1]}\n\n" }
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<b>([\\s\\S]*?)</b>", RegexOption.IGNORE_CASE)) { "**${it.groupValues[1]}**" }
            .replace(Regex("<strong>([\\s\\S]*?)</strong>", RegexOption.IGNORE_CASE)) { "**${it.groupValues[1]}**" }
            .replace(Regex("<i>([\\s\\S]*?)</i>", RegexOption.IGNORE_CASE)) { "*${it.groupValues[1]}*" }
            .replace(Regex("<em>([\\s\\S]*?)</em>", RegexOption.IGNORE_CASE)) { "*${it.groupValues[1]}*" }
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    override fun encodeBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    override fun saveGeneratedBytes(bytes: ByteArray, fileName: String): String {
        val out = File(context.cacheDir, fileName)
        out.parentFile?.mkdirs()
        out.writeBytes(bytes)
        return out.absolutePath
    }
}

actual fun Scope.createMediaProcessor(): MediaProcessor = AndroidMediaProcessor(get())
