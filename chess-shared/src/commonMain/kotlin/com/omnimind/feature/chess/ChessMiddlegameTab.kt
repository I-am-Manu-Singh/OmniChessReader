package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard

data class MiddlegameTopic(
    val title: String,
    val category: String,
    val description: String,
    val fen: String,
    val keyMoves: List<String>,
    val explanation: String
)

val defaultMiddlegameTopics = listOf(
    MiddlegameTopic(
        title = "Outposts & Knight Holes",
        category = "Positional Control",
        description = "An outpost is a square protected by your pawn that cannot be attacked by enemy pawns. Knights thrive on outposts, especially on the 5th or 6th rank.",
        fen = "r1bq1rk1/pp2ppbp/2np1np1/8/3NP3/2N1BP2/PPPQ2PP/R3KB1R w KQ - 3 9",
        keyMoves = listOf("d4c6", "b2c6", "e3d4"),
        explanation = "White establishes central control and target weaknesses in Black's pawn structure."
    ),
    MiddlegameTopic(
        title = "Open Files & Rook Batteries",
        category = "Piece Coordination",
        description = "Rooks require open files (columns with no pawns) to project influence across the enemy territory. Doubling rooks on an open file creates immense pressure.",
        fen = "2r2rk1/pp1qbppp/4pn2/3p4/3P4/2N1PN2/PP3PPP/R2Q1RK1 w - - 0 12",
        keyMoves = listOf("a1c1", "f8c8", "d1b3"),
        explanation = "Both sides contest the open c-file. Controlling file entry points is key to middlegame invasion."
    ),
    MiddlegameTopic(
        title = "King Safety & Greek Gift",
        category = "Attacking Play",
        description = "The classic bishop sacrifice on h7 tears open the enemy king shelter when white has strong knight and queen backing.",
        fen = "r1b2rk1/ppq1bppp/2n1pn2/2pp2B1/3PP3/2PB1N2/PP1N1PPP/R2Q1RK1 w - - 0 9",
        keyMoves = listOf("e4e5", "f6d7", "d3h7"),
        explanation = "Bxh7+ followed by Ng5+ and Qh5 leads to a decisive mating attack."
    ),
    MiddlegameTopic(
        title = "Opposite Side Castling Storm",
        category = "Pawn Assault",
        description = "When kings castle on opposite flanks, launching a pawn storm toward the enemy king generates open files for your heavy pieces.",
        fen = "2kr1b1r/pppn1ppp/q3pn2/8/3P2b1/2N1BN2/PPP1BPPP/R2Q1RK1 w - - 0 10",
        keyMoves = listOf("h2h3", "g4f5", "g2g4"),
        explanation = "Speed is crucial in opposite-side castling races."
    ),
    MiddlegameTopic(
        title = "Pawn Chains & Base Targets",
        category = "Strategic Planning",
        description = "Pawn chains dictate space and piece movement. Always attack the base of a pawn chain (the rearmost pawn) to undermine the entire structure.",
        fen = "r1bqk2r/pp2bppp/2n1pn2/2ppP3/3P4/2P2N2/PP1N1PPP/R1BQKB1R w KQkq - 1 7",
        keyMoves = listOf("e5f6", "e7f6", "d4c5"),
        explanation = "Challenging the central apex or base frees active piece play."
    ),
    MiddlegameTopic(
        title = "Minority Attack",
        category = "Strategic Planning",
        description = "Advancing two pawns against three on the queenside creates backward pawns and isolated weak targets.",
        fen = "r1bq1rk1/pp3ppp/2n1pn2/2pp4/3P4/2PBPN2/PP1N1PPP/R2Q1RK1 w - - 0 10",
        keyMoves = listOf("b2b4", "c5b4", "c3b4"),
        explanation = "The minority attack creates structural weaknesses in the opponent's camp."
    ),
    MiddlegameTopic(
        title = "Bishop Pair Dominance",
        category = "Piece Advantage",
        description = "Having both bishops in an open position allows long-range slicing control across diagonal diagonals.",
        fen = "r4rk1/pp1b1ppp/2n1pn2/2qp4/8/2PBPN2/PP1N1PPP/R2Q1RK1 w - - 0 11",
        keyMoves = listOf("e3e4", "d5e4", "d2e4"),
        explanation = "Opening the board maximizes the scope of the bishop pair."
    ),
    MiddlegameTopic(
        title = "Isolated Queen Pawn (IQP)",
        category = "Central Dynamics",
        description = "An isolated d-pawn offers piece activity and space in exchange for long-term weakness if blocked.",
        fen = "r2q1rk1/pp1nbppp/4pn2/3p4/3P4/2N1PN2/PP3PPP/R1BQ1RK1 w - - 0 10",
        keyMoves = listOf("d4d5", "e6d5", "c3d5"),
        explanation = "The player with the IQP must attack before endgames are reached."
    )
)

