package com.creativeblisslabs.omnimind.chess.shared.logic

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class AssetManager(private val context: Context) {
    actual suspend fun readTextFile(path: String): String = withContext(Dispatchers.IO) {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
