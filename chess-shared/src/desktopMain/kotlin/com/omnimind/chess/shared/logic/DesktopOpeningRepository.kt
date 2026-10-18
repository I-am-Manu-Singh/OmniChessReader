package com.creativeblisslabs.omnimind.chess.shared.logic

import com.creativeblisslabs.omnimind.chess.shared.OpeningPosition
import com.creativeblisslabs.omnimind.chess.shared.OpeningRepository

class DesktopOpeningRepository : OpeningRepository {
    override suspend fun getOpeningByFen(fen: String): OpeningPosition? = null
    override suspend fun getOpeningByEco(eco: String): OpeningPosition? = null
    override suspend fun searchOpenings(query: String): List<OpeningPosition> = emptyList()
}
