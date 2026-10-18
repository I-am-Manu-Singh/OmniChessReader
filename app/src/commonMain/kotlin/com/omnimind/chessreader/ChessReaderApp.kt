package com.creativeblisslabs.omnichessreader

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import com.creativeblisslabs.omnimind.core.navigation.Screen
import com.creativeblisslabs.omnimind.core.ui.theme.OmniMindTheme
import com.creativeblisslabs.omnimind.feature.chess.ChessScreen
import com.creativeblisslabs.omnimind.feature.reader.*
import org.koin.compose.koinInject

@Composable
fun ChessReaderApp() {
    val prefs: AppPreferences = koinInject()
    val themeMode by prefs.theme.collectAsState(initial = "dark")
    val isSystemDark = isSystemInDarkTheme()

    val darkTheme = when (themeMode.lowercase()) {
        "light" -> false
        "dark", "oled" -> true
        else -> isSystemDark
    }

    OmniMindTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)

            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {
                composable(Screen.Dashboard.route) {
                    HomeScreen(navController = navController)
                }
                composable(Screen.Settings.route) {
                    OmniChessSettingsScreen(navController = navController)
                }
                composable(Screen.Chess.route + "?invite={invite}") { backStackEntry ->
                    val invite = backStackEntry.arguments?.getString("invite")
                    ChessScreen(navController = navController, invite = invite)
                }
                composable(Screen.Chess.route) {
                    ChessScreen(navController = navController)
                }
                composable(Screen.ReaderHub.route) {
                    ReaderHubScreen(navController = navController)
                }
                composable(Screen.BookReader.route) {
                    BookReaderScreen(navController = navController)
                }
                composable(Screen.ReaderToc.route) {
                    ReaderTocScreen(navController = navController)
                }
                composable(Screen.ReaderFont.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Font)
                }
                composable(Screen.ReaderLayout.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Layout)
                }
                composable(Screen.ReaderColor.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Color)
                }
                composable(Screen.ReaderBehavior.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Behavior)
                }
                composable(Screen.ReaderTts.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Tts)
                }
                composable(Screen.ReaderLanguage.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Language)
                }
                composable(Screen.ReaderIntegrations.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Integrations)
                }
                composable(Screen.ReaderCustom.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Custom)
                }
                composable(Screen.ReaderAccessibility.route) {
                    ReaderSettingsScreen(navController = navController, section = ReaderSection.Accessibility)
                }
            }
        }
    }
}
