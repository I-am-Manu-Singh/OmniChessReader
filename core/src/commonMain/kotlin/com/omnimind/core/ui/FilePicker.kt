package com.creativeblisslabs.omnimind.core.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(
    onFileSelected: (uri: String) -> Unit
): () -> Unit
