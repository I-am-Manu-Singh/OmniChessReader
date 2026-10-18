package com.creativeblisslabs.omnimind.core.data.ai

import android.content.Context
import org.koin.core.context.GlobalContext

actual fun createModelPackManager(): ModelPackManager {
    val context: Context = GlobalContext.get().get()
    val prefs = GlobalContext.get().get<com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences>()
    return AndroidModelPackManager(context, prefs)
}