@Composable
fun ChessMiddlegameTab(
    vm: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val currentFen by vm.fen.collectAsStateWithLifecycle()
    val theme by vm.selectedTheme.collectAsStateWithLifecycle()
    val pieceStyle by vm.pieceStyle.collectAsStateWithLifecycle()
    val isPlayerWhite by vm.isPlayerWhite.collectAsStateWithLifecycle()
    val showThreatMap by vm.showThreatMap.collectAsStateWithLifecycle()
    val threatMap by vm.threatMap.collectAsStateWithLifecycle()
    val visualArrows by vm.visualArrows.collectAsStateWithLifecycle()
    val evaluation by vm.evaluation.collectAsStateWithLifecycle()
    val moveHistory by vm.moveHistory.collectAsStateWithLifecycle()

    var selectedSquare by remember { mutableStateOf<String?>(null) }

    var activeTopicIndex by remember { mutableIntStateOf(0) }
    val topic = defaultMiddlegameTopics.getOrNull(activeTopicIndex) ?: defaultMiddlegameTopics.first()

    // Load the current topic's FEN on entry (resets carryover from Game tab)
    LaunchedEffect(Unit) {
        vm.updateFen(topic.fen)
    }
    // Reload FEN whenever the topic changes
    LaunchedEffect(activeTopicIndex) {
        vm.updateFen(topic.fen)
        selectedSquare = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Middlegame Masterclass",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )
                Text(
                    text = topic.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )

                // Topic Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    defaultMiddlegameTopics.forEachIndexed { idx, t ->
                        FilterChip(
                            selected = idx == activeTopicIndex,
                            onClick = {
                                activeTopicIndex = idx
                                vm.updateFen(t.fen)
                            },
                            label = { Text(t.title, fontSize = 11.sp) }
                        )
                    }
                }

                // Interactive Board
                ChessBoardContainer(
                    fen = currentFen,
                    theme = theme,
                    pieceStyle = pieceStyle,
                    isPlayerWhite = isPlayerWhite,
                    selectedSquare = selectedSquare,
                    premove = null,
                    showThreatMap = showThreatMap,
                    threatMap = threatMap,
                    visualArrows = visualArrows,
                    engineEvaluation = evaluation,
                    moveHistory = moveHistory,
                    onSquareClick = { clicked ->
                        val cur = selectedSquare
                        if (cur == null) {
                            selectedSquare = clicked
                        } else {
                            if (cur != clicked) {
                                vm.makeMove(cur, clicked)
                            }
                            selectedSquare = null
                        }
                    }
                )

                // Navigation Bar
                ReusableChessControlBar(
                    moveHistory = moveHistory,
                    currentMoveIndex = (moveHistory.size - 1).coerceAtLeast(0),
                    onSelectMove = { vm.goToMove(it) },
                    onFirstMove = { vm.goToMove(0) },
                    onPrevMove = { vm.undoLastMove() },
                    onNextMove = { vm.redoNextMove() },
                    onLastMove = { vm.goToMove((moveHistory.size - 1).coerceAtLeast(0)) },
                    onFlipBoard = { vm.setPlayerColor(!isPlayerWhite) },
                    onResetBoard = { vm.updateFen(topic.fen) }
                )

                Surface(
                    color = scheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Strategic Insight:", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 12.sp)
                        Text(topic.explanation, color = scheme.onSurface, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
