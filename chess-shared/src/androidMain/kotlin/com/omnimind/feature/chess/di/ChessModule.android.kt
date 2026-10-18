package com.creativeblisslabs.omnimind.feature.chess.di

import com.creativeblisslabs.omnimind.core.chess.ChessEngine
import com.creativeblisslabs.omnimind.core.chess.EngineVersion
import com.creativeblisslabs.omnimind.chess.engine.StockfishEngine
import com.creativeblisslabs.omnimind.chess.shared.OpeningRepository
import com.creativeblisslabs.omnimind.chess.shared.logic.AndroidOpeningRepository
import com.creativeblisslabs.omnimind.chess.shared.logic.AssetManager
import com.creativeblisslabs.omnimind.chess.shared.logic.ChessAssetLoader
import com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketService
import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformChessModule(): Module = module {
    single<ChessEngine> { StockfishEngine(get(), get()) }
    single<OpeningRepository> { AndroidOpeningRepository(get()) }
    single { AssetManager(get()) }
    single { ChessAssetLoader(get()) }
    single { ChessSocketService(get()) }
}
