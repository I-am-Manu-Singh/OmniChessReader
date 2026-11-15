package com.creativeblisslabs.omnichessreader.core.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(
    onFileSelected: (uri: String) -> Unit
): () -> Unit
