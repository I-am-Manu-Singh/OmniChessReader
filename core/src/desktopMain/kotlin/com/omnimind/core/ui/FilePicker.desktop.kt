package com.creativeblisslabs.omnimind.core.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(
    onFileSelected: (uri: String) -> Unit
): () -> Unit {
    return {
        println("File picker not yet implemented for desktop")
    }
}
