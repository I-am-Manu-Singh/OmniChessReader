package com.creativeblisslabs.omnimind.feature.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.omnimind.core.chess.MoveQuality

@Composable
fun MoveQualityIndicator(quality: MoveQuality) {
    val (color, symbol) = when (quality) {
        MoveQuality.BRILLIANT -> Color(0xFF26C6DA) to "!!"
        MoveQuality.GREAT -> Color(0xFF00ACC1) to "!"
        MoveQuality.BEST -> Color(0xFF9CCC65) to "★"
        MoveQuality.EXCELLENT -> Color(0xFF81C784) to "✓"
        MoveQuality.GOOD -> Color(0xFFAED581) to "✓"
        MoveQuality.BOOK -> Color(0xFFD4E157) to "📖"
        MoveQuality.INACCURACY -> Color(0xFFFFF176) to "?!"
        MoveQuality.MISTAKE -> Color(0xFFFFB74D) to "?"
        MoveQuality.BLUNDER -> Color(0xFFE57373) to "??"
        MoveQuality.FORCED -> Color(0xFFBDBDBD) to "□"
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EvaluationGraph(
    evals: List<Double>,
    selectedIndex: Int = -1,
    onSelectMove: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (evals.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(Color(0xFF161618), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        evals.forEachIndexed { index, eval ->
            val isSelected = index == selectedIndex
            val heightFactor = (eval.coerceIn(-5.0, 5.0) + 5.0) / 10.0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFactor.toFloat().coerceIn(0.05f, 1.0f))
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            isSelected -> Color(0xFFE8883A)
                            eval >= 0 -> MaterialTheme.colorScheme.onSurface
                            else -> Color(0xFF444448)
                        }
                    )
                    .clickable { onSelectMove?.invoke(index) }
            )
        }
    }
}
