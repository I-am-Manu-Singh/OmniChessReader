package com.creativeblisslabs.omnimind.chess.shared.logic

import platform.Foundation.*

actual class AssetManager {
    actual suspend fun readTextFile(path: String): String {
        // Simple implementation for iOS using NSBundle
        val fileName = path.substringAfterLast("/")
        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".")
        
        val bundlePath = NSBundle.mainBundle.pathForResource(baseName, extension)
        return if (bundlePath != null) {
            NSString.stringWithContentsOfFile(bundlePath, NSUTF8StringEncoding, null) ?: ""
        } else {
            ""
        }
    }
}
