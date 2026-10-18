package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.creativeblisslabs.omnimind.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderSettingsSheet(
    showSettingsSheet: Boolean,
    onDismiss: () -> Unit,
    selectedSheetTab: Int,
    onSheetTabChange: (Int) -> Unit,
    viewModel: ReaderHubViewModel
) {
    if (!showSettingsSheet) return
    // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
    val AmberWarm = Color(0xFFE8883A)
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = scheme.surfaceVariant,
        dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            var isSearchingSettings by remember { mutableStateOf(false) }
            var settingsSearchQuery by remember { mutableStateOf("") }

            // Header of Bottom Sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, "Back", tint = scheme.primary, modifier = Modifier.size(32.dp))
                }
                val title = when (selectedSheetTab) {
                    0 -> "Font"
                    1 -> "Layout"
                    2 -> "Color"
                    3 -> "Behavior"
                    4 -> "Language"
                    5 -> "Sharing"
                    6 -> "TTS"
                    else -> "Custom"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { isSearchingSettings = !isSearchingSettings }) {
                    Icon(if (isSearchingSettings) Icons.Default.Close else Icons.Default.Search, "Search", tint = scheme.primary, modifier = Modifier.size(32.dp))
                }
            }

            if (isSearchingSettings) {
                OutlinedTextField(
                    value = settingsSearchQuery,
                    onValueChange = { q ->
                        settingsSearchQuery = q
                        val lower = q.lowercase()
                        if (lower.contains("font") || lower.contains("size") || lower.contains("weight") || lower.contains("serif")) {
                            onSheetTabChange(0)
                        } else if (lower.contains("layout") || lower.contains("margin") || lower.contains("spacing") || lower.contains("column") || lower.contains("scroll")) {
                            onSheetTabChange(1)
                        } else if (lower.contains("color") || lower.contains("theme") || lower.contains("dark") || lower.contains("light") || lower.contains("highlight")) {
                            onSheetTabChange(2)
                        } else if (lower.contains("behavior") || lower.contains("autoscroll") || lower.contains("speed") || lower.contains("gesture")) {
                            onSheetTabChange(3)
                        } else if (lower.contains("language") || lower.contains("translat") || lower.contains("dict")) {
                            onSheetTabChange(4)
                        } else if (lower.contains("tts") || lower.contains("voice") || lower.contains("pitch") || lower.contains("speech")) {
                            onSheetTabChange(6)
                        }
                    },
                    placeholder = { Text("Jump to setting (font, color, autoscroll, tts...)", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmberWarm,
                        unfocusedBorderColor = scheme.outline.copy(alpha = 0.3f),
                        focusedTextColor = scheme.onSurface,
                        unfocusedTextColor = scheme.onSurface
                    ),
                    trailingIcon = {
                        if (settingsSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { settingsSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )
            }

            // Horizontal tab icons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icons = listOf(
                    Icons.Default.TextFields to "Font",
                    Icons.Default.GridView to "Layout",
                    Icons.Default.Palette to "Color",
                    Icons.Default.TouchApp to "Behavior",
                    Icons.Default.Translate to "Language",
                    Icons.Default.Share to "Sharing",
                    Icons.Default.VolumeUp to "TTS",
                    Icons.Default.AccessibilityNew to "Custom"
                )
           icons.forEachIndexed { idx, (icon, label) ->
    IconButton(
        onClick = { onSheetTabChange(idx) },
        modifier = Modifier
            .size(42.dp)
            .background(
                if (selectedSheetTab == idx) MaterialTheme.colorScheme.onPrimary else Color.Transparent, // Fixed
                RoundedCornerShape(8.dp)
            )
    ) {
        Icon(icon, label, tint = if (selectedSheetTab == idx) AmberWarm else scheme.onSurface)
    }
}
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary)

            // Tab contents
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedSheetTab) {
                    0 -> {
                        // FONT TAB
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Override Book Font", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            Switch(checked = viewModel.overrideBookFont, onCheckedChange = { viewModel.updateOverrideBookFont(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Text("Font Size", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        settingsIncDecRow("Default Font Size", viewModel.defaultFontSize) { viewModel.updateDefaultFontSize((viewModel.defaultFontSize + it).coerceIn(10, 48)) }
                        settingsIncDecRow("Minimum Font Size", viewModel.minFontSize) { viewModel.updateMinFontSize((viewModel.minFontSize + it).coerceIn(6, 24)) }
                        Text("Font Weight", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        settingsIncDecRow("Font Weight", viewModel.fontWeightVal, step = 100) { viewModel.updateFontWeight((viewModel.fontWeightVal + it).coerceIn(100, 900)) }
                        Text("Font Family", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        settingsInteractiveDropdownRow(
                            label = "Serif Font",
                            selectedValue = viewModel.fontFaceSerif,
                            options = listOf("Merriweather", "Georgia", "Palatino", "Garamond", "Times New Roman"),
                            onOptionSelected = { viewModel.updateFontFaceSerif(it) }
                        )
                        settingsInteractiveDropdownRow(
                            label = "Sans-Serif Font",
                            selectedValue = viewModel.fontFaceSans,
                            options = listOf("Inter", "Roboto", "Outfit", "Helvetica", "System Sans"),
                            onOptionSelected = { viewModel.updateFontFaceSans(it) }
                        )
                        settingsInteractiveDropdownRow(
                            label = "Monospace Font",
                            selectedValue = viewModel.fontFaceMono,
                            options = listOf("Fira Code", "JetBrains Mono", "Courier New", "Source Code"),
                            onOptionSelected = { viewModel.updateFontFaceMono(it) }
                        )
                    }
                    1 -> {
                        // LAYOUT TAB
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Override Book Layout", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            Switch(checked = viewModel.overrideBookLayout, onCheckedChange = { viewModel.updateOverrideBookLayout(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Writing Mode", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { viewModel.updateWritingMode("Vertical") }) { Icon(Icons.Default.RotateLeft, null, tint = if (viewModel.writingMode == "Vertical") AmberWarm else scheme.onSurface) }
                                IconButton(onClick = { viewModel.updateWritingMode("Horizontal") }) { Icon(Icons.Default.Title, null, tint = if (viewModel.writingMode == "Horizontal") AmberWarm else scheme.onSurface) }
                                IconButton(onClick = { viewModel.updateWritingMode("LeftToRight") }) { Icon(Icons.Default.FormatAlignLeft, null, tint = if (viewModel.writingMode == "LeftToRight") AmberWarm else scheme.onSurface) }
                            }
                        }
                        Text("Paragraph",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Use Book Layout", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.useBookLayout, onCheckedChange = { viewModel.updateUseBookLayout(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                    settingsIncDecRow("Paragraph Margin", viewModel.paragraphMargin.toInt()) { viewModel.updateParagraphMargin((viewModel.paragraphMargin + it).coerceIn(0f, 4f)) }
                    settingsIncDecRow("Line Spacing", viewModel.lineSpacing.toInt()) { viewModel.updateLineSpacing((viewModel.lineSpacing + it).coerceIn(0f, 3f)) }
                    settingsIncDecRow("Word Spacing", viewModel.wordSpacing) { viewModel.updateWordSpacing((viewModel.wordSpacing + it).coerceIn(-10, 50)) }
                    settingsIncDecRow("Letter Spacing", viewModel.letterSpacing) { viewModel.updateLetterSpacing((viewModel.letterSpacing + it).coerceIn(-10, 50)) }
                    settingsIncDecRow("Text Indent", viewModel.textIndent) { viewModel.updateTextIndent((viewModel.textIndent + it).coerceIn(0, 100)) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Full Justification", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.fullJustification, onCheckedChange = { viewModel.updateFullJustification(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hyphenation", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.hyphenation, onCheckedChange = { viewModel.updateHyphenation(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }

                        Text("Page", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        settingsIncDecRow("Top Margin (px)", viewModel.topMargin) { viewModel.updateTopMargin((viewModel.topMargin + it).coerceIn(0, 200)) }
                        settingsIncDecRow("Bottom Margin (px)", viewModel.bottomMargin) { viewModel.updateBottomMargin((viewModel.bottomMargin + it).coerceIn(0, 200)) }
                        settingsIncDecRow("Left Margin (px)", viewModel.leftMargin) { viewModel.updateLeftMargin((viewModel.leftMargin + it).coerceIn(0, 200)) }
                        settingsIncDecRow("Right Margin (px)", viewModel.rightMargin) { viewModel.updateRightMargin((viewModel.rightMargin + it).coerceIn(0, 200)) }
                        settingsIncDecRow("Column Gap (%)", viewModel.columnGap) { viewModel.updateColumnGap((viewModel.columnGap + it).coerceIn(0, 100)) }
                        settingsIncDecRow("Maximum Number of Columns", viewModel.maxColumns) { viewModel.updateMaxColumns((viewModel.maxColumns + it).coerceIn(1, 4)) }
                        settingsIncDecRow("Maximum Column Width", viewModel.maxColumnWidth) { viewModel.updateMaxColumnWidth((viewModel.maxColumnWidth + it).coerceIn(100, 2000)) }
                        settingsIncDecRow("Maximum Column Height", viewModel.maxColumnHeight) { viewModel.updateMaxColumnHeight((viewModel.maxColumnHeight + it).coerceIn(100, 3000)) }

                        Text("Header & Footer",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Header", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showHeader,
                                onCheckedChange = { viewModel.updateShowHeader(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Footer", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showFooter,
                                onCheckedChange = { viewModel.updateShowFooter(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Remaining Time", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showRemainingTime,
                                onCheckedChange = { viewModel.updateShowRemainingTime(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Remaining Pages", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showRemainingPages,
                                onCheckedChange = { viewModel.updateShowRemainingPages(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Reading Progress", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showReadingProgress,
                                onCheckedChange = { viewModel.updateShowReadingProgress(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        settingsInteractiveDropdownRow(
                            label = "Reading Progress Style",
                            selectedValue = viewModel.progressStyle,
                            options = listOf("Page Number", "Percentage Read", "Estimated Time Left", "Chapter Position"),
                            onOptionSelected = { viewModel.updateProgressStyle(it) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Progress Bar", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showProgressBar,
                                onCheckedChange = { viewModel.updateShowProgressBar(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Current Time", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showCurrentTime,
                                onCheckedChange = { viewModel.updateShowCurrentTime(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Current Battery Status", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showBatteryStatus,
                                onCheckedChange = { viewModel.updateShowBatteryStatus(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Battery Percentage", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.showBatteryPercentage,
                                onCheckedChange = { viewModel.updateShowBatteryPercentage(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }

                        Text("Screen", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Orientation", color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { viewModel.updateOrientation("Portrait") }) {
                                    Icon(Icons.Default.StayCurrentPortrait, null, tint = if (viewModel.orientationSelected == "Portrait") AmberWarm else scheme.onSurface)
                                }
                                IconButton(onClick = { viewModel.updateOrientation("Landscape") }) {
                                    Icon(Icons.Default.StayCurrentLandscape, null, tint = if (viewModel.orientationSelected == "Landscape") AmberWarm else scheme.onSurface)
                                }
                            }
                        }
                    }
                    2 -> {
                        // COLOR TAB
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Theme Mode", color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { viewModel.updateThemeMode("System") }) { Icon(Icons.Default.SettingsSuggest, null, tint = if (viewModel.themeMode == "System") AmberWarm else scheme.onSurface) }
                                IconButton(onClick = { viewModel.updateThemeMode("Light") }) { Icon(Icons.Default.LightMode, null, tint = if (viewModel.themeMode == "Light") AmberWarm else scheme.onSurface) }
                                IconButton(onClick = { viewModel.updateThemeMode("Dark") }) { Icon(Icons.Default.DarkMode, null, tint = if (viewModel.themeMode == "Dark") AmberWarm else scheme.onSurface) }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Invert Image In Dark Mode", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.invertImageDark, onCheckedChange = { viewModel.updateInvertImageDark(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Override Book Color", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.overrideBookColor, onCheckedChange = { viewModel.updateOverrideBookColor(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }

                        Text("Theme Color", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        val isDarkMode = when (viewModel.themeMode) {
                            "Light" -> false
                            "Dark" -> true
                            else -> androidx.compose.foundation.isSystemInDarkTheme()
                        }
                        val themePresets = if (isDarkMode) {
                            listOf(
                                listOf("Default", "Gray", "Contrast"),
                                listOf("Grass", "Cherry", "Sunset"),
                                listOf("Gruvbox", "Nord", "OLED")
                            )
                        } else {
                            listOf(
                                listOf("Light", "Paper", "Sepia"),
                                listOf("Solarized", "E-Ink", "Custom")
                            )
                        }
                        themePresets.forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowThemes.forEach { themeName ->
                                    val cardBg = when (themeName) {
                                        "Sepia" -> Color(0xFFF4ECD8)
                                        "Contrast", "OLED" -> Color(0xFF000000)
                                        "Gray" -> Color(0xFF262626)
                                        "Grass" -> Color(0xFF1D281E)
                                        "Cherry" -> Color(0xFF281C1E)
                                        "Light", "Paper" -> Color(0xFFFAF9F6)
                                        "Solarized" -> Color(0xFFFDF6E3)
                                        "Gruvbox" -> Color(0xFF282828)
                                        "Nord" -> Color(0xFF2E3440)
                                        "Sunset" -> Color(0xFF2D1B2D)
                                        "E-Ink" -> Color(0xFFFFFFFF)
                                        else -> Color(0xFF0F0F0F)
                                    }
                                    val cardText = when (themeName) {
                                        "Sepia", "Light", "Paper", "Solarized", "E-Ink" -> Color(0xFF1E1E1E)
                                        else -> Color(0xFFEDEDED)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (viewModel.activeColorTheme == themeName) 2.dp else 1.dp,
                                                color = if (viewModel.activeColorTheme == themeName) AmberWarm else Color(0xFF444444),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .background(cardBg, RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.updateActiveColorTheme(themeName)
                                            }
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (themeName) {
                                                            "Sepia" -> Color(0xFF5B4636)
                                                            "Contrast" -> scheme.background
                                                            "Gray" -> Color.LightGray
                                                            "Grass" -> Color(0xFFE2EED2)
                                                            "Cherry" -> Color(0xFFF9DADA)
                                                            "Light" -> Color(0xFF1A1A1A)
                                                            else -> scheme.background
                                                        }
                                                    )
                                            )
                                            Text(themeName, color = cardText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Text("Background Image (Library)", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val bgs = listOf("Solid", "Sand", "Snow", "Leaves", "Moon", "Stars")
                            bgs.forEachIndexed { index, name ->
                               Box(
    modifier = Modifier
        .size(50.dp)
        .border(
            width = if (viewModel.activeBackgroundIndex == index) 2.dp else 1.dp,
            color = if (viewModel.activeBackgroundIndex == index) AmberWarm else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
        .background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp)) // Fixed parenthesis here
        .clickable { viewModel.updateActiveBackgroundIndex(index) }
)

                            }
                        }

                        Text("Highlight Colors", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val highlights = listOf("Red", "Yellow", "Green", "Blue", "Violet")
                            highlights.forEach { c ->
                                IconButton(onClick = { viewModel.updateHighlightColorSelection(c) }) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (c) {
                                                    "Red" -> Color.Red
                                                    "Yellow" -> Color.Yellow
                                                    "Green" -> Color.Green
                                                    "Blue" -> Color.Blue
                                                    else -> Color(0xFF8E44AD)
                                                }
                                            )
                                            .border(
                                                width = if (viewModel.highlightColorSelection == c) 2.dp else 0.dp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                        settingsIncDecRowFloat("Opacity", viewModel.highlightOpacity) { v -> viewModel.updateHighlightOpacity((viewModel.highlightOpacity + v).coerceIn(0.1f, 1.0f)) }

                        Text("Reading Ruler", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Reading Ruler", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.enableReadingRuler, onCheckedChange = { viewModel.updateEnableReadingRuler(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        settingsIncDecRow("Lines to Highlight", viewModel.linesToHighlight) { v -> viewModel.updateLinesToHighlight((viewModel.linesToHighlight + v).coerceIn(1, 10)) }
                        settingsInteractiveDropdownRow(
                            label = "Ruler Color",
                            selectedValue = viewModel.rulerColor,
                            options = listOf("Yellow", "Cyan", "Green", "Magenta", "Amber"),
                            onOptionSelected = { viewModel.updateRulerColor(it) }
                        )
                        settingsIncDecRowFloat("Opacity", viewModel.rulerOpacity) { v -> viewModel.updateRulerOpacity((viewModel.rulerOpacity + v).coerceIn(0.1f, 1.0f)) }

                        Text("Code Highlighting",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Highlighting", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.enableCodeHighlighting, onCheckedChange = { viewModel.updateEnableCodeHighlighting(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        settingsInteractiveDropdownRow(
                            label = "Code Language",
                            selectedValue = viewModel.codeLanguageSelection,
                            options = listOf("Auto-Detect", "Kotlin", "Python", "Java", "JavaScript", "HTML/CSS", "C++"),
                            onOptionSelected = { viewModel.updateCodeLanguageSelection(it) }
                        )
                    }
                    3 -> {
                        // BEHAVIOR TAB
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .clickable { /* Customize */ }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            
                            Text("Customize Toolbar", color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF666666))
                        }

                        Text("Reading Mode", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Continuous Scrolling", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.isScrolledMode, onCheckedChange = { viewModel.updateIsScrolledMode(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-Scroll", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.isAutoScrolling, onCheckedChange = { viewModel.updateIsAutoScrolling(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        if (viewModel.isAutoScrolling) {
                            settingsInteractiveDropdownRow(
                                label = "Auto-Scroll Mode",
                                selectedValue = viewModel.autoscrollMode,
                                options = listOf("Line by Line", "Page by Page", "Paragraph by Paragraph"),
                                onOptionSelected = { viewModel.updateAutoscrollMode(it) }
                            )
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Scroll Interval / Speed", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Text("${(viewModel.autoscrollSpeed * 10).toInt() / 10.0}s", color = AmberWarm, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = viewModel.autoscrollSpeed,
                                    onValueChange = { viewModel.updateAutoscrollSpeed(it) },
                                    valueRange = 0.5f..10.0f,
                                    steps = 18,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AmberWarm,
                                        activeTrackColor = AmberWarm,
                                        inactiveTrackColor = Color(0xFF333333)
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Paragraph Mode", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.isParagraphMode, onCheckedChange = { viewModel.updateIsParagraphMode(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speed Reading", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.isSpeedReading, onCheckedChange = { viewModel.updateIsSpeedReading(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Mode", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.isAutoMode, onCheckedChange = { viewModel.updateIsAutoMode(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }

                        Text("Animation", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Paging Animation", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.pagingAnimation, onCheckedChange = { viewModel.updatePagingAnimation(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        settingsInteractiveDropdownRow(
                            label = "Animation Style",
                            selectedValue = viewModel.animationStyle,
                            options = listOf("Page Curl", "Horizontal Slide", "Push Fade", "Instant Scroll"),
                            onOptionSelected = { viewModel.updateAnimationStyle(it) }
                        )

                        Text("Device",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("E-Ink Mode", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.eInkMode, onCheckedChange = { viewModel.updateEInkMode(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Color E-Ink Mode", color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = viewModel.colorEInkMode,
                                onCheckedChange = { viewModel.updateColorEInkMode(it) },
                                enabled = viewModel.eInkMode,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("System Screen Brightness", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.systemScreenBrightness, onCheckedChange = { viewModel.updateSystemScreenBrightness(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        if (!viewModel.systemScreenBrightness) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Auto Brightness", color = MaterialTheme.colorScheme.onSurface)
                                Switch(checked = viewModel.autoBrightness, onCheckedChange = { viewModel.updateAutoBrightness(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                            }
                            if (!viewModel.autoBrightness) {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Brightness", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                        Text("${viewModel.brightnessValue}%", color = AmberWarm, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = viewModel.brightnessValue.toFloat(),
                                        onValueChange = { viewModel.updateBrightnessValue(it.toInt()) },
                                        valueRange = 0f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AmberWarm,
                                            activeTrackColor = AmberWarm,
                                            inactiveTrackColor = Color(0xFF333333)
                                        )
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Swipe for Brightness", color = MaterialTheme.colorScheme.onSurface)
                                Text("Slide along the left edge", color = Color.Gray, fontSize = 12.sp)
                            }
                            Switch(checked = viewModel.swipeBrightness, onCheckedChange = { viewModel.updateSwipeBrightness(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Keep Screen Awake", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.keepScreenAwake, onCheckedChange = { viewModel.updateKeepScreenAwake(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }

                        Text("Security",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Allow JavaScript", color = MaterialTheme.colorScheme.onSurface)
                                Text("Enable only if you trust the file.", color = Color.Gray, fontSize = 12.sp)
                            }
                            Switch(checked = viewModel.allowJavaScript, onCheckedChange = { viewModel.updateAllowJavaScript(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }

                    }
                    4 -> {
                        // LANGUAGE TAB
                        Text("Dictionaries", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .clickable { /* Manage */ }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Manage Dictionaries", color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF666666))
                        }

                        Text("Word Lens", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                .clickable { /* Toggle */ }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Translate, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Word Lens", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Show a short native-language hint abo...", color = Color.Gray, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF666666))
                        }

                        Text("Translation", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable Translation", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.enableTranslation, onCheckedChange = { viewModel.updateEnableTranslation(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show Source Text", color = MaterialTheme.colorScheme.onSurface)
                            Switch(checked = viewModel.showSourceText, onCheckedChange = { viewModel.updateShowSourceText(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF161616), checkedTrackColor = AmberWarm))
                        }
                        settingsInteractiveDropdownRow(
                            label = "TTS Text",
                            selectedValue = viewModel.ttsTextOption,
                            options = listOf("Source and Translated", "Source Only", "Translated Only"),
                            onOptionSelected = { viewModel.updateTtsTextOption(it) }
                        )
                        settingsInteractiveDropdownRow(
                            label = "Translation Service",
                            selectedValue = viewModel.translationService,
                            options = listOf("Local On-Device AI", "Azure Translator", "DeepL Engine", "Google Translate API"),
                            onOptionSelected = { viewModel.updateTranslationService(it) }
                        )
                        settingsInteractiveDropdownRow(
                            label = "Translate To",
                            selectedValue = viewModel.translateToLanguage,
                            options = listOf("English", "Spanish", "French", "German", "Japanese", "Chinese", "Hindi", "Italian", "Russian", "Portuguese", "Korean", "Arabic"),
                            onOptionSelected = { viewModel.updateTranslateToLanguage(it) }
                        )
                    }
                    5 -> {
                        // INTEGRATIONS TAB
                        Text("Integrations", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Connect to external services for sync, highlights, and catalogs.", color = Color.Gray, fontSize = 14.sp)

                        Text("Reading Sync", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        integrationItem("KOReader", "Not connected")
                        integrationItem("Readwise", "Not connected")
                        integrationItem("Hardcover", "Not connected")

                        Text("Cloud Sync", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        integrationItem("Cloud", "Not signed in")
                        integrationItem("Google Drive", "Not connected", isPremium = true)
                        integrationItem("WebDAV", "Not connected", isPremium = true)
                        integrationItem("S3 Storage", "Not connected", isPremium = true)

                        Text("Content Sources", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        integrationItem("OPDS Catalogs", "No catalogs")
                        integrationItem("Send to library", "Email books to your library")
                    }
                    6 -> {
                        // TTS TAB
                        Text("TTS Highlighting",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        settingsInteractiveDropdownRow(
                            label = "Granularity",
                            selectedValue = viewModel.ttsGranularity,
                            options = listOf("Word by Word", "Sentence by Sentence", "Paragraph"),
                            onOptionSelected = { /* viewModel.updateTtsGranularity(it) */ }
                        )
                        settingsInteractiveDropdownRow(
                            label = "Style",
                            selectedValue = viewModel.ttsStyle,
                            options = listOf("Highlighter", "Underline", "Background Fill", "Bold Text"),
                            onOptionSelected = { viewModel.updateTtsStyle(it) }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Color", color = MaterialTheme.colorScheme.onSurface)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
                                Icon(Icons.Default.Colorize, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Colors", color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                val colors = listOf(Color.Yellow, Color.Cyan, Color.Magenta, Color.Green, Color(0xFFE67E22))
                                colors.forEach { c ->
                                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(c))
                                }
                            }
                        }

                        Text("Media Info", color =  MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        settingsInteractiveDropdownRow(
                            label = "Update Frequency",
                            selectedValue = viewModel.ttsUpdateFrequency,
                            options = listOf("Every Sentence", "Every Word", "Every Paragraph"),
                            onOptionSelected = { viewModel.updateTtsUpdateFrequency(it) }
                        )
                    }
                    7 -> {
                        // CUSTOM CSS TAB
                        Text("Custom Content CSS",  color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = viewModel.customContentCss,
                            onValueChange = { viewModel.updateCustomContentCss(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            placeholder = { Text("Enter CSS for book content styling...", color = Color.Gray) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                focusedBorderColor = Color(0xFFE8883A),
                                unfocusedBorderColor = Color(0xFF333333)
                            )
                        )

                        Text("Custom Reader UI CSS", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = viewModel.customReaderUiCss,
                            onValueChange = { viewModel.updateCustomReaderUiCss(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            placeholder = { Text("Enter CSS for reader interface styling...", color = Color.Gray) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                focusedBorderColor = Color(0xFFE8883A),
                                unfocusedBorderColor = Color(0xFF333333)
                            )
                        )
                    }
                }
            }
        }
    }
}






@Composable
private fun settingsIncDecRow(label: String, value: Int, step: Int = 1, onUpdate: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onUpdate(-step) },
                    modifier = Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Remove, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                            Spacer(Modifier.width(6.dp))
                Text(value.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = { onUpdate(step) },
                    modifier = Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Add, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun settingsIncDecRowFloat(label: String, value: Float, onUpdate: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
                    Text(String.format("%.1f", value), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { onUpdate(-0.1f) },
                    modifier = Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Remove, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                                          

                        Text(label, color = MaterialTheme.colorScheme.onSurface)

                IconButton(
                    onClick = { onUpdate(0.1f) },
                    modifier = Modifier.size(22.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Add, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun settingsInteractiveDropdownRow(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme


    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (selectedValue.isNotBlank()) selectedValue else "Default",
                    color = Color(0xFFE8883A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                                            Spacer(Modifier.width(6.dp))

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand $label menu",
                    tint = scheme.onSurfaceVariant
                )
            }
        }
DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp, 
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = opt,
                            color = if (opt == selectedValue) Color(0xFFE8883A) else scheme.onSurface,
                            fontWeight = if (opt == selectedValue) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
