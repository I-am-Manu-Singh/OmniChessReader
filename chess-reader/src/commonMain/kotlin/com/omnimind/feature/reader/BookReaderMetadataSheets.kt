package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creativeblisslabs.omnimind.core.ui.theme.*
import com.creativeblisslabs.omnimind.core.ui.util.unescapeHtml

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocSheet(
    showTocSheet: Boolean,
    onDismiss: () -> Unit,
    showBookDetailsSheet: () -> Unit,
    selectedTocTab: Int,
    onTabChange: (Int) -> Unit,
    bookmarksList: List<String>,
    onRemoveBookmark: (String) -> Unit,
    onAddBookmark: () -> Unit,
    notesList: List<String> = emptyList(),
    onRemoveNote: (String) -> Unit = {},
    onAddNote: () -> Unit = {},
    onSelectBookmark: (String) -> Unit = {},
    chapters: List<ChapterItem>,
    session: ReaderSessionState,
    onChapterSelect: (Int) -> Unit
) {

    val scheme = androidx.compose.material3.MaterialTheme.colorScheme

  if (showTocSheet) {
    // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
    val scheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Uses the soft surface containers (latte in light / deep espresso in dark)
        containerColor = scheme.surfaceVariant,
        // Adapts the drag handle border color seamlessly to both themes
        dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) }
    ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                // Header (Top bar with Back, Title, and Actions)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Table of Contents",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                }

                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter chapters, notes...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberWarm,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    )
                }

                // Book Title Header Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session.author,
                            color = Color(0xFF888888),
                            fontSize = 14.sp
                        )
                    }
                    IconButton(onClick = {
                        onDismiss()
                        showBookDetailsSheet()
                    }) {
                        Icon(Icons.Outlined.Info, "Info", tint = Color(0xFF888888))
                    }
                }

                HorizontalDivider(color = Color(0xFF222222))
