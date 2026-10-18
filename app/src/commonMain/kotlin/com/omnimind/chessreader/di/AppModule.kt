package com.creativeblisslabs.omnichessreader.di

import com.creativeblisslabs.omnimind.core.data.ai.*
import com.creativeblisslabs.omnimind.core.data.database.OmniMindDatabase
import com.creativeblisslabs.omnimind.core.data.database.getDatabaseBuilder
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
// import com.creativeblisslabs.omnimind.core.data.repository.*
import com.creativeblisslabs.omnimind.core.media.createMediaProcessor
import com.creativeblisslabs.omnimind.feature.chess.ChessViewModel
import com.creativeblisslabs.omnimind.feature.chess.di.platformChessModule
import com.creativeblisslabs.omnimind.feature.reader.di.readerModule
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
    single { get<OmniMindDatabase>().bookDao() }
    single { get<OmniMindDatabase>().bookmarkDao() }
    single { get<OmniMindDatabase>().bookChunkDao() }
}

val repositoryModule = module {
}

val preferencesModule = module {
    single { com.creativeblisslabs.omnimind.core.data.preferences.createDataStore() }
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
