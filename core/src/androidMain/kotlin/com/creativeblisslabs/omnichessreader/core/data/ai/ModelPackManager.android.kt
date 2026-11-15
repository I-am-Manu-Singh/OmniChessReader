package com.creativeblisslabs.omnichessreader.core.data.ai

import android.content.Context
import org.koin.core.context.GlobalContext

actual fun createModelPackManager(): ModelPackManager {
    val context: Context = GlobalContext.get().get()
    val prefs = GlobalContext.get().get<com.creativeblisslabs.omnichessreader.core.data.preferences.AppPreferences>()
    return AndroidModelPackManager(context, prefs)
}

