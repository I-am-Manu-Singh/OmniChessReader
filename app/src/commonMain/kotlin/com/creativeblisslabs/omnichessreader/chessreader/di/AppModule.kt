package com.creativeblisslabs.omnichessreader.di

import com.creativeblisslabs.omnichessreader.core.data.ai.*
import com.creativeblisslabs.omnichessreader.core.data.database.OmniChessReaderDatabase
import com.creativeblisslabs.omnichessreader.core.data.database.getDatabaseBuilder
import com.creativeblisslabs.omnichessreader.core.data.preferences.AppPreferences
// import com.creativeblisslabs.omnichessreader.core.data.repository.*
import com.creativeblisslabs.omnichessreader.core.media.createMediaProcessor
import com.creativeblisslabs.omnichessreader.feature.chess.ChessViewModel
import com.creativeblisslabs.omnichessreader.feature.chess.di.platformChessModule
import com.creativeblisslabs.omnichessreader.feature.reader.di.readerModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }
}

val databaseModule = module {
    single { getDatabaseBuilder() }
    single { get<OmniChessReaderDatabase>().bookDao() }
    single { get<OmniChessReaderDatabase>().bookmarkDao() }
    single { get<OmniChessReaderDatabase>().bookChunkDao() }
}

val repositoryModule = module {
}

val preferencesModule = module {
    single { com.creativeblisslabs.omnichessreader.core.data.preferences.createDataStore() }
    single { AppPreferences(get()) }
}

val aiModule = module {
    includes(platformChessModule())
    single { createHttpClient() }
    single { GeminiService(get()) }
    single { createOnDeviceAiRuntime() }
    single { createModelPackManager() }
    single { createMediaProcessor() }
    single { OmniAiService(get(), get(), get(), get()) }
}

val chessReaderViewModelModule = module {
    viewModelOf(::ChessViewModel)
}

val appModule = listOf(
    databaseModule,
    repositoryModule,
    aiModule,
    preferencesModule,
    chessReaderViewModelModule,
    readerModule
)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(appModule)
    }
