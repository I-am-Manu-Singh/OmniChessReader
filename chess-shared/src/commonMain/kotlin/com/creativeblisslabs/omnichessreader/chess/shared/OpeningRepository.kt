package com.creativeblisslabs.omnichessreader.chess.shared

import com.creativeblisslabs.omnichessreader.chess.shared.OpeningPosition

interface OpeningRepository {
    suspend fun getOpeningByFen(fen: String): OpeningPosition?
    suspend fun getOpeningByEco(eco: String): OpeningPosition?
    suspend fun searchOpenings(query: String): List<OpeningPosition>
}
