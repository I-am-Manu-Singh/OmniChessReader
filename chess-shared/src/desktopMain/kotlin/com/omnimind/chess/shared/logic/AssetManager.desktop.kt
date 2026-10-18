package com.creativeblisslabs.omnimind.chess.shared.logic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class AssetManager {
    actual suspend fun readTextFile(path: String): String = withContext(Dispatchers.IO) {
        AssetManager::class.java.classLoader.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() } ?: ""
    }
}
