package com.creativeblisslabs.omnichessreader.feature.reader.di

import com.creativeblisslabs.omnichessreader.feature.reader.ReaderHubViewModel
import com.creativeblisslabs.omnichessreader.feature.reader.engine.BookAnalyzer
import org.koin.dsl.module
import com.creativeblisslabs.omnichessreader.core.data.ai.OmniAiService
import com.creativeblisslabs.omnichessreader.core.data.database.dao.BookDao
import com.creativeblisslabs.omnichessreader.core.data.database.dao.BookChunkDao
import com.creativeblisslabs.omnichessreader.core.data.database.dao.BookmarkDao
import com.creativeblisslabs.omnichessreader.core.data.ai.OnDeviceAiRuntime
import com.creativeblisslabs.omnichessreader.core.media.MediaProcessor
import com.creativeblisslabs.omnichessreader.core.data.preferences.AppPreferences
import org.koin.core.module.dsl.viewModelOf

val readerModule = module {
    single { BookAnalyzer(get<OmniAiService>()) }
    viewModelOf(::ReaderHubViewModel)
}
