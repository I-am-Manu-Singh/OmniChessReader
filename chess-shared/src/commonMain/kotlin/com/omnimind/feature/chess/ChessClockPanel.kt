package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChessClockPanel(
    p1TimeLeft: Int,
    p2TimeLeft: Int,
    activeClockPlayer: Int,
    isClockSoundMuted: Boolean,
    onPlayer1Tap: () -> Unit,
    onPlayer2Tap: () -> Unit,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onSettings: () -> Unit,
    onMuteToggle: () -> Unit,
    onFlip: () -> Unit = {}
) {
        // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
    val formatTime = { sec: Int ->
        val safeSec = sec.coerceAtLeast(0)
        val m = safeSec / 60
        val s = safeSec % 60
        "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }

    val p1Active = activeClockPlayer == 1
    val p2Active = activeClockPlayer == 2
    val isPaused = activeClockPlayer == 0

    val scheme = MaterialTheme.colorScheme

    val p1Color = when {
        p1TimeLeft < 10 -> Color(0xFFEB5757)
        p1TimeLeft < 30 -> Color(0xFFF2994A)
        p1Active -> scheme.onSurface
        else -> Color(0x99FFFFFF)
    }
    val p2Color = when {
        p2TimeLeft < 10 -> Color(0xFFEB5757)
        p2TimeLeft < 30 -> Color(0xFFF2994A)
        p2Active -> scheme.onSurface
        else -> Color(0x99FFFFFF)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight
        val scheme = MaterialTheme.colorScheme

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background)
            ) {
                // ── Left Column: Player 1 Clock Tile ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (p1Active) scheme.primaryContainer.copy(alpha = 0.25f) else scheme.surfaceContainerHigh)
                        .clickable { onPlayer1Tap() }
                        .graphicsLayer(rotationZ = 180f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = formatTime(p1TimeLeft), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = p1Color, letterSpacing = (-2).sp)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (p1Active) "▶ Your turn" else if (isPaused) "Paused" else "Waiting...", fontSize = 14.sp, color = if (p1Active) scheme.primary else scheme.onSurfaceVariant)
                    }
                }

                // Center Control Strip
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(60.dp)
                        .background(scheme.surfaceContainerHighest)
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = onPlayPause) { Icon(if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, null, tint = scheme.primary) }
                    IconButton(onClick = onReset) { Icon(Icons.Filled.Refresh, null, tint = scheme.onSurface) }
                    IconButton(onClick = onMuteToggle) { Icon(if (isClockSoundMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, tint = scheme.onSurface) }
                }

                // ── Right Column: Player 2 Clock Tile ──
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (p2Active) scheme.primaryContainer.copy(alpha = 0.25f) else scheme.surfaceContainerHigh)
                        .clickable { onPlayer2Tap() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = formatTime(p2TimeLeft), fontSize = 64.sp, fontWeight = FontWeight.Bold, color = p2Color, letterSpacing = (-2).sp)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (p2Active) "▶ Your turn" else if (isPaused) "Paused" else "Waiting...", fontSize = 14.sp, color = if (p2Active) scheme.primary else scheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0C))
            ) {
        // ── P1 Panel (opponent facing top half) ───────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (p1Active) Color(0xFF1E1E22) else Color(0xFF111113))
                .clickable { onPlayer1Tap() }
                .graphicsLayer(rotationZ = 180f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTime(p1TimeLeft),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = p1Color,
                    letterSpacing = (-2).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (p1Active) "▶ Your turn" else if (isPaused) "Paused" else "Waiting...",
                    fontSize = 14.sp,
                    color = if (p1Active) Color(0xFFE8883A) else Color(0x66FFFFFF)
                )
            }

            if (p1TimeLeft < 10 && p1Active) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x1AEB5757))
                )
            }
        }

        // ── Center Controls Bar ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D0F))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = "Play/Pause",
                    tint = scheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }

            IconButton(onClick = onReset, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Replay, "Reset", tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            }

            IconButton(onClick = onSettings, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.Settings, "Settings", tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            }

            IconButton(onClick = onMuteToggle, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (isClockSoundMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (isClockSoundMuted) Color(0xFFEB5757) else scheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(onClick = onFlip, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Filled.SwapVert, "Flip", tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }

        // ── P2 Panel (player at bottom half) ──────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(if (p2Active) Color(0xFF1E1E22) else Color(0xFF111113))
                .clickable { onPlayer2Tap() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTime(p2TimeLeft),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = p2Color,
                    letterSpacing = (-2).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (p2Active) "▶ Your turn" else if (isPaused) "Paused" else "Waiting...",
                    fontSize = 14.sp,
                    color = if (p2Active) Color(0xFFE8883A) else Color(0x66FFFFFF)
                )
            }

            if (p2TimeLeft < 10 && p2Active) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x1AEB5757))
                )
            }
        }
    }
}
}
}
