package com.creativeblisslabs.omnichessreader.core.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(
    onFileSelected: (uri: String) -> Unit
): () -> Unit {
    return {
        println("File picker not yet implemented for iOS")
    }
}
