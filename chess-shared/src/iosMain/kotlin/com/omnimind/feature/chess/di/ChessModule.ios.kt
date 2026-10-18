package com.creativeblisslabs.omnimind.feature.chess.di

import com.creativeblisslabs.omnimind.chess.engine.ChessEngine
import com.creativeblisslabs.omnimind.chess.engine.IosChessEngine
import com.creativeblisslabs.omnimind.chess.shared.OpeningRepository
import com.creativeblisslabs.omnimind.chess.shared.logic.AssetManager
import com.creativeblisslabs.omnimind.chess.shared.logic.ChessAssetLoader
import com.creativeblisslabs.omnimind.chess.shared.logic.IosOpeningRepository
import com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketService
import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformChessModule(): Module = module {
    single<ChessEngine> { IosChessEngine() }
    single { AssetManager() }
    single { ChessAssetLoader(get()) }
    single<OpeningRepository> { IosOpeningRepository() }
    single { ChessSocketService(get()) }
}
