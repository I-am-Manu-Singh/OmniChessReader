package com.creativeblisslabs.omnimind.core.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(
    onFileSelected: (uri: String) -> Unit
): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onFileSelected(it.toString()) }
    }
    return { launcher.launch(arrayOf("*/*")) }
}
