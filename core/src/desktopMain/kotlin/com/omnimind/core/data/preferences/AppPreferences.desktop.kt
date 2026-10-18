package com.creativeblisslabs.omnimind.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import java.io.File

actual fun createDataStore(): DataStore<Preferences> {
    val dir = File(System.getProperty("user.home"), ".omnimind")
    dir.mkdirs()
    return PreferenceDataStoreFactory.createWithPath { "${dir.absolutePath}/prefs.preferences_pb".toPath() }
}
