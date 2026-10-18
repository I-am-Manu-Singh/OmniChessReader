package com.creativeblisslabs.omnimind.feature.reader.di

import com.creativeblisslabs.omnimind.feature.reader.ReaderHubViewModel
import com.creativeblisslabs.omnimind.feature.reader.engine.BookAnalyzer
import org.koin.dsl.module
import com.creativeblisslabs.omnimind.core.data.ai.OmniAiService
import com.creativeblisslabs.omnimind.core.data.database.dao.BookDao
import com.creativeblisslabs.omnimind.core.data.database.dao.BookChunkDao
import com.creativeblisslabs.omnimind.core.data.database.dao.BookmarkDao
import com.creativeblisslabs.omnimind.core.data.ai.OnDeviceAiRuntime
import com.creativeblisslabs.omnimind.core.media.MediaProcessor
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import org.koin.core.module.dsl.viewModelOf

val readerModule = module {
    single { BookAnalyzer(get<OmniAiService>()) }
    viewModelOf(::ReaderHubViewModel)
}
