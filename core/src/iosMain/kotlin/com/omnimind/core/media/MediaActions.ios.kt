package com.creativeblisslabs.omnimind.core.media

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMediaActions(
    onImagePicked: (MediaAttachmentUi) -> Unit,
    onFilePicked: (MediaAttachmentUi) -> Unit,
    onAudioPicked: (MediaAttachmentUi) -> Unit,
    onClipboardImage: (MediaAttachmentUi) -> Unit
): MediaActions = MediaActions(
    pickImage = {},
    pickFile = {},
    pickAudio = {},
    startRecording = {},
    stopRecording = {},
    playAudio = {},
    stopPlayback = {}
)
