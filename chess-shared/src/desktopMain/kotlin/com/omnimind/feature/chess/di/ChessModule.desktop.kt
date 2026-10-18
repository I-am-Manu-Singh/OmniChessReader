package com.creativeblisslabs.omnimind.feature.chess.di

import com.creativeblisslabs.omnimind.chess.engine.ChessEngine
import com.creativeblisslabs.omnimind.chess.engine.DesktopChessEngine
import com.creativeblisslabs.omnimind.chess.shared.OpeningRepository
import com.creativeblisslabs.omnimind.chess.shared.logic.AssetManager
import com.creativeblisslabs.omnimind.chess.shared.logic.ChessAssetLoader
import com.creativeblisslabs.omnimind.chess.shared.logic.DesktopOpeningRepository
import com.creativeblisslabs.omnimind.feature.chess.network.ChessSocketService
import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformChessModule(): Module = module {
    single<ChessEngine> { DesktopChessEngine() }
    single { AssetManager() }
    single { ChessAssetLoader(get()) }
    single<OpeningRepository> { DesktopOpeningRepository() }
    single { ChessSocketService(get()) }
}
