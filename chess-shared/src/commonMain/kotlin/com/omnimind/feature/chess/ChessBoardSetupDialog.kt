package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.creativeblisslabs.omnimind.core.chess.ChessLogic

@Composable
fun ChessBoardSetupDialog(
    initialFen: String,
    pieceStyle: String = "cburnett",
    onApplyFen: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val boardState = remember {
        mutableStateMapOf<String, Char>().apply {
            putAll(ChessLogic.parseFen(initialFen).pieces)
        }
    }
    var activePiece by remember { mutableStateOf<Char?>('P') }
    var activeTurn by remember { mutableStateOf("w") }
    val scheme = MaterialTheme.colorScheme

    fun buildFen(): String {
        val fenRows = mutableListOf<String>()
        for (r in 7 downTo 0) {
            var emptyCount = 0
            val rowStr = StringBuilder()
            for (c in 0..7) {
                val sq = "${('a' + c)}${r + 1}"
                val piece = boardState[sq]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        rowStr.append(emptyCount)
                        emptyCount = 0
                    }
                    rowStr.append(piece)
                }
            }
            if (emptyCount > 0) rowStr.append(emptyCount)
            fenRows.add(rowStr.toString())
        }
        return "${fenRows.joinToString("/")} $activeTurn KQkq - 0 1"
    }

    val currentSetupFen = buildFen()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Visual Board Setup", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface, fontWeight = FontWeight.Bold)

                // Piece Palette
                val whitePieces = listOf('K', 'Q', 'R', 'B', 'N', 'P')
                val blackPieces = listOf('k', 'q', 'r', 'b', 'n', 'p')

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Piece to Place:", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        whitePieces.forEach { p ->
                            val sel = activePiece == p
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (sel) Color(0xFFE8883A) else Color(0xFF2B2B30), RoundedCornerShape(6.dp))
                                    .clickable { activePiece = p },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p.toString(), color = scheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        blackPieces.forEach { p ->
                            val sel = activePiece == p
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (sel) Color(0xFFE8883A) else Color(0xFF2B2B30), RoundedCornerShape(6.dp))
                                    .clickable { activePiece = p },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p.toString(), color = Color.DarkGray, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { boardState.clear() }) {
                        Text("Clear Board", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        boardState.clear()
                        boardState.putAll(ChessLogic.parseFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1").pieces)
                    }) {
                        Text("Reset Initial", fontSize = 10.sp, color = scheme.onSurfaceVariant)
                    }
                }

                // 8x8 Grid
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Gray, RoundedCornerShape(8.dp))
                ) {
                    Column {
                        for (r in 7 downTo 0) {
                            Row(modifier = Modifier.weight(1f)) {
                                for (c in 0..7) {
                                    val sq = "${('a' + c)}${r + 1}"
                                    val isLight = (c + r) % 2 == 1
                                    val piece = boardState[sq]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(if (isLight) Color(0xFFEEEED2) else Color(0xFF769656))
                                            .clickable {
                                                if (activePiece != null) {
                                                    boardState[sq] = activePiece!!
                                                } else {
                                                    boardState.remove(sq)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (piece != null) {
                                            Text(
                                                text = piece.toString(),
                                                color = if (piece.isUpperCase()) scheme.onSurface else scheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
    val scheme = MaterialTheme.colorScheme

                Text("FEN: $currentSetupFen", fontSize = 10.sp, color = scheme.onSurfaceVariant, maxLines = 2)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = scheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onApplyFen(currentSetupFen)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8883A))
                    ) {
                        Text("Apply to Board", color = scheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
