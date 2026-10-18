package com.creativeblisslabs.omnimind.core.data.ai

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.work.*
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class AndroidModelPackManager(
    private val context: Context,
    private val prefs: AppPreferences
) : ModelPackManager {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _allPacks = MutableStateFlow<List<ModelPackInfo>>(emptyList())
    override val allPacks: Flow<List<ModelPackInfo>> = _allPacks.asStateFlow()

    init {
        scope.launch {
            refresh()
        }
    }
    
    override val packState: Flow<ModelPackInfo> = combine(prefs.localModel, allPacks) { profileName, packs ->
        packs.find { it.profile.name == profileName } ?: run {
            val profile = LocalModelProfile.entries.find { it.name == profileName } ?: LocalModelProfile.Gemma4
            ModelPackInfo(
                profile = profile,
                fileSize = profile.defaultFileSize
            )
        }
    }

    private fun defaultExtensionFor(type: LocalModelType): String = when (type) {
        LocalModelType.LLM -> "task"
        LocalModelType.CHESS_ENGINE -> "bin"
        else -> "bin"
    }

    private fun formatFileSize(sizeBytes: Long): String {
        return if (sizeBytes > 1024 * 1024 * 1024) String.format(Locale.US, "%.2f GB", sizeBytes / (1024.0 * 1024 * 1024))
        else if (sizeBytes > 1024 * 1024) String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024))
        else if (sizeBytes > 1024) "${sizeBytes / 1024} KB"
        else "$sizeBytes B"
    }

    private fun isElfBinary(file: File): Boolean {
        if (!file.exists() || file.isDirectory || file.length() < 10 * 1024) return false
        return try {
            file.inputStream().buffered().use { stream ->
                val header = ByteArray(4)
                val count = stream.read(header)
                count == 4 && header[0] == 0x7F.toByte() && header[1] == 'E'.toByte() && header[2] == 'L'.toByte() && header[3] == 'F'.toByte()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun updatePackImportState(profile: LocalModelProfile, isImporting: Boolean, progress: Int, statusMsg: String) {
        val list = _allPacks.value.toMutableList()
        val idx = list.indexOfFirst { it.profile == profile }
        if (idx != -1) {
            list[idx] = list[idx].copy(
                isImporting = isImporting,
                importProgress = progress,
                statusMessage = statusMsg
            )
            _allPacks.value = list
        }
    }

    override suspend fun refresh() {
        checkAndExtractAssetStockfish()

        val modelsDir = context.filesDir.resolve("models").apply { mkdirs() }
        val remoteModelManager = RemoteModelManager.getInstance()
        
        val downloadedMlKitModels = try {
            remoteModelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
        } catch (e: Exception) {
            emptySet<TranslateRemoteModel>()
        }

        val packs = LocalModelProfile.entries.map { profile ->
      if (profile.type == LocalModelType.TRANSLATION_PACK) {
                val langCode = profile.suggestedPackUrl.removePrefix("mlkit://translate/")
                val isDl = downloadedMlKitModels.any { it.language == langCode }
                
                ModelPackInfo(
                    profile = profile,
                    isDownloaded = isDl,
                    downloadProgress = if (isDl) 100 else 0,
                    filePath = if (isDl) "mlkit://$langCode" else "",
                    lastError = "",
                    fileSize = profile.defaultFileSize
                )
            } else {
                var status = prefs.getModelStatus(profile.name).first()
                var path = prefs.getModelPath(profile.name).first()
                var error = prefs.getModelError(profile.name).first()
                
                val savedFile = if (path.isNotBlank()) File(path) else null
                val ext = defaultExtensionFor(profile.type)
                val defaultFile = File(modelsDir, "${profile.name}.$ext")
                
                val foundFile = when {
                    savedFile != null && savedFile.exists() && savedFile.length() > 0 -> savedFile
                    defaultFile.exists() && defaultFile.length() > 0 -> defaultFile
                    else -> modelsDir.listFiles()?.find { 
                        it.name.startsWith(profile.name) && !it.name.startsWith("temp_") && !it.name.startsWith("uncompressed_") && it.length() > 0 
                    }
                }

                if (foundFile != null && (profile.type != LocalModelType.CHESS_ENGINE || isElfBinary(foundFile))) {
                    status = "downloaded"
                    path = foundFile.absolutePath
                    error = ""
                    if (profile.type == LocalModelType.CHESS_ENGINE) {
                        foundFile.setExecutable(true, false)
                        try { Runtime.getRuntime().exec(arrayOf("chmod", "755", foundFile.absolutePath)).waitFor() } catch (_: Exception) {}
                    }
                    prefs.setModelPath(profile.name, foundFile.absolutePath)
                    prefs.setModelStatus(profile.name, "downloaded")
                    prefs.setModelError(profile.name, "")
                }
                
                val isDl = status == "downloaded" && path.isNotBlank() && File(path).exists()
                val sizeStr = if (isDl) formatFileSize(File(path).length()) else profile.defaultFileSize

                ModelPackInfo(
                    profile = profile,
                    isDownloaded = isDl,
                    downloadProgress = if (isDl) 100 else 0,
                    filePath = path,
                    lastError = if (isDl) "" else error,
                    fileSize = sizeStr
                )
            }
        }
        _allPacks.value = packs
        
        observeDownloads()
        cleanupOrphanedModels()
    }

    private suspend fun checkAndExtractAssetStockfish() {
        withContext(Dispatchers.IO) {
            try {
                val sfProfile = LocalModelProfile.Stockfish17
                val modelsDir = context.filesDir.resolve("models").apply { mkdirs() }
                val finalBin = File(modelsDir, "${sfProfile.name}.bin")

                if (finalBin.exists() && isElfBinary(finalBin)) {
                    finalBin.setExecutable(true, false)
                    prefs.setModelPath(sfProfile.name, finalBin.absolutePath)
                    prefs.setModelStatus(sfProfile.name, "downloaded")
                    prefs.setModelError(sfProfile.name, "")
                    return@withContext
                }

                val assetList = context.assets.list("chess") ?: emptyArray()
                val sfAsset = assetList.find { it.contains("stockfish", ignoreCase = true) }

                if (sfAsset != null) {
                    android.util.Log.i("OmniMind", "Found Stockfish binary asset: chess/$sfAsset. Extracting to app storage...")
                    updatePackImportState(sfProfile, true, 10, "Extracting Stockfish engine...")
                    val tempAssetFile = File(modelsDir, "temp_$sfAsset")

                    context.assets.open("chess/$sfAsset").use { input ->
                        tempAssetFile.outputStream().buffered(64 * 1024).use { output ->
                            input.copyTo(output)
                        }
                    }

                    updatePackImportState(sfProfile, true, 50, "Unpacking binary engine...")
                    var extractedElf: File? = null
                    val lowerName = sfAsset.lowercase()

                    if (lowerName.endsWith(".tar") || lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") || lowerName.endsWith(".zip")) {
                        val extractDir = File(modelsDir, "sf_asset_extracted").apply { mkdirs() }
                        try {
                            if (lowerName.endsWith(".zip")) {
                                ZipInputStream(tempAssetFile.inputStream().buffered()).use { zip ->
                                    var entry = zip.nextEntry
                                    while (entry != null) {
                                        if (!entry.isDirectory) {
                                            val outFile = File(extractDir, entry.name)
                                            outFile.parentFile?.mkdirs()
                                            outFile.outputStream().use { zip.copyTo(it) }
                                        }
                                        entry = zip.nextEntry
                                    }
                                }
                            } else {
                                val inputStream = if (lowerName.endsWith(".tar")) tempAssetFile.inputStream() else org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(tempAssetFile.inputStream().buffered())
                                org.apache.commons.compress.archivers.tar.TarArchiveInputStream(inputStream.buffered()).use { tar ->
                                    var entry = tar.nextTarEntry
                                    while (entry != null) {
                                        if (!entry.isDirectory) {
                                            val outFile = File(extractDir, entry.name)
                                            outFile.parentFile?.mkdirs()
                                            outFile.outputStream().use { tar.copyTo(it) }
                                        }
                                        entry = tar.nextTarEntry
                                    }
                                }
                            }
                            extractedElf = extractDir.walkTopDown().find { !it.isDirectory && isElfBinary(it) }
                        } catch (e: Exception) {
                            android.util.Log.e("OmniMind", "Asset archive extraction failed: ${e.localizedMessage}", e)
                        }
                    }

                    val sourceBin = extractedElf ?: if (isElfBinary(tempAssetFile)) tempAssetFile else null

                    if (sourceBin != null) {
                        sourceBin.copyTo(finalBin, overwrite = true)
                        finalBin.setExecutable(true, false)
                        try { Runtime.getRuntime().exec(arrayOf("chmod", "755", finalBin.absolutePath)).waitFor() } catch (_: Exception) {}

                        val codeCacheBin = context.codeCacheDir.resolve("Stockfish17.bin")
                        sourceBin.copyTo(codeCacheBin, overwrite = true)
                        codeCacheBin.setExecutable(true, false)
                        try { Runtime.getRuntime().exec(arrayOf("chmod", "755", codeCacheBin.absolutePath)).waitFor() } catch (_: Exception) {}

                        prefs.setModelPath(sfProfile.name, finalBin.absolutePath)
                        prefs.setModelStatus(sfProfile.name, "downloaded")
                        prefs.setModelError(sfProfile.name, "")
                        android.util.Log.i("OmniMind", "Successfully prepared Stockfish 17 binary from assets: ${finalBin.absolutePath}")
                    }
                    tempAssetFile.delete()
                    modelsDir.resolve("sf_asset_extracted").deleteRecursively()
                    updatePackImportState(sfProfile, false, 100, "")
                }
            } catch (t: Throwable) {
                android.util.Log.e("OmniMind", "Error extracting asset Stockfish", t)
                updatePackImportState(LocalModelProfile.Stockfish17, false, 0, "")
            }
        }
    }


    private suspend fun observeDownloads() {
        val workManager = WorkManager.getInstance(context)
        val modelsDir = context.filesDir.resolve("models")

        workManager.getWorkInfosByTagFlow("model_pack_download").collectLatest { workInfos ->
            val updatedPacks = _allPacks.value.toMutableList()
            var changed = false
            
            workInfos.forEach { workInfo ->
                val profileName = workInfo.tags.find { it.startsWith("profile:") }?.removePrefix("profile:")
                if (profileName != null) {
                    val index = updatedPacks.indexOfFirst { it.profile.name == profileName }
                    if (index != -1) {
                        val pack = updatedPacks[index]

                        val diskFile = if (pack.filePath.isNotEmpty()) File(pack.filePath) else File(modelsDir, "${pack.profile.name}.${defaultExtensionFor(pack.profile.type)}")
                        if (diskFile.exists() && diskFile.length() > 0 && (pack.profile.type != LocalModelType.CHESS_ENGINE || isElfBinary(diskFile))) {
                            if (!pack.isDownloaded || pack.downloadProgress != 100 || pack.lastError.isNotEmpty()) {
                                updatedPacks[index] = pack.copy(
                                    isDownloaded = true,
                                    downloadProgress = 100,
                                    filePath = diskFile.absolutePath,
                                    fileSize = formatFileSize(diskFile.length()),
                                    lastError = ""
                                )
                                changed = true
                            }
                            return@forEach
                        }

                        val progress = workInfo.progress.getInt("Progress", 0)
                        val status = when (workInfo.state) {
                            WorkInfo.State.SUCCEEDED -> "downloaded"
                            WorkInfo.State.FAILED -> "error"
                            WorkInfo.State.RUNNING -> "downloading"
                            WorkInfo.State.ENQUEUED -> "queued"
                            else -> "not_downloaded"
                        }
                        
                        val isDl = status == "downloaded"
                        updatedPacks[index] = pack.copy(
                            downloadProgress = if (isDl) 100 else progress,
                            isDownloaded = isDl,
                            statusMessage = if (status == "downloading") "Downloading $progress%..." else "",
                            lastError = if (status == "error") prefs.getModelError(profileName).first() else ""
                        )
                        changed = true
                    }
                }
            }
            if (changed) {
                _allPacks.value = updatedPacks
            }
        }
    }

    override suspend fun checkCompatibility(profile: LocalModelProfile): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                
                val availRamMb = memoryInfo.availMem / (1024 * 1024)
                val totalRamMb = memoryInfo.totalMem / (1024 * 1024)
                
                val isCompatible = when (profile.type) {
                    LocalModelType.LLM -> totalRamMb >= 3500 && availRamMb >= 1200
                 else -> true
                }
                
                Result.success(isCompatible)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cleanupOrphanedModels() {
        withContext(Dispatchers.IO) {
            val modelsDir = context.filesDir.resolve("models")
            if (!modelsDir.exists()) return@withContext
            
            val files = modelsDir.listFiles() ?: return@withContext
            files.forEach { file ->
                if (file.name.startsWith("temp_import_") || file.name.startsWith("uncompressed_")) {
                    file.delete()
                }
            }
        }
    }

    override suspend fun download(profile: LocalModelProfile) {

        if (profile.type == LocalModelType.TRANSLATION_PACK) {
            val langCode = profile.suggestedPackUrl.removePrefix("mlkit://translate/")
            val model = TranslateRemoteModel.Builder(langCode).build()
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            
            val remoteModelManager = RemoteModelManager.getInstance()
            try {
                // Update status to downloading immediately
                prefs.setModelStatus(profile.name, "downloading")
                prefs.setModelError(profile.name, "Starting download...")
                
                remoteModelManager.download(model, conditions).await()
                
                prefs.setModelStatus(profile.name, "downloaded")
                prefs.setModelPath(profile.name, "mlkit://$langCode")
                prefs.setModelError(profile.name, "")
                refresh()
            } catch (e: Exception) {
                prefs.setModelStatus(profile.name, "error")
                prefs.setModelError(profile.name, e.localizedMessage ?: "ML Kit download failed")
                refresh()
            }
            return
        }

        prefs.setModelStatus(profile.name, "queued")
        prefs.setModelError(profile.name, "")
        
        val request = OneTimeWorkRequestBuilder<ModelPackDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag("model_pack_download")
            .addTag("profile:${profile.name}")
            .setInputData(workDataOf("PROFILE_NAME" to profile.name))
            .build()
            
        WorkManager.getInstance(context).enqueue(request)
    }

    override suspend fun importLocalPack(uri: String, profile: LocalModelProfile) {
        withContext(Dispatchers.IO) {
            try {
                updatePackImportState(profile, true, 5, "Reading input file...")
                prefs.setModelError(profile.name, "")

                val modelUri = Uri.parse(uri)
                val rawFileName = getFileNameFromUri(context, modelUri) ?: "${profile.name}.${defaultExtensionFor(profile.type)}"
                android.util.Log.i("OmniMind", "[OmniMindImport] Starting model import: file '$rawFileName' (Uri: $modelUri) for profile '${profile.name}'")
                println("[OmniMindImport] Starting model import: file '$rawFileName' (Uri: $modelUri) for profile '${profile.name}'")
                
                val ext = rawFileName.substringAfterLast('.', defaultExtensionFor(profile.type))
                val modelsDir = context.filesDir.resolve("models").apply { mkdirs() }

                val totalBytes = try {
                    context.contentResolver.openFileDescriptor(modelUri, "r")?.use { it.statSize } ?: -1L
                } catch (e: Exception) { -1L }

                val tempFile = File(modelsDir, "temp_import_${profile.name}.$ext")

                context.contentResolver.openInputStream(modelUri)?.use { input ->
                    tempFile.outputStream().buffered(128 * 1024).use { output ->
                        val buffer = ByteArray(128 * 1024)
                        var bytesCopied = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            if (totalBytes > 0) {
                                val pct = ((bytesCopied * 70) / totalBytes).toInt().coerceIn(5, 75)
                                updatePackImportState(profile, true, pct, "Importing (${formatFileSize(bytesCopied)} / ${formatFileSize(totalBytes)})...")
                            }
                        }
                    }
                } ?: run {
                    prefs.setModelError(profile.name, "Failed to open input stream from selected file.")
                    updatePackImportState(profile, false, 0, "")
                    return@withContext
                }

                var savedPath = tempFile.absolutePath

                if (profile.type == LocalModelType.CHESS_ENGINE || profile == LocalModelProfile.Stockfish17) {
                    updatePackImportState(profile, true, 80, "Extracting Stockfish engine...")
                    val lowerName = rawFileName.lowercase()
                    val extractDir = File(modelsDir, "${profile.name}_extracted").apply { mkdirs() }
                    var foundBinary: File? = null

                    if (lowerName.endsWith(".tar") || lowerName.endsWith(".tar.gz") || lowerName.endsWith(".tgz") || lowerName.endsWith(".zip")) {
                        try {
                            if (lowerName.endsWith(".zip")) {
                                ZipInputStream(tempFile.inputStream().buffered()).use { zip ->
                                    var entry = zip.nextEntry
                                    while (entry != null) {
                                        if (!entry.isDirectory) {
                                            val outFile = File(extractDir, entry.name)
                                            outFile.parentFile?.mkdirs()
                                            outFile.outputStream().use { zip.copyTo(it) }
                                        }
                                        entry = zip.nextEntry
                                    }
                                }
                            } else {
                                val inputStream = if (lowerName.endsWith(".tar")) tempFile.inputStream() else org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream(tempFile.inputStream().buffered())
                                org.apache.commons.compress.archivers.tar.TarArchiveInputStream(inputStream.buffered()).use { tar ->
                                    var entry = tar.nextTarEntry
                                    while (entry != null) {
                                        if (!entry.isDirectory) {
                                            val outFile = File(extractDir, entry.name)
                                            outFile.parentFile?.mkdirs()
                                            outFile.outputStream().use { tar.copyTo(it) }
                                        }
                                        entry = tar.nextTarEntry
                                    }
                                }
                            }
                            foundBinary = extractDir.walkTopDown().find { !it.isDirectory && isElfBinary(it) }
                        } catch (e: Exception) {
                            android.util.Log.e("OmniMind", "Archive extraction failed", e)
                        }
                    }

                    val realBinary = foundBinary ?: if (isElfBinary(tempFile)) tempFile else extractDir.walkTopDown().find { !it.isDirectory && isElfBinary(it) }
                    
                    if (realBinary != null) {
                        val executableFile = File(modelsDir, "${profile.name}.bin")
                        realBinary.copyTo(executableFile, overwrite = true)
                        executableFile.setExecutable(true, false)
                        try { Runtime.getRuntime().exec(arrayOf("chmod", "755", executableFile.absolutePath)).waitFor() } catch (_: Exception) {}

                        if (profile == LocalModelProfile.Stockfish17) {
                            val codeCacheBin = context.codeCacheDir.resolve("Stockfish17.bin")
                            realBinary.copyTo(codeCacheBin, overwrite = true)
                            codeCacheBin.setExecutable(true, false)
                            try { Runtime.getRuntime().exec(arrayOf("chmod", "755", codeCacheBin.absolutePath)).waitFor() } catch (_: Exception) {}
                        }
                        savedPath = executableFile.absolutePath
                    } else {
                        prefs.setModelError(profile.name, "Import failed: Selected file is not a valid Stockfish ARM64 executable binary.")
                        if (tempFile.exists()) tempFile.delete()
                        if (extractDir.exists()) extractDir.deleteRecursively()
                        updatePackImportState(profile, false, 0, "")
                        return@withContext
                    }
                    if (tempFile.exists()) tempFile.delete()
                    if (extractDir.exists()) extractDir.deleteRecursively()
                }

                updatePackImportState(profile, true, 80, "Validating model format...")
                val validationResult = validateModel(savedPath, profile, onProgress = { pct, msg ->
                    updatePackImportState(profile, true, 80 + (pct * 15 / 100), msg)
                })

                if (validationResult != null) {
                    File(savedPath).delete()
                    prefs.setModelError(profile.name, validationResult)
                    updatePackImportState(profile, false, 0, "")
                    return@withContext
                }

                val sourceFile = File(savedPath)
                val targetFile = File(modelsDir, "${profile.name}.${sourceFile.extension}")
                if (sourceFile.absolutePath != targetFile.absolutePath) {
                    sourceFile.copyTo(targetFile, overwrite = true)
                    sourceFile.delete()
                }

                prefs.setModelPath(profile.name, targetFile.absolutePath)
                prefs.setModelPath(profile.id, targetFile.absolutePath)
                if (profile.type == LocalModelType.LLM) {
                    prefs.setLocalModel(profile.name)
                }
                prefs.setModelStatus(profile.name, "downloaded")
                prefs.setModelStatus(profile.id, "downloaded")
                prefs.setModelError(profile.name, "")
                prefs.setModelError(profile.id, "")
                
                android.util.Log.i("OmniMind", "[OmniMindImport] Successfully imported model file: '$rawFileName' -> '${targetFile.absolutePath}' (Size: ${formatFileSize(targetFile.length())})")
                println("[OmniMindImport] Successfully imported model file: '$rawFileName' -> '${targetFile.absolutePath}' (Size: ${formatFileSize(targetFile.length())})")

                updatePackImportState(profile, false, 100, "Ready to use")
                refresh()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                android.util.Log.e("OmniMind", "Import failed", t)
                val msg = if (t is OutOfMemoryError) "Import failed: Device ran out of memory." else "Import failed: ${t.localizedMessage}"
                prefs.setModelError(profile.name, msg)
                updatePackImportState(profile, false, 0, "")
            } finally {
                cleanupOrphanedModels()
            }
        }
    }

    private suspend fun validateModel(
        path: String,
        profile: LocalModelProfile,
        onProgress: (Int, String) -> Unit
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) return@withContext "Model file does not exist on disk."

                when (profile.type) {
                    LocalModelType.LLM -> {
                        if (file.length() < 10 * 1024 * 1024) {
                            return@withContext "File is too small (${file.length() / (1024 * 1024)}MB) to be a valid LLM model file."
                        }

                        var isZip = false
                        var needsUncompressedRepackage = false
                        try {
                            ZipFile(file).use { zipFile ->
                                isZip = true
                                val entries = zipFile.entries()
                                while (entries.hasMoreElements()) {
                                    val entry = entries.nextElement()
                                    if (entry.method != ZipEntry.STORED && !entry.isDirectory) {
                                        needsUncompressedRepackage = true
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            isZip = false
                        }

                        if (!isZip) {
                            return@withContext "Import Error: '${file.name}' is a raw binary/GGUF file, not a MediaPipe '.task' bundle. On-device LLM requires a MediaPipe .task model bundle. Tap Download in Settings to fetch the official task file."
                        }

                        if (needsUncompressedRepackage) {
                            onProgress(10, "Optimizing model for MediaPipe engine...")
                            val tempRepackFile = File(file.parentFile, "uncompressed_${file.nameWithoutExtension}.task")
                            try {
                                val zos = ZipOutputStream(tempRepackFile.outputStream().buffered(128 * 1024))
                                zos.setLevel(java.util.zip.Deflater.NO_COMPRESSION)

                                if (isZip) {
                                    ZipFile(file).use { zipFile ->
                                        val entries = zipFile.entries().toList().filter { !it.isDirectory }
                                        val totalEntries = entries.size
                                        var hasPrefillDecode = entries.any { it.name == "TF_LITE_PREFILL_DECODE" }
                                        var hasMetadata = entries.any { it.name == "METADATA" }
                                        var hasExactTokenizer = entries.any { it.name == "TOKENIZER_MODEL" }

                                        entries.forEachIndexed { idx, entry ->
                                            onProgress(10 + ((idx + 1) * 70 / totalEntries), "Repackaging entry '${entry.name}'...")

                                            val (crcVal, sizeVal) = zipFile.getInputStream(entry).use { stream ->
                                                val crc = CRC32()
                                                val buf = ByteArray(128 * 1024)
                                                var n: Int
                                                var len = 0L
                                                while (stream.read(buf).also { n = it } != -1) {
                                                    crc.update(buf, 0, n)
                                                    len += n
                                                }
                                                Pair(crc.value, len)
                                            }

                                            val isTokenizerFile = !hasExactTokenizer && (entry.name.contains("tokenizer", ignoreCase = true) || entry.name.endsWith(".model") || entry.name.endsWith(".spm"))
                                            val entryName = when {
                                                !hasPrefillDecode && (entry.name.endsWith(".bin") || entry.name.endsWith(".tflite") || entry.name == "model.bin" || entry.size > 10 * 1024 * 1024) -> "TF_LITE_PREFILL_DECODE"
                                                isTokenizerFile -> "TOKENIZER_MODEL"
                                                else -> entry.name
                                            }

                                            val newEntry = ZipEntry(entryName).apply {
                                                method = ZipEntry.STORED
                                                size = sizeVal
                                                compressedSize = sizeVal
                                                crc = crcVal
                                            }
                                            zos.putNextEntry(newEntry)
                                            zipFile.getInputStream(entry).use { input ->
                                                input.copyTo(zos, bufferSize = 128 * 1024)
                                            }
                                            zos.closeEntry()
                                        }

                                        if (!hasMetadata) {
                                            val metadataBytes = """{"backend":"CPU","start_token":"<bos>","stop_tokens":["<eos>","<end_of_turn>"]}""".encodeToByteArray()
                                            val metaCrc = CRC32().apply { update(metadataBytes) }.value
                                            val metaEntry = ZipEntry("METADATA").apply {
                                                method = ZipEntry.STORED
                                                size = metadataBytes.size.toLong()
                                                compressedSize = metadataBytes.size.toLong()
                                                crc = metaCrc
                                            }
                                            zos.putNextEntry(metaEntry)
                                            zos.write(metadataBytes)
                                            zos.closeEntry()
                                        }

                                        if (!hasExactTokenizer) {
                                            val tokenBytes = createMinimalSentencePieceTokenizer()
                                            val tokenCrc = CRC32().apply { update(tokenBytes) }.value
                                            val tokenEntry = ZipEntry("TOKENIZER_MODEL").apply {
                                                method = ZipEntry.STORED
                                                size = tokenBytes.size.toLong()
                                                compressedSize = tokenBytes.size.toLong()
                                                crc = tokenCrc
                                            }
                                            zos.putNextEntry(tokenEntry)
                                            zos.write(tokenBytes)
                                            zos.closeEntry()
                                        }
                                    }
                                } else {
                                    onProgress(40, "Calculating CRC checksum...")
                                    val (crcVal, sizeVal) = file.inputStream().use { stream ->
                                        val crc = CRC32()
                                        val buf = ByteArray(128 * 1024)
                                        var n: Int
                                        var len = 0L
                                        while (stream.read(buf).also { n = it } != -1) {
                                            crc.update(buf, 0, n)
                                            len += n
                                        }
                                        Pair(crc.value, len)
                                    }

                                    onProgress(60, "Writing MediaPipe task bundle...")
                                    val entry1 = ZipEntry("TF_LITE_PREFILL_DECODE").apply {
                                        method = ZipEntry.STORED
                                        size = sizeVal
                                        compressedSize = sizeVal
                                        crc = crcVal
                                    }
                                    zos.putNextEntry(entry1)
                                    file.inputStream().use { input -> input.copyTo(zos, bufferSize = 128 * 1024) }
                                    zos.closeEntry()

                                    val metadataBytes = """{"backend":"CPU","start_token":"<bos>","stop_tokens":["<eos>","<end_of_turn>"]}""".encodeToByteArray()
                                    val metaCrc = CRC32().apply { update(metadataBytes) }.value
                                    val metaEntry = ZipEntry("METADATA").apply {
                                        method = ZipEntry.STORED
                                        size = metadataBytes.size.toLong()
                                        compressedSize = metadataBytes.size.toLong()
                                        crc = metaCrc
                                    }
                                    zos.putNextEntry(metaEntry)
                                    zos.write(metadataBytes)
                                    zos.closeEntry()

                                    val tokenBytes = createMinimalSentencePieceTokenizer()
                                    val tokenCrc = CRC32().apply { update(tokenBytes) }.value
                                    val tokenEntry = ZipEntry("TOKENIZER_MODEL").apply {
                                        method = ZipEntry.STORED
                                        size = tokenBytes.size.toLong()
                                        compressedSize = tokenBytes.size.toLong()
                                        crc = tokenCrc
                                    }
                                    zos.putNextEntry(tokenEntry)
                                    zos.write(tokenBytes)
                                    zos.closeEntry()
                                }
                                zos.close()

                                if (tempRepackFile.exists() && tempRepackFile.length() > 0) {
                                    file.delete()
                                    tempRepackFile.renameTo(file)
                                    android.util.Log.i("OmniMind", "Successfully repackaged LLM task bundle to uncompressed STORED ZIP.")
                                }
                            } catch (t: Throwable) {
                                android.util.Log.e("OmniMind", "Failed to repackage task bundle", t)
                                if (tempRepackFile.exists()) tempRepackFile.delete()
                                return@withContext "Failed to repackage model bundle: ${t.localizedMessage}"
                            }
                        }
                    }
                    LocalModelType.CHESS_ENGINE -> {
                        if (!isElfBinary(file)) {
                            return@withContext "File is not a valid Stockfish ELF binary executable."
                        }
                        file.setExecutable(true, false)
                    }
                    else -> {
                        if (file.length() < 100 * 1024) {
                            return@withContext "File is too small to be a valid model."
                        }
                    }
                }
                null
            } catch (t: Throwable) {
                val errorMsg = t.localizedMessage ?: "Unknown error"
                "Model validation failed: $errorMsg"
            }
        }
    }

    override suspend fun deletePack(profile: LocalModelProfile) {
        withContext(Dispatchers.IO) {
            if (profile.type == LocalModelType.TRANSLATION_PACK) {
                val langCode = profile.suggestedPackUrl.removePrefix("mlkit://")
                val model = TranslateRemoteModel.Builder(langCode).build()
                RemoteModelManager.getInstance().deleteDownloadedModel(model).await()
                refresh()
                return@withContext
            }

            val path = prefs.getModelPath(profile.name).first()
            if (path.isNotEmpty()) {
                File(path).delete()
            }
            val modelsDir = context.filesDir.resolve("models")
            modelsDir.listFiles()?.filter { it.name.startsWith(profile.name) }?.forEach { it.delete() }
            
            prefs.setModelPath(profile.name, "")
            prefs.setModelStatus(profile.name, "not_downloaded")
            prefs.setModelError(profile.name, "")
            refresh()
        }
    }

    override suspend fun checkForUpdates(): Result<Boolean> {
        refresh()
        return Result.success(false)
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}

internal fun createMinimalSentencePieceTokenizer(): ByteArray {
    val baos = java.io.ByteArrayOutputStream()
    
    fun writeVarint(os: java.io.ByteArrayOutputStream, value: Int) {
        var v = value
        while (v ushr 7 != 0) {
            os.write((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        os.write(v and 0x7F)
    }

    fun writeFieldString(os: java.io.ByteArrayOutputStream, fieldNum: Int, str: String) {
        val bytes = str.encodeToByteArray()
        writeVarint(os, (fieldNum shl 3) or 2)
        writeVarint(os, bytes.size)
        os.write(bytes)
    }

    fun writeFieldInt(os: java.io.ByteArrayOutputStream, fieldNum: Int, value: Int) {
        writeVarint(os, (fieldNum shl 3) or 0)
        writeVarint(os, value)
    }

    // 1. TrainerSpec (Field 1 of ModelProto)
    val trainerOs = java.io.ByteArrayOutputStream()
    writeFieldInt(trainerOs, 3, 0) // unk_id = 0
    writeFieldInt(trainerOs, 4, 1) // bos_id = 1
    writeFieldInt(trainerOs, 5, 2) // eos_id = 2
    writeFieldInt(trainerOs, 6, 3) // pad_id = 3
    writeFieldString(trainerOs, 10, "<unk>")
    writeFieldString(trainerOs, 11, "<s>")
    writeFieldString(trainerOs, 12, "</s>")
    writeFieldString(trainerOs, 13, "<pad>")
    
    val trainerBytes = trainerOs.toByteArray()
    writeVarint(baos, (1 shl 3) or 2)
    writeVarint(baos, trainerBytes.size)
    baos.write(trainerBytes)

    // 2. Pieces (Field 3 of ModelProto)
    // SentencePiece message: piece (field 1 string), score (field 2 float32), type (field 3 enum)
    fun createPiece(text: String, typeVal: Int, score: Float = 0.0f): ByteArray {
        val pOs = java.io.ByteArrayOutputStream()
        writeFieldString(pOs, 1, text)
        
        val bits = java.lang.Float.floatToIntBits(score)
        writeVarint(pOs, (2 shl 3) or 5) // tag 2 wire type 5 (fixed32)
        pOs.write(bits and 0xFF)
        pOs.write((bits ushr 8) and 0xFF)
        pOs.write((bits ushr 16) and 0xFF)
        pOs.write((bits ushr 24) and 0xFF)
        
        writeFieldInt(pOs, 3, typeVal)
        return pOs.toByteArray()
    }

    val pieces = listOf(
        createPiece("<unk>", 2),
        createPiece("<s>", 3),
        createPiece("</s>", 3),
        createPiece("<bos>", 3),
        createPiece("<eos>", 3),
        createPiece("<pad>", 3)
    )

    for (pBytes in pieces) {
        writeVarint(baos, (3 shl 3) or 2)
        writeVarint(baos, pBytes.size)
        baos.write(pBytes)
    }

    return baos.toByteArray()
}

internal fun isTfliteFlatBuffer(file: File): Boolean {
    if (!file.exists() || file.length() < 100) return false
    return try {
        file.inputStream().use { stream ->
            val buf = ByteArray(8)
            val read = stream.read(buf)
            if (read < 8) false
            else buf[4] == 'T'.code.toByte() && buf[5] == 'F'.code.toByte() && buf[6] == 'L'.code.toByte() && buf[7] == '3'.code.toByte()
        }
    } catch (e: Exception) { false }
}

internal fun zipContainsTflite(file: File): Boolean {
    if (!file.exists() || file.length() < 100) return false
    return try {
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            var found = false
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && (entry.name == "TF_LITE_PREFILL_DECODE" || entry.name.endsWith(".task") || entry.name.endsWith(".tflite"))) {
                    found = true
                    break
                }
                if (!entry.isDirectory && entry.size > 10 * 1024 * 1024) {
                    zip.getInputStream(entry).use { stream ->
                        val buf = ByteArray(8)
                        val read = stream.read(buf)
                        if (read >= 8 && buf[4] == 'T'.code.toByte() && buf[5] == 'F'.code.toByte() && buf[6] == 'L'.code.toByte() && buf[7] == '3'.code.toByte()) {
                            found = true
                        }
                    }
                    if (found) break
                }
            }
            found
        }
    } catch (e: Exception) { false }
}