val scheme = androidx.compose.material3.MaterialTheme.colorScheme
                // Tabs Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val tabs = listOf("Chapters", "Notes (${notesList.size})", "Bookmarks (${bookmarksList.size})")
                    tabs.forEachIndexed { index, title ->
                        val active = selectedTocTab == index
                        Text(
                            text = title,
                            color = if (active) scheme.primary else scheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { onTabChange(index) }
                                .drawBehind {
                                    if (active) {
                                        drawRect(
                                            color = AmberWarm,
                                            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height + 6.dp.toPx()),
                                            size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                                        )
                                    }
                                }
                                .padding(bottom = 6.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1B1B1B))

                // Selected Tab Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTocTab) {
                        0 -> {
                            val filteredChapters = if (searchQuery.isBlank()) {
                                chapters.mapIndexed { idx, ch -> idx to ch }
                            } else {
                                chapters.mapIndexed { idx, ch -> idx to ch }.filter { it.second.title.contains(searchQuery, ignoreCase = true) }
                            }
                            // Chapters List
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {
                                items(filteredChapters.size) { fIndex ->
                                    val (index, chap) = filteredChapters[fIndex]
                                    val isCurrent = session.currentChapter == index

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
    if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) 
    else Color.Transparent
)
                                            .border(
                                                width = if (isCurrent) 1.dp else 0.dp,
                                                color = if (isCurrent) AmberWarm.copy(alpha = 0.6f) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                onChapterSelect(index)
                                                onDismiss()
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                val cleanTitle = unescapeHtml(chap.title)
                                                    .replace(Regex("^#+\\s*"), "")
                                                    .replace(Regex("_+"), " ")
                                                    .trim()

                                                if (isCurrent) {
                                                    Icon(Icons.Default.CheckCircle, "Active Chapter", tint = AmberWarm, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = cleanTitle,
                                                        color = AmberWarm,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                } else {
                                                    Icon(Icons.Default.MenuBook, null, tint = Color(0xFF666666), modifier = Modifier.size(16.dp))
                                                    Text(
                                                        text = cleanTitle,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Page ${chap.page}",
                                                color = if (isCurrent) AmberWarm.copy(alpha = 0.8f) else Color(0xFF666666),
                                                fontSize = 13.sp,
                                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = Color(0xFF1B1B1B))
                                }
                            }
                        }
                        1 -> {
                            // Notes List Tab
                            if (notesList.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                       Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                        .background(    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                    Icon(Icons.Default.EditNote, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text("No Saved Notes", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Highlight any sentence in the book to save personal notes.", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 14.sp)
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = onAddNote,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarm.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Add Quick Note", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(notesList.size) { idx ->
                                        val note = notesList[idx]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onSelectBookmark(note)
                                                    onDismiss()
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(note, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                                    Spacer(Modifier.height(4.dp))
                                                    Text("Note #${idx + 1} • Saved in ${session.title}", color = AmberWarm, fontSize = 12.sp)
                                                }
                                                IconButton(onClick = { onRemoveNote(note) }) {
                                                    Icon(Icons.Default.Delete, "Delete Note", tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Bookmarks List Tab
                            if (bookmarksList.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                        Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                        .background(    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                    Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text("No Saved Bookmarks", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Bookmark important chapters or pages to revisit them later.", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 14.sp)
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = onAddBookmark,
                                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarm.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Icon(Icons.Default.BookmarkBorder, null, tint = MaterialTheme.colorScheme.inverseOnSurface)
                                            Text("Bookmark This Page", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(bookmarksList.size) { idx ->
                                        val bm = bookmarksList[idx]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) )
                                                .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onSelectBookmark(bm)
                                                    onDismiss()
                                                }
                                                .padding(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                                                    Column {
                                                        Text(bm, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                        Text("Saved Bookmark • Tap to Jump", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                                    }
                                                }
                                                IconButton(onClick = { onRemoveBookmark(bm) }) {
                                                    Icon(Icons.Default.Delete, "Delete Bookmark", tint = Color(0xFF888888), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Navigation Bar inside TOC sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .height(64.dp)
                        .padding(horizontal = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onTabChange(0) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (selectedTocTab == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.List, "Chapters", tint = if (selectedTocTab == 0) scheme.primary.copy(alpha = 0.8f) else scheme.primary.copy(alpha = 0.8f))
                    }
                    IconButton(
                        onClick = { onTabChange(1) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (selectedTocTab == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.EditNote, "Notes", tint = if (selectedTocTab == 1) scheme.primary.copy(alpha = 0.8f) else scheme.primary.copy(alpha = 0.8f))
                    }
                    IconButton(
                        onClick = { onTabChange(2) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (selectedTocTab == 2) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.BookmarkBorder, "Bookmarks", tint = if (selectedTocTab == 2) scheme.primary.copy(alpha = 0.8f) else scheme.primary.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsSheet(
    showBookDetailsSheet: Boolean,
    onDismiss: () -> Unit,
    session: ReaderSessionState
) {
        val scheme = MaterialTheme.colorScheme

    if (showBookDetailsSheet) {
        val AmberWarm = Color(0xFFE8883A)
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with Back Button and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Book Details",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.width(48.dp)) // Spacer to center title
                }

                // Book Main Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Book Cover Box
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (session.coverImagePath.isNotBlank()) {
                            coil3.compose.AsyncImage(
                                model = session.coverImagePath,
                                contentDescription = session.title,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = session.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = session.author,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    fontSize = 9.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = session.author,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!session.mood.isNullOrEmpty()) {
                             Box(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)) // Fixed
        .padding(horizontal = 6.dp, vertical = 0.dp)
) {
    Text(
        text = session.mood.uppercase(),
        color = MaterialTheme.colorScheme.onPrimary, // Fixed: Changed to onPrimary for readability
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )
}
                            }
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = session.format,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Metadata Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Book Metadata",
                        color = AmberWarm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        metadataRow("Publisher", session.publisher, "Published", session.publishedDate)
                        metadataRow("Updated", session.updatedDate, "Added", session.addedDate)
                        metadataRow("Language", "English", "File Size", session.fileSize)
                        metadataRowSingle("Subjects", session.subjects)
                    }
                }

                // Description Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Description",
                        color = AmberWarm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = session.descriptionText,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Integrations Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Integerations",
                        color = AmberWarm,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
               Column(
    modifier = Modifier
        .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    integrationItem("Calibre Companion", "Connected")
    integrationItem("Goodreads Sync", "Not Configured")
}
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSheet(
    showBrightnessSheet: Boolean,
    onDismiss: () -> Unit,
    brightnessValue: Int,
    onBrightnessChange: (Int) -> Unit,
    activeColorTheme: String,
    onColorThemeChange: (String) -> Unit,
    autoBrightness: Boolean,
    onAutoBrightnessChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    if (showBrightnessSheet) {
        val AmberWarm = Color(0xFFE8883A)
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
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Title
                Text(
                    text = "Brightness & Theme",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Brightness Slider Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )

                    Slider(
                        value = brightnessValue.toFloat().coerceIn(0f, 100f),
                        onValueChange = { onBrightnessChange(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = AmberWarm,
                            inactiveTrackColor = Color(0xFF242424),
                            thumbColor = AmberWarm
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Auto Brightness Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Brightness", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Switch(
                        checked = autoBrightness,
                        onCheckedChange = onAutoBrightnessChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF141414),
                            checkedTrackColor = AmberWarm,
                            uncheckedThumbColor = Color(0xFF888888),
                            uncheckedTrackColor = Color(0xFF242424)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFF242424))
Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Color", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
    
    val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
    val themes = if (isDarkMode) {
        listOf("Default", "Gray", "Contrast", "Grass", "Cherry", "Gruvbox", "Nord", "Sunset")
    } else {
        listOf("Light", "Paper", "Sepia", "Solarized", "E-Ink")
    }

    // FlowRow automatically wraps items into multiple rows
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4 // Optional: keeps exactly 3-4 items per row nicely spaced
    ) {
        themes.forEach { cName ->
            val isSelected = activeColorTheme == cName
            
            // Calculate actual color for the card background
            val itemBgColor = when (cName) {
                "Sepia" -> Color(0xFFF4ECD8)
                "Contrast" -> Color(0xFF000000)
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

            // Decide text color dynamically so it's readable on both dark and light cards
            val textColor = when (cName) {
                "Sepia", "Light", "Paper", "Solarized", "E-Ink" -> Color(0xFF1E1E1E)
                "Contrast" -> Color.White
                else -> Color(0xFFEDEDED)
            }

            Box(
                modifier = Modifier
                    // Use a fraction of the row width instead of weight(1f) so items look uniform across rows
                    .fillMaxWidth(0.22f) 
                    .clip(RoundedCornerShape(8.dp))
                    // Shows the actual theme color directly inside the card background
                    .background(itemBgColor)
                    // Highlights the active item with a clear border
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) AmberWarm else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onColorThemeChange(cName) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cName,
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}



// Helpers
@Composable
fun metadataRow(key1: String, val1: String, key2: String, val2: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(key1.uppercase(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 11.sp)
            Text(val1, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(key2.uppercase(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 11.sp)
            Text(val2, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun metadataRowSingle(key: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(key.uppercase(), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontSize = 11.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun integrationItem(name: String, status: String, isPremium: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),  
            RoundedCornerShape(12.dp))            
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            if (isPremium) {
                Box(modifier = Modifier.background(Color(0xFF5DADE2).copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                    Text("PRO", color = Color(0xFF5DADE2), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(status, color = if (status == "Connected") Color(0xFF61D395) else Color.Gray, fontSize = 14.sp)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFontSheet(
    showQuickFontSheet: Boolean,
    onDismiss: () -> Unit,
    defaultFontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    lineSpacing: Float,
    onLineSpacingChange: (Float) -> Unit,
    paragraphMargin: Float,
    onParagraphMarginChange: (Float) -> Unit,
    letterSpacing: Int,
    onLetterSpacingChange: (Int) -> Unit
) {
        val scheme = MaterialTheme.colorScheme

    if (showQuickFontSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
           containerColor = scheme.surfaceVariant,
            dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Text & Layout Spacing", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                // Font size controls row with interactive Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Text("${defaultFontSize} sp", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("A", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), fontSize = 12.sp)
                        Slider(
                            value = defaultFontSize.toFloat(),
                            onValueChange = { onFontSizeChange(it.toInt()) },
                            valueRange = 10f..42f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFE8883A), activeTrackColor = Color(0xFFE8883A))
                        )
                        Text("A", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Line spacing settings row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Line Spacing",  color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Small", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 12.sp)
                        IconButton(onClick = { onLineSpacingChange((lineSpacing - 0.1f).coerceIn(0.5f, 3.0f)) }) {
                            Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(String.format("%.1f", lineSpacing), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onLineSpacingChange((lineSpacing + 0.1f).coerceIn(0.5f, 3.0f)) }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Large", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }

                // Paragraph spacing row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Paragraph Margin",  color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Small", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 12.sp)
                        IconButton(onClick = { onParagraphMarginChange((paragraphMargin - 0.1f).coerceIn(0.1f, 4.0f)) }) {
                            Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(String.format("%.1f", paragraphMargin), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onParagraphMarginChange((paragraphMargin + 0.1f).coerceIn(0.1f, 4.0f)) }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Large", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }

                // Letter spacing row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) , RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Letter Spacing", color = MaterialTheme.colorScheme.primary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconButton(onClick = { onLetterSpacingChange((letterSpacing - 1).coerceIn(-5, 20)) }) {
                            Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(letterSpacing.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onLetterSpacingChange((letterSpacing + 1).coerceIn(-5, 20)) }) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSeekSheet(
    showQuickSeekSheet: Boolean,
    onDismiss: () -> Unit,
    session: ReaderSessionState,
    onProgressChange: (Int) -> Unit,
    onChapterChange: (Int) -> Unit,
    onSeekToParagraph: (Int) -> Unit = {}
) {
        
    val scheme = MaterialTheme.colorScheme

    if (showQuickSeekSheet) {
        // com.creativeblisslabs.omnimind.core.ui.util.ImmersiveSystemBarsEffect(enable = true)
        var sliderPos by remember(session.progress) { mutableStateOf(session.progress.toFloat() / 100f) }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = scheme.surfaceVariant,
            dragHandle = { BottomSheetDefaults.DragHandle(color = scheme.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Reading Navigation", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }

                // Slider seekbar row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("${(sliderPos * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Slider(
    value = sliderPos,
    onValueChange = { newProgress ->
        sliderPos = newProgress
        val p = (newProgress * 100).toInt()
        onProgressChange(p)
    },
    onValueChangeFinished = {
        // Optionally trigger scroll here
    },
    modifier = Modifier.weight(1f),
    colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.onSurface,       // Fixed camelCase
        activeTrackColor = MaterialTheme.colorScheme.onSurface, // Fixed camelCase
        inactiveTrackColor = Color(0xFF333333)
    )
)
                }

                // Row of Navigation controls: « < ↺ ↻ > »
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // « Previous Chapter
                    IconButton(onClick = {
                        if (session.currentChapter > 0) {
                            onChapterChange(session.currentChapter - 1)
                        }
                    }) {
                        Text("«", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // < Previous Page / Sim progress decrease
                    IconButton(onClick = {
                        val newP = (session.progress - 5).coerceAtLeast(0)
                        onProgressChange(newP)
                    }) {
                        Text("<", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // ↺ Reset/Reload chapter
                    IconButton(onClick = {
                        onProgressChange(0)
                    }) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    }

                    // ↻ Reload page
                    IconButton(onClick = {
                        onProgressChange(session.progress)
                    }) {
                        Icon(Icons.Default.Cached, null, tint = MaterialTheme.colorScheme.primary)
                    }

                    // > Next Page / Sim progress increase
                    IconButton(onClick = {
                        val newP = (session.progress + 5).coerceAtMost(100)
                        onProgressChange(newP)
                    }) {
                        Text(">", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    // » Next Chapter
                    IconButton(onClick = {
                        if (session.currentChapter < session.chapters.size - 1) {
                            onChapterChange(session.currentChapter + 1)
                        }
                    }) {
                        Text("»", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationDialog(
    show: Boolean,
    sourceText: String,
    translatedText: String,
    isTranslating: Boolean,
    onDismiss: () -> Unit,
    onTranslate: (targetLang: String) -> Unit,
    onCopy: (String) -> Unit
) {
    if (!show) return
    val scheme = MaterialTheme.colorScheme
    val AmberWarm = Color(0xFFE8883A)
    val languages = listOf("English", "Spanish", "French", "German", "Hindi", "Chinese", "Japanese", "Russian", "Arabic", "Portuguese", "Italian")
    var selectedLang by remember { mutableStateOf("English") }
    var langMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedLang, sourceText) {
        if (sourceText.isNotBlank()) {
            onTranslate(selectedLang)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = scheme.primary)
                        Text("Translation", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = scheme.onSurface)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = scheme.onSurfaceVariant)
                    }
                }

                // Target Language selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Translate to:", fontSize = 14.sp, color = scheme.onSurfaceVariant)
                    Box {
                        OutlinedButton(
                            onClick = { langMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(selectedLang, color = scheme.primary, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = scheme.primary)
                        }
                        DropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLang = lang
                                        langMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Original Text Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(12.dp)
                ) {
                    Text("Original", fontSize = 11.sp, color = scheme.primary.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = sourceText.ifBlank { "No text selected" },
                        fontSize = 13.sp,
                        color = scheme.onSurface,
                        maxLines = 4,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                // Translated Text Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(12.dp)
                ) {
                    Text("Translated ($selectedLang)", fontSize = 11.sp, color = AmberWarm, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    if (isTranslating) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AmberWarm, strokeWidth = 2.dp)
                            Text("Translating text...", fontSize = 13.sp, color = scheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            text = translatedText.ifBlank { "Translating..." },
                            fontSize = 14.sp,
                            color = scheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onCopy(translatedText) },
                        enabled = translatedText.isNotBlank() && !isTranslating
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Translation")
                    }
                }
            }
        }
    }
}


