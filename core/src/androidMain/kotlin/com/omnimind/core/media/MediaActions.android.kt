package com.creativeblisslabs.omnimind.core.media

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import java.io.File
import java.util.UUID

private class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start(): File {
        val file = File(context.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        currentFile = file
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        return file
    }

    fun stop(): File? {
        val file = currentFile
        try {
            recorder?.stop()
        } catch (_: Throwable) {
        }
        recorder?.release()
        recorder = null
        currentFile = null
        return file
    }
}

private class PcmAudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT) * 2
    private val recordedData = mutableListOf<Float>()

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (isRecording) return
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            recordedData.clear()
            audioRecord?.startRecording()
            isRecording = true
            
            Thread {
                val buffer = FloatArray(bufferSize / 4)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: -1
                    if (read > 0) {
                        synchronized(recordedData) {
                            for (i in 0 until read) recordedData.add(buffer[i])
                        }
                    }
                }
            }.start()
        } catch (e: SecurityException) {
            Log.e("OmniMind", "Mic permission missing for AudioRecord", e)
        }
    }

    fun stop(onResult: (FloatArray) -> Unit) {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        val data = synchronized(recordedData) { recordedData.toFloatArray() }
        onResult(data)
    }
}

private class AudioPlayback {
    private var player: MediaPlayer? = null
    fun play(path: String) {
        stop()
        player = MediaPlayer().apply {
            setDataSource(path)
            prepare()
            start()
        }
    }

    fun stop() {
        player?.release()
        player = null
    }
}

private enum class RationaleType { MICROPHONE, STORAGE }

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun rememberMediaActions(
    onImagePicked: (MediaAttachmentUi) -> Unit,
    onFilePicked: (MediaAttachmentUi) -> Unit,
    onAudioPicked: (MediaAttachmentUi) -> Unit,
    onClipboardImage: (MediaAttachmentUi) -> Unit,
    speakText: (String, String?, Float?, Float?) -> Unit,
    startVoiceInput: ((String) -> Unit) -> Unit
): MediaActions {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    val pcmRecorder = remember { PcmAudioRecorder(context) }
    val playback = remember { AudioPlayback() }
    val speech = remember { AndroidSpeechSupport(context) }

    var rationaleType by remember { mutableStateOf<RationaleType?>(null) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO) { granted ->
        if (granted) recorder.start()
    }

    val storagePermissionState = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE) { granted ->
            if (granted) pendingAction?.invoke()
            pendingAction = null
        }
    } else null

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onImagePicked(uriToAttachment(context, it, "image")) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onFilePicked(uriToAttachment(context, it, "file")) }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onAudioPicked(uriToAttachment(context, it, "audio")) }
    }

    if (rationaleType != null) {
        GlassRationaleDialog(
            title = if (rationaleType == RationaleType.MICROPHONE) "Microphone Access" else "Storage Access",
            description = if (rationaleType == RationaleType.MICROPHONE) {
                "OmniMind needs access to your microphone to record voice messages and understand your audio queries."
            } else {
                "To attach files and media on this version of Android, OmniMind needs access to your storage."
            },
            onConfirm = {
                val type = rationaleType
                rationaleType = null
                if (type == RationaleType.MICROPHONE) {
                    micPermissionState.launchPermissionRequest()
                } else {
                    storagePermissionState?.launchPermissionRequest()
                }
            },
            onDismiss = {
                rationaleType = null
                pendingAction = null
            }
        )
    }

    LaunchedEffect(Unit) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        val item = clip?.getItemAt(0)
        val uri = item?.uri
        if (uri != null && clipboardHasImage(clip)) onClipboardImage(uriToAttachment(context, uri, "screenshot"))
    }

    return MediaActions(
        pickImage = { imagePicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        pickFile = {
            val action = { filePicker.launch("*/*") }
            if (storagePermissionState == null || storagePermissionState.status.isGranted) {
                action()
            } else if (storagePermissionState.status.shouldShowRationale) {
                pendingAction = action
                rationaleType = RationaleType.STORAGE
            } else {
                pendingAction = action
                storagePermissionState.launchPermissionRequest()
            }
        },
        pickAudio = {
            val action = { audioPicker.launch("audio/*") }
            if (storagePermissionState == null || storagePermissionState.status.isGranted) {
                action()
            } else if (storagePermissionState.status.shouldShowRationale) {
                pendingAction = action
                rationaleType = RationaleType.STORAGE
            } else {
                pendingAction = action
                storagePermissionState.launchPermissionRequest()
            }
        },
        startRecording = {
            when {
                micPermissionState.status.isGranted -> recorder.start()
                micPermissionState.status.shouldShowRationale -> rationaleType = RationaleType.MICROPHONE
                else -> micPermissionState.launchPermissionRequest()
            }
        },
        stopRecording = { recorder.stop()?.let { onAudioPicked(MediaAttachmentUi("audio", it.name, it.absolutePath)) } },
        startPcmRecording = {
            if (micPermissionState.status.isGranted) pcmRecorder.start()
            else micPermissionState.launchPermissionRequest()
        },
        stopPcmRecording = { onResult -> pcmRecorder.stop(onResult) },
        playAudio = { playback.play(it) },
        stopPlayback = { playback.stop() },
        speakText = { text, lang, speed, pitch -> speech.speak(text, lang, speed, pitch) },
        startVoiceInput = { onResult -> speech.startVoiceInput(onResult) },
        shareMedia = { path, mimeType ->
            val file = File(path)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Media"))
            }
        },
        saveToGallery = { path, mimeType ->
            val file = File(path)
            if (file.exists()) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                val collection = if (mimeType.startsWith("image/")) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if (mimeType.startsWith("video/")) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val uri = context.contentResolver.insert(collection, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        context.contentResolver.update(it, values, null, null)
                    }
                    Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@Composable
private fun GlassRationaleDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Deny")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Allow")
                    }
                }
            }
        }
    }
}

private fun uriToAttachment(context: Context, uri: Uri, fallbackType: String): MediaAttachmentUi {
    val name = queryDisplayName(context, uri) ?: "${fallbackType}_${System.currentTimeMillis()}"
    val file = File(context.cacheDir, name.replace("/", "_"))
    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    return MediaAttachmentUi(fallbackType, name, file.absolutePath)
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
    }

private fun clipboardHasImage(clip: ClipData?): Boolean {
    val desc = clip?.description ?: return false
    for (i in 0 until desc.mimeTypeCount) if (desc.getMimeType(i).startsWith("image/")) return true
    return false
}
