package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.creativeblisslabs.omnimind.core.chess.EngineEvaluation
import com.creativeblisslabs.omnimind.core.chess.MoveAnalysis

/**
 * Consolidated Chess Analysis Side Panel.
 * Delegates all 18 attributes directly to the unified Analysis Panel in ChessAnalysisScreen.kt.
 */
@Composable
fun ChessAnalysisSidePanel(
    moveHistory: List<String>,
    moveAnalysis: List<MoveAnalysis> = emptyList(),
    currentMoveIndex: Int = -1,
    evaluation: EngineEvaluation? = null,
    openingName: String = "",
    whitePlayer: String = "",
    whiteRating: Int? = null,
    blackPlayer: String = "",
    blackRating: Int? = null,
    gameResult: String = "",
    onSelectMove: (Int) -> Unit = {},
    onFirstMove: () -> Unit = {},
    onPrevMove: () -> Unit = {},
    onNextMove: () -> Unit = {},
    onLastMove: () -> Unit = {},
    onOpenReviewScreen: () -> Unit = {},
    onSaveGame: () -> Unit = {},
    onNewGame: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("Analysis") }
    var isAnalysisEnabled by remember { mutableStateOf(true) }
    val scheme = MaterialTheme.colorScheme

    val whiteLabel = buildString {
        append(if (whitePlayer.isNotBlank()) whitePlayer else "White")
        if (whiteRating != null && whiteRating > 0) append(" ($whiteRating)")
    }
    val blackLabel = buildString {
        append(if (blackPlayer.ifBlank { "Black" } != "") blackPlayer else "Black")
        if (blackRating != null && blackRating > 0) append(" ($blackRating)")
        if (gameResult.isNotBlank()) append(" $gameResult")
    }

    AnalysisPanelContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        isAnalysisEnabled = isAnalysisEnabled,
        onAnalysisToggle = { isAnalysisEnabled = it },
        evaluation = evaluation,
        currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
        openingName = openingName,
        whiteLabel = whiteLabel,
        blackLabel = blackLabel,
        moveHistory = moveHistory,
        moveAnalysis = moveAnalysis,
        currentMoveIndex = currentMoveIndex,
        vm = null,
        practiceBannerMessage = null,
        onDismissPracticeBanner = {},
        onShowReview = onOpenReviewScreen,
        scheme = scheme
    )
}
