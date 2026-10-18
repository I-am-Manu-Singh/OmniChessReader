package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.creativeblisslabs.omnimind.chess.shared.ChessTournament
import com.creativeblisslabs.omnimind.chess.shared.LichessGame
import com.creativeblisslabs.omnimind.chess.shared.LichessLeaderEntry
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

@Composable
fun TournamentsPanel(tournaments: List<ChessTournament>, onFaqClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var selectedSubTab by remember { mutableStateOf(1) } // 0: Completed, 1: Ongoing, 2: Upcoming

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Arena Tournaments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = scheme.primary)
            IconButton(onClick = onFaqClick) {
                Icon(Icons.Filled.HelpOutline, "FAQ", tint = scheme.primary)
            }
        }

        // Sub-tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Completed", "Ongoing", "Upcoming").forEachIndexed { index, title ->
                val active = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedSubTab = index }
                        .border(
                            width = if (active) 2.dp else 0.dp,
                            color = if (active) scheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) scheme.primary else Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Tournament List filtering
        val filteredList = when (selectedSubTab) {
            0 -> listOf(
                ChessTournament("tc1", "Hourly Bullet Arena", 0, 268, "Completed", emptyList(), true),
                ChessTournament("tc2", "≤1300 Blitz Arena", 0, 44, "Completed", emptyList(), true),
                ChessTournament("tc3", "≤1700 Bullet Arena", 0, 26, "Completed", emptyList(), true)
            )
            1 -> listOf(
                ChessTournament("to1", "Hourly SuperBlitz Arena", 0, 616, "Live", emptyList(), true),
                ChessTournament("to2", "Hourly Blitz Arena", 0, 280, "Live", emptyList(), true),
                ChessTournament("to3", "≤1500 SuperBlitz Arena", 0, 68, "Live", emptyList(), true)
            )
            else -> listOf(
                ChessTournament("tu1", "Hourly UltraBullet Arena", 0, 66, "Upcoming", emptyList(), true),
                ChessTournament("tu2", "≤2000 Rapid Arena", 0, 169, "Upcoming", emptyList(), true),
                ChessTournament("tu3", "Hourly Chess960 Arena", 0, 49, "Upcoming", emptyList(), true)
            )
        }

        filteredList.forEach { tour ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x1F888888),
                border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            tour.title.contains("Bullet") -> Icons.Filled.FlashOn
                            tour.title.contains("Blitz") -> Icons.Filled.LocalFireDepartment
                            tour.title.contains("Rapid") -> Icons.Filled.DirectionsRun
                            else -> Icons.Filled.Grid4x4
                        }
                        val tint = when {
                            tour.title.contains("Bullet") -> Color(0xFF64B5F6)
                            tour.title.contains("Blitz") -> Color(0xFF81C784)
                            tour.title.contains("Rapid") -> Color(0xFFFFB74D)
                            else -> Color(0xFFBA68C8)
                        }
                        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))

                        Column {
                            Text(tour.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val rateStr = when {
                                tour.title.contains("Bullet") -> "1+0 Rated • 27 minutes"
                                tour.title.contains("Blitz") -> "5+0 Rated • 57 minutes"
                                tour.title.contains("Rapid") -> "10+0 Rated • 57 minutes"
                                else -> "5+3 Rated • 57 minutes"
                            }
                            Text(rateStr, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val timeStr = when (selectedSubTab) {
                            0 -> "03:00 - 03:27"
                            1 -> "03:30 - 04:27"
                            else -> "04:30 - 05:27"
                        }
                        Text(timeStr, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.People, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Text(tour.playersCount.toString(), fontSize = 12.sp, color = scheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentsTabContent(
    vm: ChessViewModel,
    onFaqClick: () -> Unit
) {
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    TournamentsPanel(tournaments, onFaqClick = onFaqClick)
}

@Composable
fun LeaderboardSection(
    leaderboard: List<LichessLeaderEntry>,
    vm: ChessViewModel,
    scheme: ColorScheme
) {
    var leaderFilter by remember { mutableStateOf("bullet") }
    var platformSource by remember { mutableStateOf("Lichess") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Lichess", "Chess.com").forEach { platform ->
                    FilterChip(
                        selected = platformSource == platform,
                        onClick = { platformSource = platform },
                        label = { Text(platform, fontSize = 11.sp) }
                    )
                }
            }
            IconButton(onClick = { vm.fetchLichessLeaderboard() }) {
                Icon(Icons.Filled.Refresh, "Refresh", tint = scheme.primary)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("bullet", "blitz", "rapid", "classical").forEach { cat ->
                FilterChip(
                    selected = leaderFilter == cat,
                    onClick = { leaderFilter = cat },
                    label = { Text(cat.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (leaderboard.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = scheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("Fetching $platformSource leaderboards…", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                }
            }
        } else {
            val filtered = leaderboard.filter { it.category.lowercase() == leaderFilter.lowercase() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("# Rank", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
                Text("Player Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("Rating", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scheme.onSurfaceVariant)
            }

            filtered.forEachIndexed { idx, entry ->
                GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (idx) {
                                0 -> Color(0xFFFFD700)
                                1 -> Color(0xFFC0C0C0)
                                2 -> Color(0xFFCD7F32)
                                else -> scheme.onSurfaceVariant
                            },
                            modifier = Modifier.width(36.dp)
                        )

                        entry.title?.let { title ->
                            Text(
                                text = "$title ",
                                fontSize = 12.sp,
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = entry.username,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${entry.rating}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = scheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveStreamPlayerCard(
    gameChannel: String,
    whitePlayer: String,
    blackPlayer: String,
    onWatchClick: () -> Unit
) {
        val scheme = MaterialTheme.colorScheme

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFFE53935), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE STREAM • $gameChannel", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE53935))
                }
                SuggestionChip(onClick = onWatchClick, label = { Text("Watch Live", fontSize = 11.sp) })
            }

            Text(
                text = "$whitePlayer vs $blackPlayer",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = scheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFF1E1E24), RoundedCornerShape(8.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.PlayCircle, null, tint = Color(0xFFE8883A), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to connect stream & move feed", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

class ChessSocketService(private val httpClient: HttpClient) {
    suspend fun streamGameMoves(gameId: String, onMoveReceived: (String) -> Unit) {
        try {
            httpClient.prepareGet("https://lichess.org/api/board/game/stream/$gameId") {
                header("Accept", "application/x-ndjson")
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isNotBlank()) {
                        onMoveReceived(line)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun streamLichessTvChannels(onDataUpdate: (String) -> Unit) {
        try {
            httpClient.prepareGet("https://lichess.org/api/tv/channels").execute { response ->
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isNotBlank()) {
                        onDataUpdate(line)
                    }
                }
            }
        } catch (_: Exception) {}
    }
}