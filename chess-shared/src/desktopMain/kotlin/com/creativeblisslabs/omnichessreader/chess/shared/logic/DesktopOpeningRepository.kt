package com.creativeblisslabs.omnichessreader.chess.shared.logic

import com.creativeblisslabs.omnichessreader.chess.shared.OpeningPosition
import com.creativeblisslabs.omnichessreader.chess.shared.OpeningRepository

class DesktopOpeningRepository : OpeningRepository {
    override suspend fun getOpeningByFen(fen: String): OpeningPosition? = null
    override suspend fun getOpeningByEco(eco: String): OpeningPosition? = null
    override suspend fun searchOpenings(query: String): List<OpeningPosition> = emptyList()
}
