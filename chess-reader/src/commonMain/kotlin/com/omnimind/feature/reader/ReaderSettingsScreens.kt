package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.data.preferences.AppPreferences
import com.creativeblisslabs.omnimind.core.ui.components.GlassCard
import com.creativeblisslabs.omnimind.core.ui.components.ProductHeader
import com.creativeblisslabs.omnimind.core.ui.components.StatusPill
import com.creativeblisslabs.omnimind.core.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.graphics.Color

enum class ReaderSection { Font, Layout, Color, Behavior, Tts, Language, Integrations, Custom, Accessibility }

@Composable
fun ReaderSettingsScreen(navController: NavController, section: ReaderSection) {
    // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
    
    val prefs: AppPreferences = koinInject()
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    val fontSize by prefs.readerFontSize.collectAsState(initial = 18f)
    val lineHeight by prefs.readerLineHeight.collectAsState(initial = 1.2f)
    val readerTheme by prefs.readerTheme.collectAsState(initial = "dark")
    val family by prefs.readerFontFamily.collectAsState(initial = "serif")
    val ttsEnabled by prefs.readerTtsEnabled.collectAsState(initial = true)
    val language by prefs.readerLanguage.collectAsState(initial = "en")
    val translationEnabled by prefs.readerTranslationEnabled.collectAsState(initial = false)

    Column(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).verticalScroll(scroll)
    ) {
        ProductHeader(
            title = section.name,
            subtitle = "Saved reader preferences for imported books and the in-app reading surface.",
            icon = Icons.Filled.MenuBook,
            accent = Color(0xFFE8883A),
            modifier = Modifier.padding(16.dp),
            action = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.Close, null, tint = Color.Gray)
                }
            }
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Saved", Color(0xFFE8883A))
            StatusPill("Reader panel", androidx.compose.material3.MaterialTheme.colorScheme.secondary)
            StatusPill(section.name, androidx.compose.material3.MaterialTheme.colorScheme.tertiary)
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (section) {
                    ReaderSection.Font -> {
                        Text("Font", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Tune book text size, line rhythm, and typeface.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text("Font size: ${fontSize.toInt()}sp")
                        Slider(value = fontSize, onValueChange = { v -> scope.launch { prefs.setReaderFontSize(v) } }, valueRange = 12f..28f)
                        Text("Line height: ${"%.1f".format(lineHeight)}")
                        Slider(value = lineHeight, onValueChange = { v -> scope.launch { prefs.setReaderLineHeight(v) } }, valueRange = 1.0f..2.0f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("serif", "sans", "mono").forEach { option ->
                                FilterChip(
                                    selected = family == option,
                                    onClick = { scope.launch { prefs.setReaderFontFamily(option) } },
                                    label = { Text(option.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                    ReaderSection.Layout -> {
                        Text("Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Keep paragraph flow comfortable for long reading sessions.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        listOf("compact", "comfortable", "wide").forEach { option ->
                            FilterChip(
                                selected = readerTheme == option,
                                onClick = { scope.launch { prefs.setReaderTheme(option) } },
                                label = { Text(option.replaceFirstChar { it.uppercase() }) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    ReaderSection.Color -> {
                        Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Theme state is saved locally and applied to the reader surface.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("dark", "light", "sepia").forEach { option ->
                                FilterChip(
                                    selected = readerTheme == option,
                                    onClick = { scope.launch { prefs.setReaderTheme(option) } },
                                    label = { Text(option.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                    ReaderSection.Behavior -> {
                        val keepAwake by prefs.readerKeepScreenAwake.collectAsState(initial = true)
                        val pagingAnim by prefs.readerPagingAnimation.collectAsState(initial = true)
                        Text("Behavior", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Behavior controls affect how reading sessions feel during long form use.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = keepAwake, onCheckedChange = { scope.launch { prefs.setReaderKeepScreenAwake(it) } })
                            Spacer(Modifier.width(8.dp))
                            Text("Keep screen awake")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = pagingAnim, onCheckedChange = { scope.launch { prefs.setReaderPagingAnimation(it) } })
                            Spacer(Modifier.width(8.dp))
                            Text("Paging animation")
                        }
                    }
                    ReaderSection.Tts -> {
                        Text("TTS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Speech highlighting and voice playback are stored locally.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = ttsEnabled, onCheckedChange = { scope.launch { prefs.setReaderTtsEnabled(it) } })
                            Spacer(Modifier.width(8.dp))
                            Text("Enable reader TTS")
                        }
                    }
                    ReaderSection.Language -> {
                        Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Dictionary and translation defaults for the reader.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("en", "es", "fr", "de").forEach { option ->
                                FilterChip(
                                    selected = language == option,
                                    onClick = { scope.launch { prefs.setReaderLanguage(option) } },
                                    label = { Text(option.uppercase()) }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(checked = translationEnabled, onCheckedChange = { scope.launch { prefs.setReaderTranslationEnabled(it) } })
                            Spacer(Modifier.width(8.dp))
                            Text("Enable translation assist")
                        }
                    }
                    ReaderSection.Integrations -> {
                        Text("Integrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Connect your reading experience to external platforms.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        listOf("KOReader", "Readwise", "Hardcover").forEach { platform ->
                            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                                Text("Connect to $platform")
                            }
                        }
                    }
                    ReaderSection.Custom -> {
                        Text("Custom CSS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Inject custom styles into the reader content or UI.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        OutlinedTextField(
                            value = "/* Custom Content CSS */",
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            label = { Text("Content CSS") }
                        )
                        OutlinedTextField(
                            value = "/* Custom UI CSS */",
                            onValueChange = { },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            label = { Text("UI CSS") }
                        )
                    }
                    ReaderSection.Accessibility -> {
                        Text("Accessibility", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Assistive reading and contrast-friendly controls.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var highContrast by remember { mutableStateOf(false) }
                            Switch(checked = highContrast, onCheckedChange = { highContrast = it })
                            Spacer(Modifier.width(8.dp))
                            Text("High contrast mode")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var largerTargets by remember { mutableStateOf(false) }
                            Switch(checked = largerTargets, onCheckedChange = { largerTargets = it })
                            Spacer(Modifier.width(8.dp))
                            Text("Larger touch targets")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderTocScreen(navController: NavController) {
    
    val scroll = rememberScrollState()
    val session = ReaderSessionStore.state

    Column(
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).verticalScroll(scroll)
    ) {
        ProductHeader(
            title = "Table of Contents",
            subtitle = "Live chapter list from the current book or imported document.",
            icon = Icons.Filled.List,
            accent = Color(0xFFE8883A),
            modifier = Modifier.padding(16.dp),
            action = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.Close, null, tint = Color.Gray)
                }
            }
        )

        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(session.importedTitle ?: session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Chapters: ${session.chapters.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (session.chapters.isEmpty()) {
                    Text("No imported chapters yet. Load a PDF, EPUB, or text file to build the live table of contents.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    session.chapters.forEachIndexed { index, chapter ->
                        ListItem(
                            headlineContent = { Text("Section ${index + 1}") },
                            supportingContent = { Text(chapter.take(180)) },
                            trailingContent = {
                                AssistChip(onClick = {
                                    ReaderSessionStore.update { copy(currentChapter = index) }
                                    navController.popBackStack()
                                }, label = { Text("Open") })
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}
