package com.creativeblisslabs.omnimind.core.media

import androidx.compose.runtime.Composable

data class MediaAttachmentUi(val type: String, val name: String, val path: String)

data class MediaActions(
    val pickImage: () -> Unit,
    val pickFile: () -> Unit,
    val pickAudio: () -> Unit,
    val startRecording: () -> Unit,
    val stopRecording: () -> Unit,
    val startPcmRecording: () -> Unit,
    val stopPcmRecording: (onResult: (FloatArray) -> Unit) -> Unit,
    val playAudio: (String) -> Unit,
    val stopPlayback: () -> Unit,
    val speakText: (String, language: String?, speed: Float?, pitch: Float?) -> Unit,
    val startVoiceInput: ((String) -> Unit) -> Unit,
    val shareMedia: (String, String) -> Unit,
    val saveToGallery: (String, String) -> Unit
)

@Composable
expect fun rememberMediaActions(
    onImagePicked: (MediaAttachmentUi) -> Unit,
    onFilePicked: (MediaAttachmentUi) -> Unit,
    onAudioPicked: (MediaAttachmentUi) -> Unit,
    onClipboardImage: (MediaAttachmentUi) -> Unit,
    speakText: (String, String?, Float?, Float?) -> Unit = { _, _, _, _ -> },
    startVoiceInput: ((String) -> Unit) -> Unit = {}
): MediaActions