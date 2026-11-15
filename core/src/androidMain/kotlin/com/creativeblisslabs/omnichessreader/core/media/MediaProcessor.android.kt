package com.creativeblisslabs.omnichessreader.core.media

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

    private var exercises1000Map: Map<String, String>? = null

    private fun getExercise1000Solution(exNum: String): String? {
        if (exercises1000Map == null) {
            try {
                val jsonStr = context.assets.open("chess/1000_exercises_solutions.json").bufferedReader().use { it.readText() }
                val parsed = mutableMapOf<String, String>()
                val matcher = java.util.regex.Pattern.compile("\"(\\d+)\"\\s*:\\s*\"([^\"]+)\"").matcher(jsonStr)
                while (matcher.find()) {
                    parsed[matcher.group(1)] = matcher.group(2)
                }
                exercises1000Map = parsed
            } catch (e: Exception) {
                exercises1000Map = emptyMap()
            }
        }
        return exercises1000Map?.get(exNum)
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

    override suspend fun renderPdfPageAsImage(path: String, pageNumber: Int, bookId: String): String? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext null
        if (!file.name.lowercase().endsWith(".pdf")) return@withContext null

        val pagesDir = File(context.cacheDir, "pdf_pages/$bookId").apply { mkdirs() }
        val targetImageFile = File(pagesDir, "page_${pageNumber}.png")
        if (targetImageFile.exists() && targetImageFile.length() > 0) {
            return@withContext targetImageFile.absolutePath
        }

        runCatching {
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val zeroIndexPage = (pageNumber - 1).coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
            if (renderer.pageCount > 0 && zeroIndexPage < renderer.pageCount) {
                val page = renderer.openPage(zeroIndexPage)
                // High quality render for reading crisp text and diagrams (scale 2.0x, clamped to 1600x2400)
                val width = (page.width * 2f).toInt().coerceAtMost(1600)
                val height = (page.height * 2f).toInt().coerceAtMost(2400)
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                java.io.FileOutputStream(targetImageFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                }
                bitmap.recycle()
                targetImageFile.absolutePath
            } else {
                renderer.close()
                pfd.close()
                null
            }
        }.getOrNull()
    }

    override suspend fun extractPdfPageText(path: String, pageNumber: Int): String = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.name.lowercase().endsWith(".pdf")) return@withContext ""
        try {
            PDFBoxResourceLoader.init(context)
            // Use MemoryUsageSetting to limit heap footprint and prevent OOM on large books
            val memSetting = com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMixed(50 * 1024 * 1024)
            PDDocument.load(file, memSetting).use { doc ->
                if (pageNumber in 1..doc.numberOfPages) {
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    stripper.getText(doc).trim()
                } else ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun getPdfPageCount(path: String): Int = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.name.lowercase().endsWith(".pdf")) return@withContext 0
        runCatching {
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        }.getOrDefault(0)
    }

    override suspend fun extractTableOfContents(path: String): List<TocItem> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext emptyList()
        val lower = file.name.lowercase()

        when {
            lower.endsWith(".pdf") -> {
                runCatching {
                    PDDocument.load(file).use { document ->
                        val results = mutableListOf<TocItem>()
                        val outline = document.documentCatalog?.documentOutline ?: return@use emptyList()

                        fun traverseOutline(item: com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem?, level: Int) {
                            var curr = item
                            while (curr != null) {
                                val title = curr.title?.trim()
                                if (!title.isNullOrBlank()) {
                                    val destPage = runCatching {
                                        val page = curr.findDestinationPage(document)
                                        if (page != null) document.pages.indexOf(page) + 1 else -1
                                    }.getOrDefault(-1)
                                    val pageNum = if (destPage > 0) destPage else 1
                                    results.add(TocItem(title = title, page = pageNum, level = level))
                                }
                                if (curr.firstChild != null) {
                                    traverseOutline(curr.firstChild, level + 1)
                                }
                                curr = curr.nextSibling
                            }
                        }

                        traverseOutline(outline.firstChild, 0)
                        results
                    }
                }.getOrDefault(emptyList())
            }
            lower.endsWith(".epub") -> {
                runCatching {
                    val results = mutableListOf<TocItem>()
                    var ncxBytes: ByteArray? = null
                    var navBytes: ByteArray? = null

                    ZipInputStream(FileInputStream(file)).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name.lowercase()
                            if (!entry.isDirectory) {
                                if (name.endsWith("toc.ncx") || name.endsWith(".ncx")) {
                                    ncxBytes = zip.readBytes()
                                } else if (name.endsWith("nav.xhtml") || name.endsWith("nav.html")) {
                                    navBytes = zip.readBytes()
                                }
                            }
                            entry = zip.nextEntry
                        }
                    }

                    // 1. Try EPUB 2 NCX
                    if (ncxBytes != null) {
                        val ncxContent = ncxBytes!!.toString(Charsets.UTF_8)
                        val navPointRegex = Regex("<navPoint[\\s\\S]*?<text>(.*?)</text>[\\s\\S]*?<content\\s+src=[\"'](.*?)[\"']", RegexOption.IGNORE_CASE)
                        var pageCounter = 1
                        navPointRegex.findAll(ncxContent).forEach { match ->
                            val text = match.groupValues[1].replace(Regex("<[^>]+>"), "").trim()
                            val src = match.groupValues[2].trim()
                            if (text.isNotBlank()) {
                                results.add(TocItem(title = text, page = pageCounter++, level = 0, href = src))
                            }
                        }
                    }

                    // 2. Fallback to EPUB 3 Nav Document
                    if (results.isEmpty() && navBytes != null) {
                        val navContent = navBytes!!.toString(Charsets.UTF_8)
                        val navLinkRegex = Regex("<a\\s+[^>]*href=[\"'](.*?)[\"'][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE)
                        var pageCounter = 1
                        navLinkRegex.findAll(navContent).forEach { match ->
                            val href = match.groupValues[1].trim()
                            val text = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                            if (text.isNotBlank() && !text.equals("Table of Contents", ignoreCase = true)) {
                                results.add(TocItem(title = text, page = pageCounter++, level = 0, href = href))
                            }
                        }
                    }
                    results
                }.getOrDefault(emptyList())
            }
            else -> emptyList()
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

    override suspend fun extractPdfDiagrams(path: String): Map<Int, List<PdfDiagramPosition>> = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.name.lowercase().endsWith(".pdf")) return@withContext emptyMap()
        val result = mutableMapOf<Int, MutableList<PdfDiagramPosition>>()
        try {
            val data = file.readBytes()
            val text = String(data, Charsets.ISO_8859_1)

            val pageObjPattern = java.util.regex.Pattern.compile("(\\d+)\\s+0\\s+obj([\\s\\S]*?)endobj")
            val objMap = mutableMapOf<String, String>()
            val objMatcher = pageObjPattern.matcher(text)
            while (objMatcher.find()) {
                objMap[objMatcher.group(1)] = objMatcher.group(2)
            }

            var rootPagesId: String? = null
            for ((oid, body) in objMap) {
                if (body.contains("/Type/Catalog") || body.contains("/Type /Catalog")) {
                    val pm = java.util.regex.Pattern.compile("/Pages\\s+(\\d+)\\s+0\\s+R").matcher(body)
                    if (pm.find()) {
                        rootPagesId = pm.group(1)
                        break
                    }
                }
            }

            val kids = mutableListOf<String>()
            fun resolvePages(oid: String) {
                val body = objMap[oid] ?: return
                val typeM = java.util.regex.Pattern.compile("/Type\\s*/(\\w+)").matcher(body)
                val objType = if (typeM.find()) typeM.group(1) else ""
                if (objType == "Page") {
                    kids.add(oid)
                } else if (objType == "Pages") {
                    val km = java.util.regex.Pattern.compile("/Kids\\s*\\[([\\s\\S]*?)\\]").matcher(body)
                    if (km.find()) {
                        val refM = java.util.regex.Pattern.compile("(\\d+)\\s+0\\s+R").matcher(km.group(1))
                        while (refM.find()) {
                            resolvePages(refM.group(1))
                        }
                    }
                }
            }

            if (rootPagesId != null) {
                resolvePages(rootPagesId)
            }

            if (kids.isEmpty()) {
                for ((oid, body) in objMap) {
                    if (body.contains("/Type/Page") || body.contains("/Type /Page")) {
                        kids.add(oid)
                    }
                }
            }
            if (kids.isEmpty()) return@withContext emptyMap()

            val bangPattern = java.util.regex.Pattern.compile("\\[\\s*\\(!\\)[\\s\\S]*?\\(#\\)\\s*\\]\\s*TJ")

            fun glyphToPiece(c: Char): Char? = when (c.code) {
                1, 2, 3, 'w'.code, '\t'.code -> null
                '*'.code, 'D'.code, 6, 'd'.code, '_'.code, 42 -> null
                14, '\r'.code, 'P'.code -> 'P'
                'G'.code, 23, 'R'.code, 70 -> 'R'
                'I'.code, 16, 'N'.code, 71 -> 'N'
                'C'.code, 'c'.code, 32 -> 'B' // Uppercase White Bishop (ASCII 32 in some encodings)
                10, 'Q'.code -> 'Q'
                43, '@'.code, 'q'.code -> 'q' // 43 is '+' which is Black Queen in standard diagram fonts!
                '>'.code, 'K'.code, 21, 62 -> 'K'
                '<'.code, 15, 'p'.code, 60 -> 'p'
                'H'.code, ';'.code, 'r'.code, 72, 59 -> 'r'
                '='.code, 'B'.code, 'n'.code, 61 -> 'n'
                'E'.code, 5, 'b'.code, 'g'.code, 69 -> 'b'
                '\''.code, 'k'.code, 7, 39 -> 'k'
                else -> null
            }

            fun extractPdfStrings(streamText: String): List<String> {
                val res = mutableListOf<String>()
                var pos = 0
                val len = streamText.length
                while (pos < len) {
                    val idx = streamText.indexOf('(', pos)
                    if (idx == -1) break
                    var cur = idx + 1
                    val sb = StringBuilder()
                    while (cur < len) {
                        val ch = streamText[cur]
                        if (ch == '\\') {
                            val nextCh = if (cur + 1 < len) streamText[cur + 1] else ' '
                            if (nextCh == 'r') sb.append('\r')
                            else if (nextCh == 'n') sb.append('\n')
                            else if (nextCh == 't') sb.append('\t')
                            else sb.append(nextCh)
                            cur += 2
                        } else if (ch == ')') {
                            break
                        } else {
                            sb.append(ch)
                            cur++
                        }
                    }
                    res.add(sb.toString())
                    pos = cur + 1
                }
                return res
            }

            for (p in kids.indices) {
                val pageNum = p + 1
                val kid = kids[p]
                val pageObjBody = objMap[kid] ?: continue

                // Extract page dimensions (MediaBox/CropBox)
                var pW = 420f
                var pH = 595f
                val mboxMatcher = java.util.regex.Pattern.compile("/(?:MediaBox|CropBox)\\s*\\[\\s*([0-9.]+)\\s+([0-9.]+)\\s+([0-9.]+)\\s+([0-9.]+)\\s*\\]").matcher(pageObjBody)
                if (mboxMatcher.find()) {
                    val w = mboxMatcher.group(3).toFloatOrNull() ?: 420f
                    val h = mboxMatcher.group(4).toFloatOrNull() ?: 595f
                    if (w > 0f) pW = w
                    if (h > 0f) pH = h
                }

                val contentsMatcher = java.util.regex.Pattern.compile("/Contents\\s+(\\d+)\\s+0\\s+R").matcher(pageObjBody)
                if (!contentsMatcher.find()) continue
                val cid = contentsMatcher.group(1)
                val cidBody = objMap[cid] ?: continue

                val cidMatcher = java.util.regex.Pattern.compile("\\b$cid\\s+0\\s+obj\\s*<<[\\s\\S]*?>>\\s*stream").matcher(text)
                if (!cidMatcher.find()) continue
                var startStream = cidMatcher.end()
                if (startStream + 1 < data.size && data[startStream] == '\r'.code.toByte() && data[startStream + 1] == '\n'.code.toByte()) {
                    startStream += 2
                } else if (startStream < data.size && (data[startStream] == '\n'.code.toByte() || data[startStream] == '\r'.code.toByte())) {
                    startStream += 1
                }

                val endStream = text.indexOf("endstream", startStream)
                if (endStream == -1) continue

                // Check for /Length if specified in object header
                var lengthVal: Int? = null
                val lenMatcher = java.util.regex.Pattern.compile("/Length\\s+(\\d+)(?:\\s+0\\s+R)?").matcher(cidBody)
                if (lenMatcher.find()) {
                    val rawNum = lenMatcher.group(1)?.toIntOrNull()
                    if (rawNum != null) {
                        if (cidBody.contains("/Length $rawNum 0 R")) {
                            // Indirect length object
                            val indObjBody = objMap[rawNum.toString()]
                            val indVal = indObjBody?.trim()?.toIntOrNull()
                            if (indVal != null) lengthVal = indVal
                        } else {
                            lengthVal = rawNum
                        }
                    }
                }

                fun decompressStream(endPos: Int): String? {
                    val infl = java.util.zip.Inflater()
                    infl.setInput(data, startStream, (endPos - startStream).coerceAtLeast(0))
                    val buf = ByteArray(65536)
                    val bos = java.io.ByteArrayOutputStream()
                    return try {
                        while (!infl.finished()) {
                            val count = infl.inflate(buf)
                            if (count == 0 && infl.needsInput()) break
                            if (count > 0) bos.write(buf, 0, count)
                            else break
                        }
                        infl.end()
                        bos.toString("ISO-8859-1")
                    } catch (e: Exception) {
                        null
                    }
                }

                var dec: String? = null
                if (lengthVal != null && startStream + lengthVal <= data.size) {
                    dec = decompressStream(startStream + lengthVal)
                }
                if (dec == null) {
                    var endData = endStream
                    if (endData >= 2 && data[endData - 2] == '\r'.code.toByte() && data[endData - 1] == '\n'.code.toByte()) {
                        endData -= 2
                    } else if (endData >= 1 && (data[endData - 1] == '\n'.code.toByte() || data[endData - 1] == '\r'.code.toByte())) {
                        endData -= 1
                    }
                    dec = decompressStream(endData) ?: decompressStream(endStream)
                }
                if (dec == null) continue

                try {
                    // Extract text matrix positions (Tm, Td, TL, Tf) to determine exact diagram bounding boxes
                    val tokPattern = java.util.regex.Pattern.compile("(\\[[^\\]]*\\]TJ|\\([^)]*\\)\\s*Tj|[0-9.-]+\\s+[0-9.-]+\\s+[0-9.-]+\\s+[0-9.-]+\\s+[0-9.-]+\\s+[0-9.-]+\\s+Tm|[0-9.-]+\\s+[0-9.-]+\\s+Td|[0-9.-]+\\s+TL|T\\*|BT|ET|/R\\w+\\s+[0-9.]+\\s+Tf)")
                    val tokMatcher = tokPattern.matcher(dec)
                    var currX = 0f
                    var currY = 0f
                    var currTl = 14f
                    var currFontSize = 14f
                    var lineStartX = 0f
                    var lineStartY = 0f
                    val diagCoords = mutableMapOf<Int, FloatArray>() // matchStart -> [boxX0, boxY0, boxX1, boxY1]

                    while (tokMatcher.find()) {
                        val tok = tokMatcher.group(1) ?: ""
                        val tokStart = tokMatcher.start()
                        if (tok.endsWith("Tm")) {
                            val parts = tok.split("\\s+".toRegex())
                            if (parts.size >= 6) {
                                currX = parts[4].toFloatOrNull() ?: 0f
                                currY = parts[5].toFloatOrNull() ?: 0f
                                lineStartX = currX
                                lineStartY = currY
                            }
                        } else if (tok.endsWith("Td")) {
                            val parts = tok.split("\\s+".toRegex())
                            if (parts.size >= 3) {
                                currX = lineStartX + (parts[0].toFloatOrNull() ?: 0f)
                                currY = lineStartY + (parts[1].toFloatOrNull() ?: 0f)
                                lineStartX = currX
                                lineStartY = currY
                            }
                        } else if (tok.endsWith("TL")) {
                            currTl = tok.split("\\s+".toRegex()).firstOrNull()?.toFloatOrNull() ?: 14f
                        } else if (tok == "T*") {
                            currX = lineStartX
                            currY = lineStartY - currTl
                            lineStartY = currY
                        } else if (tok.endsWith("Tf")) {
                            val parts = tok.split("\\s+".toRegex())
                            if (parts.size >= 2) {
                                currFontSize = parts[1].toFloatOrNull() ?: 14f
                            }
                        } else if (tok.contains("(!)") && tok.contains("(#)")) {
                            val boardSize = 8.8f * currFontSize
                            val yTop = pH - (currY + currFontSize * 0.9f)
                            val yBottom = yTop + boardSize
                            val xLeft = currX
                            val xRight = xLeft + boardSize
                            diagCoords[tokStart] = floatArrayOf(xLeft, yTop, xRight, yBottom)
                        }
                    }

                    val bm = bangPattern.matcher(dec)
                    var diagIndex = 0
                    while (bm.find()) {
                        val start = bm.start()
                        val snippet = dec.substring(start, (start + 2500).coerceAtMost(dec.length))
                        val strs = extractPdfStrings(snippet)
                        val boardRanks = mutableListOf<String>()
                        val currRank = StringBuilder()
                        var inRank = false
                        for (s in strs) {
                            if (s.contains("123456789:") || s == ":") break
                            for (c_i in 0 until s.length) {
                                val ch = s[c_i]
                                if ((ch == '$' || ch == '&' || ch == '(' || ch == ')' || ch == ',' || ch == '-' || ch == '.' || ch == '/') && !inRank) {
                                    inRank = true
                                    currRank.setLength(0)
                                } else if (ch == '%' && inRank) {
                                    inRank = false
                                    boardRanks.add(currRank.toString())
                                    currRank.setLength(0)
                                } else if (inRank) {
                                    currRank.append(ch)
                                }
                            }
                        }

                        if (boardRanks.size == 8) {
                            val fenRanks = mutableListOf<String>()
                            for (r in boardRanks) {
                                val cleanRank = if (r.length > 8) {
                                    val filtered = r.filter { it.code != 1 && it.code != 2 }
                                    if (filtered.length == 8) filtered else r.take(8)
                                } else if (r.length < 8) {
                                    r.padEnd(8, 3.toChar())
                                } else {
                                    r
                                }

                                val rStr = StringBuilder()
                                var emptyCnt = 0
                                for (ci in 0 until cleanRank.length) {
                                    val piece = glyphToPiece(cleanRank[ci])
                                    if (piece == null) {
                                        emptyCnt++
                                    } else {
                                        if (emptyCnt > 0) {
                                            rStr.append(emptyCnt)
                                            emptyCnt = 0
                                        }
                                        rStr.append(piece)
                                    }
                                }
                                if (emptyCnt > 0) rStr.append(emptyCnt)
                                fenRanks.add(rStr.toString())
                            }

                            val fullFen = fenRanks.joinToString("/")
                            diagIndex++

                            // Extract nearby text snippet around the diagram font glyphs
                            val beforeSnippet = dec.substring((start - 300).coerceAtLeast(0), start)
                            val afterSnippet = dec.substring(start, (start + 2000).coerceAtMost(dec.length))
                            val nearbyStrings = (extractPdfStrings(beforeSnippet).takeLast(4) + extractPdfStrings(afterSnippet).take(8))
                                .filter { it.isNotBlank() && !it.contains("123456789:") && it != ":" }
                            val nearbyTextCombined = nearbyStrings.joinToString(" ").trim().take(300)

                            val sideToMove = if (nearbyTextCombined.contains("Black to move", ignoreCase = true) || nearbyTextCombined.contains("Black to play", ignoreCase = true)) "b" else "w"
                            val meta = mutableMapOf<String, String>()
                            if (nearbyTextCombined.isNotBlank()) meta["nearbyText"] = nearbyTextCombined

                            val exactCoords = diagCoords[start] ?: diagCoords.values.elementAtOrNull(diagIndex - 1)
                            val bX0 = exactCoords?.get(0) ?: 0f
                            val bY0 = exactCoords?.get(1) ?: 0f
                            val bX1 = exactCoords?.get(2) ?: 0f
                            val bY1 = exactCoords?.get(3) ?: 0f

                            result.getOrPut(pageNum) { mutableListOf() }.add(
                                PdfDiagramPosition(
                                    pageNumber = pageNum,
                                    placement = fullFen,
                                    label = "Diagram $diagIndex (Page $pageNum)",
                                    boxX0 = bX0,
                                    boxY0 = bY0,
                                    boxX1 = bX1,
                                    boxY1 = bY1,
                                    sideToMove = sideToMove,
                                    pageWidth = pW,
                                    pageHeight = pH,
                                    nearbyText = nearbyTextCombined.ifBlank { null },
                                    metadata = meta
                                )
                            )
                        }
                    }

                    // 2. Detection of InDesign sliced/rasterized diagram boards (e.g. 8234.pdf, New In Chess, etc.)
                    val cmDoPattern = java.util.regex.Pattern.compile("([0-9.]+)\\s+0\\s+0\\s+([0-9.]+)\\s+([0-9.]+)\\s+([0-9.]+)\\s+cm\\s*\\r?\\n\\s*/(\\w+)\\s+Do")
                    val cmMatcher = cmDoPattern.matcher(dec)
                    val boardSlices = mutableListOf<FloatArray>() // [x, y, w, h]
                    val squareDiagrams = mutableListOf<FloatArray>() // [x, y, w, h]

                    while (cmMatcher.find()) {
                        val wF = cmMatcher.group(1).toFloatOrNull() ?: 0f
                        val hF = cmMatcher.group(2).toFloatOrNull() ?: 0f
                        val xF = cmMatcher.group(3).toFloatOrNull() ?: 0f
                        val yF = cmMatcher.group(4).toFloatOrNull() ?: 0f

                        // Check for sliced diagrams (e.g. 5 horizontal strips)
                        if (wF in 125f..155f && hF in 15f..35f) {
                            boardSlices.add(floatArrayOf(xF, yF, wF, hF))
                        }
                        // Check for 1:1 square full diagram images (e.g. 1000-chess-exercises-mate-in-2, Informant, Everyman)
                        else if (wF in 120f..300f && hF in 120f..300f && kotlin.math.abs(wF - hF) < 15f) {
                            squareDiagrams.add(floatArrayOf(xF, yF, wF, hF))
                        }
                    }

                    if (boardSlices.isNotEmpty()) {
                        val byX = boardSlices.groupBy { kotlin.math.round(it[0] * 10f) / 10f }
                        for ((_, slices) in byX) {
                            if (slices.size >= 4) { // 5 horizontal slices stacked = 1 chess diagram board
                                val minX = slices.minOf { it[0] }
                                val maxX = slices.maxOf { it[0] + it[2] }
                                val minY = slices.minOf { it[1] }
                                val maxY = slices.maxOf { it[1] + it[3] }
                                diagIndex++
                                result.getOrPut(pageNum) { mutableListOf() }.add(
                                    PdfDiagramPosition(
                                        pageNumber = pageNum,
                                        placement = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                                        label = "Diagram $diagIndex (Page $pageNum)",
                                        sideToMove = "w",
                                        boxX0 = minX,
                                        boxY0 = minY,
                                        boxX1 = maxX,
                                        boxY1 = maxY,
                                        pageWidth = pW,
                                        pageHeight = pH
                                    )
                                )
                            }
                        }
                    }

                    // 3. Process 1:1 square diagram images and associate side-to-move / exercise labels
                    if (squareDiagrams.isNotEmpty()) {
                        // Extract text labels from stream if available (e.g. "7 - White to play" or "8 - Black to play")
                        val labelTmPattern = java.util.regex.Pattern.compile("([0-9.]+)\\s+([0-9.]+)\\s+Tm\\s*\\r?\\n\\s*\\[?\\((.*?)\\)\\]?TJ?", java.util.regex.Pattern.CASE_INSENSITIVE)
                        val labelMatcher = labelTmPattern.matcher(dec)
                        val textLabels = mutableListOf<Triple<Float, Float, String>>() // x, y, cleanedText
                        var hasAnyChessKeywordsInPage = false
                        while (labelMatcher.find()) {
                            val lx = labelMatcher.group(1).toFloatOrNull() ?: 0f
                            val ly = labelMatcher.group(2).toFloatOrNull() ?: 0f
                            val rawText = labelMatcher.group(3) ?: ""
                            val clean = rawText.replace(Regex("\\)\\s*[-0-9.]+\\s*\\("), "").trim()
                            if (clean.contains("to play", ignoreCase = true) || clean.contains("to move", ignoreCase = true) ||
                                clean.contains("White", ignoreCase = true) || clean.contains("Black", ignoreCase = true) ||
                                clean.contains("mate", ignoreCase = true) || clean.contains("diagram", ignoreCase = true) ||
                                clean.contains("exercise", ignoreCase = true) || clean.contains("solution", ignoreCase = true) ||
                                clean.contains("chess", ignoreCase = true)) {
                                hasAnyChessKeywordsInPage = true
                            }
                            if (clean.contains("to play", ignoreCase = true) || clean.contains("to move", ignoreCase = true) ||
                                clean.contains("White", ignoreCase = true) || clean.contains("Black", ignoreCase = true)) {
                                textLabels.add(Triple(lx, ly, clean))
                            }
                        }

                        // Filter out false positives on title/cover/intro pages:
                        // If there is only 1 isolated square image and the page text has zero chess-related keywords, skip it.
                        val validDiagrams = if (squareDiagrams.size == 1 && !hasAnyChessKeywordsInPage && textLabels.isEmpty()) {
                            emptyList()
                        } else {
                            squareDiagrams
                        }

                        // Sort diagrams top-to-bottom, left-to-right (descending Y, ascending X)
                        val sortedDiagrams = validDiagrams.sortedWith(
                            compareByDescending<FloatArray> { kotlin.math.round(it[1] / 30f) * 30f }
                                .thenBy { it[0] }
                        )

                        for (sq in sortedDiagrams) {
                            val xF = sq[0]
                            val yF = sq[1]
                            val wF = sq[2]
                            val hF = sq[3]
                            diagIndex++

                            // Find closest text label for this diagram (typically right above it: same X, Y slightly above yF + hF)
                            val matchedLabel = textLabels.minByOrNull { (lx, ly, _) ->
                                val dx = kotlin.math.abs(lx - xF)
                                val dy = if (ly >= yF) ly - (yF + hF) else 500f
                                dx * 2f + dy
                            }

                            val labelText = matchedLabel?.third
                            val sideToMove = if (labelText != null && labelText.contains("Black", ignoreCase = true)) "b" else "w"
                            val finalLabel = if (!labelText.isNullOrBlank()) {
                                "$labelText (Page $pageNum)"
                            } else {
                                "Exercise $diagIndex (Page $pageNum)"
                            }

                            val meta = mutableMapOf<String, String>()
                            if (!labelText.isNullOrBlank()) meta["caption"] = labelText
                            val exerciseNumMatch = Regex("([0-9]{1,4})\\s*[-.]").find(labelText ?: "")
                            if (exerciseNumMatch != null) {
                                val exNum = exerciseNumMatch.groupValues[1]
                                meta["exerciseNumber"] = exNum
                                val sol = getExercise1000Solution(exNum)
                                if (sol != null) {
                                    meta["solution"] = sol
                                    val moveTokens = Regex("(?:[KQRBN]?[a-h]?[1-8]?[xX\u00d7]?[a-h][1-8](?:=[QRBN])?|O-O(?:-O)?|0-0(?:-0)?)[+#]?").findAll(sol).map { it.value }.toList()
                                    if (moveTokens.isNotEmpty()) {
                                        meta["expectedMoves"] = moveTokens.joinToString(" ")
                                    }
                                }
                            }

                            result.getOrPut(pageNum) { mutableListOf() }.add(
                                PdfDiagramPosition(
                                    pageNumber = pageNum,
                                    placement = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                                    label = finalLabel,
                                    sideToMove = sideToMove,
                                    boxX0 = xF,
                                    boxY0 = yF,
                                    boxX1 = xF + wF,
                                    boxY1 = yF + hF,
                                    pageWidth = pW,
                                    pageHeight = pH,
                                    nearbyText = labelText,
                                    metadata = meta
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        result
    }
}

actual fun Scope.createMediaProcessor(): MediaProcessor = AndroidMediaProcessor(get())
