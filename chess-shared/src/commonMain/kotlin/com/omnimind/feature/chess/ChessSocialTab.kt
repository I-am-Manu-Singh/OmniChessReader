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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.chess.shared.*

@Composable
fun SocialTabContent(
    vm: ChessViewModel,
    onAddFriend: () -> Unit,
    onPlayerClick: (String) -> Unit
) {
    val friends by vm.friends.collectAsStateWithLifecycle()
    val lichessLeaderboard by vm.lichessLeaderboard.collectAsStateWithLifecycle()
    val isLichessLoading by vm.isLichessLoading.collectAsStateWithLifecycle()
    val lichessLiveGames by vm.lichessLiveGames.collectAsStateWithLifecycle()
    val lichessStreamEvents by vm.lichessStreamEvents.collectAsStateWithLifecycle()
    val lichessFetchError by vm.lichessFetchError.collectAsStateWithLifecycle()
  var showFriendDialog by remember { mutableStateOf(false) }
    var friendInput by remember { mutableStateOf("") }
        var showArenaFaqSheet by remember { mutableStateOf(false) }

    // User Profile / Platform search states
    var inputUsername by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("Lichess") }
    val userProfile by vm.userProfile.collectAsStateWithLifecycle()
    val chessComProfile by vm.chessComProfile.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    var socialTab by remember { mutableStateOf("Stats") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Navigation Rail & Quick Action Cards
                Column(
                    modifier = Modifier.width(140.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NavigationRail(
                        modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)),
                        containerColor = scheme.surfaceContainerHigh.copy(alpha = 0.5f)
                    ) {
                        val tabs = listOf(
                            "Stats" to Icons.Filled.QueryStats,
                            "Friends" to Icons.Filled.People,
                            "Leaderboard" to Icons.Filled.EmojiEvents,
                            "Live TV" to Icons.Filled.Tv,
                            "Events" to Icons.Filled.Event
                        )
                        tabs.forEach { (title, icon) ->
                            NavigationRailItem(
                                selected = socialTab == title,
                                onClick = { socialTab = title },
                                icon = { Icon(icon, null) },
                                label = { Text(title, fontSize = 10.sp) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = scheme.onPrimary,
                                    selectedTextColor = scheme.primary,
                                    indicatorColor = scheme.primary
                                )
                            )
                        }
                    }

                    if (socialTab == "Friends") {
                        Button(
                            onClick = onAddFriend,
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Right Column: Full Interactive Social & Leaderboard Main Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isLichessLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                    lichessFetchError?.let { err ->
                        Text(err, color = scheme.error, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }

                    SocialTabMainContent(
                        socialTab = socialTab,
                        vm = vm,
                        inputUsername = inputUsername,
                        onInputUsernameChange = { inputUsername = it },
                        selectedPlatform = selectedPlatform,
                        friends = friends,
                        onAddFriend = onAddFriend,
                        onPlayerClick = onPlayerClick,
                        lichessLeaderboard = lichessLeaderboard,
                        scheme = scheme,
                        uriHandler = uriHandler
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

        // Sub-tabs for Social section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x1F888888), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Stats", "Friends", "Leaderboard", "Live TV", "Events").forEach { t ->
                val isActive = socialTab == t
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) scheme.primary else Color.Transparent)
                        .clickable { socialTab = t }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = if (isActive) scheme.onPrimary else scheme.onSurfaceVariant
                    )
                }
            }
        }
        
    // Friend Dialog
    if (showFriendDialog) {
        AlertDialog(
            onDismissRequest = { showFriendDialog = false },
            title = { Text("Add Friend") },
            text = {
                OutlinedTextField(
                    value = friendInput,
                    onValueChange = { friendInput = it },
                    label = { Text("Lichess.com / Chess.com / Username") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (friendInput.isNotBlank()) {
                        vm.addFriend(friendInput)
                        friendInput = ""
                    }
                    showFriendDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

   



    // Arena FAQ Bottom Sheet (Screenshot 2)
    if (showArenaFaqSheet) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showArenaFaqSheet = false },
            containerColor = Color(0xFF141414),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF333333)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Arena Tournament FAQ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Is it rated?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = scheme.secondary
                    )
                    Text(
                        "Some tournaments are rated and will affect your rating profile directly.",
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "How are scores calculated?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = scheme.secondary
                    )
                    Text(
                        "A win has a base score of 2 points, a draw 1 point, and a loss is worth no points.\nIf you win two games consecutively, you start a double-point streak, represented by a flame icon.",
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Arena Berserk Rule",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = scheme.secondary
                    )
                    Text(
                        "When a player clicks the Berserk button at the beginning of the game, they lose half of their clock time, but the win is worth one extra tournament point.\nGoing Berserk in time controls with an increment also cancels the increment.",
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "How is the winner decided?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = scheme.secondary
                    )
                    Text(
                        "The player(s) with the most points after the tournament's set time limit will be announced the winner(s).",
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }
    }

        // Loading / error banner
        if (isLichessLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        lichessFetchError?.let { err ->
            Text(
                err, color = scheme.error, fontSize = 11.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        SocialTabMainContent(
            socialTab = socialTab,
            vm = vm,
            inputUsername = inputUsername,
            onInputUsernameChange = { inputUsername = it },
            selectedPlatform = selectedPlatform,
            friends = friends,
            onAddFriend = onAddFriend,
            onPlayerClick = onPlayerClick,
            lichessLeaderboard = lichessLeaderboard,
            scheme = scheme,
            uriHandler = uriHandler
        )
    }
}
}
}
@Composable
private fun SocialTabMainContent(
    socialTab: String,
    vm: ChessViewModel,
    inputUsername: String,
    onInputUsernameChange: (String) -> Unit,
    selectedPlatform: String,
    friends: List<ChessFriend>,
    onAddFriend: () -> Unit,
    onPlayerClick: (String) -> Unit,
    lichessLeaderboard: List<LichessLeaderEntry>,
    scheme: ColorScheme,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val userProfile by vm.userProfile.collectAsStateWithLifecycle()
    val chessComProfile by vm.chessComProfile.collectAsStateWithLifecycle()
    val lichessLiveGames by vm.lichessLiveGames.collectAsStateWithLifecycle()
    val lichessStreamEvents by vm.lichessStreamEvents.collectAsStateWithLifecycle()

    var selectedProfilePlatform by remember { mutableStateOf("Lichess") }

    when (socialTab) {
        "Stats" -> {
            val activeUser = if (selectedProfilePlatform == "Lichess") userProfile else chessComProfile

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isLandscape = maxWidth > 600.dp

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Username Input, Platform Switcher, Account Summary
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = inputUsername,
                                        onValueChange = onInputUsernameChange,
                                        label = { Text("Username", color = scheme.onSurfaceVariant) },
                                        placeholder = { Text("e.g. magnuson-carlsium", color = scheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = scheme.onSurface,
                                            unfocusedTextColor = scheme.onSurface,
                                            focusedBorderColor = scheme.primary,
                                            unfocusedBorderColor = scheme.outlineVariant
                                        ),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(scheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf("Lichess", "Chess.com").forEach { platform ->
                                            val isActive = selectedProfilePlatform == platform
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isActive) scheme.primary else Color.Transparent)
                                                    .clickable { selectedProfilePlatform = platform }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(platform, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isActive) scheme.onPrimary else scheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val u = inputUsername.ifBlank { "magnuson-carlsium" }
                                                vm.fetchDualPlatformProfile(u, "Both")
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                                        ) {
                                            Text("Fetch Ratings", color = scheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val u = activeUser.username.ifBlank { inputUsername.ifBlank { "magnuson-carlsium" } }
                                                val url = if (selectedProfilePlatform == "Lichess") "https://lichess.org/@/$u" else "https://www.chess.com/member/$u"
                                                uriHandler.openUri(url)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("View Web Profile", color = scheme.onSurface, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            // Overview Performance Card
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Account Overview", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Games:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.totalGames}", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Record (W / L / D):", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.wins} W / ${activeUser.losses} L / ${activeUser.draws} D", fontSize = 12.sp, color = scheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Country:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text(activeUser.country.ifBlank { "INDIA" }, fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Right Column: Ratings & Breakdown Grid
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Ratings Breakdown ($selectedProfilePlatform)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = scheme.onSurface)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val bullet = activeUser.bulletRating?.toString() ?: "N/A"
                                val blitz = activeUser.blitzRating?.toString() ?: "N/A"
                                val rapid = activeUser.rapidRating?.toString() ?: "N/A"
                                val classical = activeUser.classicalRating?.toString() ?: "N/A"
                                val puzzles = activeUser.puzzleRating?.toString() ?: "N/A"

                                listOf(
                                    Triple("Bullet", bullet, Icons.Filled.Bolt),
                                    Triple("Blitz", blitz, Icons.Filled.Speed),
                                    Triple("Rapid", rapid, Icons.Filled.HourglassTop),
                                    Triple("Classical", classical, Icons.Filled.Business),
                                    Triple("Puzzles", puzzles, Icons.Filled.Extension)
                                ).forEach { (mode, rating, icon) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
                                        modifier = Modifier.weight(1f).aspectRatio(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(icon, mode, tint = scheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(mode, fontSize = 10.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                                            Text(rating, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, maxLines = 1)
                                        }
                                    }
                                }
                            }

                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Match Type Breakdown ($selectedProfilePlatform)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = scheme.primary)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Blitz Games:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.blitzGames}", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Bullet Games:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.bulletGames}", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Rapid Games:", fontSize = 12.sp, color = scheme.onSurfaceVariant)
                                        Text("${activeUser.rapidGames}", fontSize = 12.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Portrait layout
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = inputUsername,
                                    onValueChange = onInputUsernameChange,
                                    label = { Text("Username", color = scheme.onSurfaceVariant) },
                                    placeholder = { Text("e.g. magnuson-carlsium", color = scheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = scheme.onSurface,
                                        unfocusedTextColor = scheme.onSurface,
                                        focusedBorderColor = scheme.primary,
                                        unfocusedBorderColor = scheme.outlineVariant
                                    ),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth().background(scheme.surfaceContainerHighest, RoundedCornerShape(10.dp)).padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf("Lichess", "Chess.com").forEach { platform ->
                                        val isActive = selectedProfilePlatform == platform
                                        Box(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isActive) scheme.primary else Color.Transparent).clickable { selectedProfilePlatform = platform }.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(platform, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isActive) scheme.onPrimary else scheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val u = inputUsername.ifBlank { "magnuson-carlsium" }
                                            vm.fetchDualPlatformProfile(u, "Both")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = scheme.primary)
                                    ) { Text("Fetch Ratings", color = scheme.onPrimary, fontWeight = FontWeight.Bold) }

                                    OutlinedButton(
                                        onClick = {
                                            val u = activeUser.username.ifBlank { inputUsername.ifBlank { "magnuson-carlsium" } }
                                            val url = if (selectedProfilePlatform == "Lichess") "https://lichess.org/@/$u" else "https://www.chess.com/member/$u"
                                            uriHandler.openUri(url)
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("View Profile", color = scheme.onSurface) }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val bullet = activeUser.bulletRating?.toString() ?: "N/A"
                            val blitz = activeUser.blitzRating?.toString() ?: "N/A"
                            val rapid = activeUser.rapidRating?.toString() ?: "N/A"
                            val classical = activeUser.classicalRating?.toString() ?: "N/A"
                            val puzzles = activeUser.puzzleRating?.toString() ?: "N/A"

                            listOf(
                                Triple("Bullet", bullet, Icons.Filled.Bolt),
                                Triple("Blitz", blitz, Icons.Filled.Speed),
                                Triple("Rapid", rapid, Icons.Filled.HourglassTop),
                                Triple("Classical", classical, Icons.Filled.Business),
                                Triple("Puzzles", puzzles, Icons.Filled.Extension)
                            ).forEach { (mode, rating, icon) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerHigh),
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(icon, mode, tint = scheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(mode, fontSize = 10.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                                        Text(rating, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = scheme.onSurface, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        "Friends" -> {
            SocialPanel(
                friends = friends,
                onAddFriend = { onAddFriend() },
                onPlayerClick = { user ->
                    onPlayerClick(user)
                }
            )
        }

        "Leaderboard" -> {
            LeaderboardPanel(
                leaderboard = lichessLeaderboard,
                onPlayerClick = { user -> onPlayerClick(user) }
            )
        }

        "Live TV" -> {
            if (lichessLiveGames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading live TV channels…", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Chess TV Channels", style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary, fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { vm.fetchLichessLiveGames() }) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = scheme.primary)
                        }
                    }
                    lichessLiveGames.forEach { game ->
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                                        Spacer(Modifier.width(6.dp))
                                        Text(game.channel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = scheme.primary)
                                    }
                                    SuggestionChip(onClick = {}, label = { Text(game.speed, fontSize = 11.sp) })
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("♔ ${game.white}", fontSize = 13.sp, color = scheme.onSurface)
                                        Text("vs", fontSize = 11.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 2.dp))
                                        Text("♚ ${game.black}", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Rating", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                                        Text("${game.rating}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = scheme.secondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        "Events" -> {
            if (lichessStreamEvents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading broadcasts…", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Chess Broadcasts", style = MaterialTheme.typography.titleSmall,
                            color = scheme.primary, fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { vm.fetchLichessStreamEvents() }) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = scheme.primary)
                        }
                    }
                    lichessStreamEvents.forEach { event ->
                        GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (event.isLive) {
                                        Box(
                                            modifier = Modifier.background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("LIVE", fontSize = 10.sp, color = scheme.onSurface, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        event.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        color = scheme.onSurface, modifier = Modifier.weight(1f)
                                    )
                                }
                                if (event.description.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(event.description, fontSize = 12.sp, color = scheme.onSurfaceVariant, maxLines = 2)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text("lichess.org →", fontSize = 11.sp, color = scheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
