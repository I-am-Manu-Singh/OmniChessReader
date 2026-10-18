package com.creativeblisslabs.omnimind.core.media

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMediaActions(
    onImagePicked: (MediaAttachmentUi) -> Unit,
    onFilePicked: (MediaAttachmentUi) -> Unit,
    onAudioPicked: (MediaAttachmentUi) -> Unit,
    onClipboardImage: (MediaAttachmentUi) -> Unit,
    speakText: (String) -> Unit,
    startVoiceInput: ((String) -> Unit) -> Unit
): MediaActions = MediaActions(
    pickImage = {},
    pickFile = {},
    pickAudio = {},
    startRecording = {},
    stopRecording = {},
    playAudio = {},
    stopPlayback = {},
    speakText = speakText,
    startVoiceInput = startVoiceInput,
    shareMedia = { _, _ -> },
    saveToGallery = { _, _ -> }
)
