package com.creativeblisslabs.omnimind.core.data.ai

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.koin.core.context.GlobalContext

import androidx.work.workDataOf
import java.io.InputStream
import java.io.OutputStream
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat

class ModelPackDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val PROGRESS = "Progress"
        const val KEY_PROFILE_NAME = "PROFILE_NAME"
    }

    override suspend fun doWork(): Result {
        val prefs = org.koin.core.context.GlobalContext.get().get<AppPreferences>()
        val profileName = inputData.getString(KEY_PROFILE_NAME) ?: return Result.failure()
        val profile = LocalModelProfile.entries.find { it.name == profileName } ?: return Result.failure()
        
        val rawSourceUrl = prefs.getModelUrl(profileName).first().ifBlank { profile.suggestedPackUrl }
        
        if (rawSourceUrl.isBlank()) {
            val error = "Model source URL is empty for ${profile.label}."
            prefs.setModelStatus(profileName, "missing_url")
            prefs.setModelError(profileName, error)
            return Result.failure()
        }
        
        val hfToken = prefs.hfToken.first().ifBlank { "" }
        var sourceUrl = rawSourceUrl.replace("/blob/", "/resolve/")
        
        val fileName = URL(sourceUrl).path.substringAfterLast('/')
        val targetPath = applicationContext.filesDir.resolve("models/$fileName")
        targetPath.parentFile?.mkdirs()
        
        val downloadResult = withContext(Dispatchers.IO) {
            try {
                var currentUrl = sourceUrl
                var connection: HttpURLConnection? = null
                var redirects = 0
                val maxRedirects = 5

                while (redirects < maxRedirects) {
                    val urlObj = URL(currentUrl)
                    connection = (urlObj.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 30000
                        readTimeout = 60000
                        instanceFollowRedirects = false // Handle redirects manually
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:128.0) Gecko/128.0 Firefox/128.0")
                        // Send Authorization header ONLY to huggingface.co domain
                        if (urlObj.host.contains("huggingface.co")) {
                            setRequestProperty("Authorization", "Bearer $hfToken")
                        }
                    }

                    val code = connection.responseCode
                    if (code in 300..399) {
                        val loc = connection.getHeaderField("Location")
                        if (loc.isNullOrBlank()) return@withContext "Download redirect failed: Empty location header"
                        currentUrl = if (loc.startsWith("http")) loc else URL(urlObj, loc).toString()
                        connection.disconnect()
                        redirects++
                        continue
                    }

                    if (code !in 200..299) {
                        return@withContext "Download failed with HTTP status $code from $currentUrl"
                    }
                    break
                }

                val activeConn = connection ?: return@withContext "Failed to establish connection to model source"
                val contentType = activeConn.contentType ?: ""
                if (contentType.contains("text/html")) {
                    return@withContext "Downloaded HTML page instead of binary model file."
                }

                val contentLength = activeConn.contentLength.toLong()
                activeConn.inputStream.use { input ->
                    targetPath.outputStream().use { output ->
                        copyAndReportProgress(input, output, contentLength)
                    }
                }

                if (!targetPath.exists() || targetPath.length() < 100 * 1024) {
                    return@withContext "Downloaded model file is empty or corrupted."
                }

                null // Success
            } catch (e: Exception) {
                e.localizedMessage ?: "Network error during download"
            }
        }

        if (downloadResult != null || !targetPath.exists() || targetPath.length() < 1024) {
            targetPath.delete()
            val error = downloadResult ?: "Downloaded model is invalid."
            prefs.setModelStatus(profileName, "download_failed")
            prefs.setModelError(profileName, error)
            return Result.failure()
        }

        // Validation based on type
        if (profile.type == LocalModelType.LLM) {
            setProgress(workDataOf(PROGRESS to 95))
            val validationError = validateLlm(targetPath.absolutePath)
            if (validationError != null) {
                targetPath.delete()
                prefs.setModelStatus(profileName, "invalid_signature")
                prefs.setModelError(profileName, validationError)
                return Result.failure()
            }
        }

        setProgress(workDataOf(PROGRESS to 100))
        
        // Handle decompression for specific formats (.tar, .tar.gz, .zip)
        val finalPath = if (fileName.endsWith(".tar") || fileName.endsWith(".tar.gz") || fileName.endsWith(".tgz") || fileName.endsWith(".zip")) {
            setProgress(workDataOf(PROGRESS to 91))
            val extractedPath = withContext(Dispatchers.IO) {
                try {
                    val extractDir = targetPath.parentFile?.resolve(profile.name) ?: return@withContext null
                    extractDir.mkdirs()
                    
                    if (fileName.endsWith(".zip")) {
                        ZipInputStream(targetPath.inputStream().buffered()).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    val outFile = File(extractDir, entry.name)
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { zip.copyTo(it) }
                                    if (entry.name.contains("stockfish", ignoreCase = true)) {
                                        outFile.setExecutable(true)
                                    }
                                }
                                entry = zip.nextEntry
                            }
                        }
                    } else {
                        val inputStream = if (fileName.endsWith(".tar")) targetPath.inputStream() else GzipCompressorInputStream(targetPath.inputStream().buffered())
                        TarArchiveInputStream(inputStream.buffered()).use { tar ->
                            var entry = tar.nextTarEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    val outFile = File(extractDir, entry.name)
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { tar.copyTo(it) }
                                    if (entry.name.contains("stockfish", ignoreCase = true)) {
                                        outFile.setExecutable(true)
                                    }
                                }
                                entry = tar.nextTarEntry
                            }
                        }
                    }
                    // Clean up original archive
                    targetPath.delete()
                    
                    // Find true ELF binary in extracted dir
                    val binary = extractDir.walkTopDown().find { !it.isDirectory && isElfBinary(it) }
                    if (binary != null) {
                        val finalBin = applicationContext.filesDir.resolve("models/${profile.name}.bin")
                        binary.copyTo(finalBin, overwrite = true)
                        finalBin.setExecutable(true, false)
                        extractDir.deleteRecursively()
                        finalBin.absolutePath
                    } else {
                        extractDir.deleteRecursively()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (extractedPath == null) {
                prefs.setModelStatus(profileName, "download_failed")
                prefs.setModelError(profileName, "Extraction failed: No valid Stockfish ARM64 binary found in downloaded archive.")
                return Result.failure()
            }
            extractedPath
        } else {
            if (profile.type == LocalModelType.CHESS_ENGINE && !isElfBinary(targetPath)) {
                targetPath.delete()
                prefs.setModelStatus(profileName, "download_failed")
                prefs.setModelError(profileName, "Downloaded file is not a valid Stockfish ELF executable binary.")
                return Result.failure()
            }
            targetPath.setExecutable(true, false)
            targetPath.absolutePath
        }

        prefs.setModelPath(profileName, finalPath)
        prefs.setModelStatus(profileName, "downloaded")
        prefs.setModelError(profileName, "")
        showCompletionNotification(profile.label)
        return Result.success()
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

    private fun showCompletionNotification(modelLabel: String) {
        val channelId = "model_downloads"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Model Downloads", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                applicationContext, 0, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Model Download Complete")
            .setContentText("Local model '$modelLabel' is ready to use.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(modelLabel.hashCode(), notification)
    }

    private suspend fun validateLlm(path: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .build()
                LlmInference.createFromOptions(applicationContext, options).close()
                null
            } catch (t: Throwable) {
                "Invalid LLM: ${t.localizedMessage}"
            }
        }
    }

    private suspend fun copyAndReportProgress(input: InputStream, output: OutputStream, totalBytes: Long) {
        val buffer = ByteArray(64 * 1024)
        var bytesCopied: Long = 0
        var read = input.read(buffer)
        var lastProgress = 0
        
        while (read >= 0) {
            output.write(buffer, 0, read)
            bytesCopied += read
            
            if (totalBytes > 0) {
                val progress = ((bytesCopied * 100) / totalBytes).toInt()
                if (progress > lastProgress) {
                    lastProgress = progress
                    setProgress(workDataOf(PROGRESS to progress.coerceIn(0, 90)))
                }
            }
            read = input.read(buffer)
        }
    }
}
