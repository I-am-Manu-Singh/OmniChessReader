package com.creativeblisslabs.omnichessreader.feature.chess.di

import com.creativeblisslabs.omnichessreader.chess.engine.ChessEngine
import com.creativeblisslabs.omnichessreader.chess.engine.DesktopChessEngine
import com.creativeblisslabs.omnichessreader.chess.shared.OpeningRepository
import com.creativeblisslabs.omnichessreader.chess.shared.logic.AssetManager
import com.creativeblisslabs.omnichessreader.chess.shared.logic.ChessAssetLoader
import com.creativeblisslabs.omnichessreader.chess.shared.logic.DesktopOpeningRepository
import com.creativeblisslabs.omnichessreader.feature.chess.network.ChessSocketService
import org.koin.dsl.module
import org.koin.core.module.Module

actual fun platformChessModule(): Module = module {
    single<ChessEngine> { DesktopChessEngine() }
    single { AssetManager() }
    single { ChessAssetLoader(get()) }
    single<OpeningRepository> { DesktopOpeningRepository() }
    single { ChessSocketService(get()) }
}
