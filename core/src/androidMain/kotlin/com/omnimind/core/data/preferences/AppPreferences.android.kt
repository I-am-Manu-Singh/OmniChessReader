package com.creativeblisslabs.omnimind.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import org.koin.core.context.GlobalContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omnimind_prefs")

actual fun createDataStore(): DataStore<Preferences> {
    val context: Context = GlobalContext.get().get()
    return context.dataStore
}
