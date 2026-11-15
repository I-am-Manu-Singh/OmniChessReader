package com.creativeblisslabs.omnichessreader.feature.chess.di

import com.creativeblisslabs.omnichessreader.core.chess.ChessEngine
import com.creativeblisslabs.omnichessreader.core.chess.EngineVersion
import com.creativeblisslabs.omnichessreader.chess.engine.StockfishEngine
import com.creativeblisslabs.omnichessreader.chess.shared.OpeningRepository
import com.creativeblisslabs.omnichessreader.chess.shared.logic.AndroidOpeningRepository
import com.creativeblisslabs.omnichessreader.chess.shared.logic.AssetManager
import com.creativeblisslabs.omnichessreader.chess.shared.logic.ChessAssetLoader
import com.creativeblisslabs.omnichessreader.feature.chess.network.ChessSocketService
import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformChessModule(): Module = module {
    single<ChessEngine> { StockfishEngine(get(), get()) }
    single<OpeningRepository> { AndroidOpeningRepository(get()) }
    single { AssetManager(get()) }
    single { ChessAssetLoader(get()) }
    single { ChessSocketService(get()) }
}
