package com.creativeblisslabs.omnimind.feature.reader

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import com.creativeblisslabs.omnimind.core.media.MediaAttachmentUi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.creativeblisslabs.omnimind.core.media.rememberMediaActions
import com.creativeblisslabs.omnimind.core.navigation.Screen
import com.creativeblisslabs.omnimind.core.ui.theme.*
import kotlinx.coroutines.IO
import org.koin.compose.viewmodel.koinViewModel

import com.creativeblisslabs.omnimind.core.ui.components.GradientBackground
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderHubScreen(
    navController: NavController,
    viewModel: ReaderHubViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        LibraryStore.initDatabase(viewModel.bookDao, this)
    }

    // Dropdowns visibility
    var showMoreMenu by remember { mutableStateOf(false) }
    var showMainMenu by remember { mutableStateOf(false) }

    // Sort/Grid state variables
    var isGridView by remember { mutableStateOf(true) }
    var columnCount by remember { mutableStateOf(3) }
    var coverCropFit by remember { mutableStateOf("Crop") }
    var groupBySetting by remember { mutableStateOf("Groups") }
    var sortBySetting by remember { mutableStateOf("Date Read") }
    var sortAscending by remember { mutableStateOf(false) }
    var thenBySetting by remember { mutableStateOf("None") }

    // Main Menu options
    var autoUploadCloud by remember { mutableStateOf(true) }
    var autoImportFile by remember { mutableStateOf(true) }
    var openLastBookOnStart by remember { mutableStateOf(false) }
    var alwaysShowStatusBar by remember { mutableStateOf(false) }

    var pendingPasswordAttachment by remember { mutableStateOf<MediaAttachmentUi?>(null) }
    var passwordInputText by remember { mutableStateOf("") }
    var isPasswordError by remember { mutableStateOf(false) }

    val mediaActions = rememberMediaActions(
        onImagePicked = {},
        onFilePicked = { attachment ->
            viewModel.importBook(
                attachment = attachment,
                onPasswordRequired = { path, name ->
                    pendingPasswordAttachment = attachment
                    passwordInputText = ""
                    isPasswordError = false
                }
            ) { importedBook, chunks ->
                ReaderSessionStore.loadBook(importedBook, chunks)
                navController.navigate(Screen.BookReader.route)
            }
        },
        onAudioPicked = {},
        onClipboardImage = {}
    )

    if (pendingPasswordAttachment != null) {
        AlertDialog(
            onDismissRequest = {
                pendingPasswordAttachment = null
                passwordInputText = ""
                isPasswordError = false
            },
            icon = {
                Icon(Icons.Default.Lock, contentDescription = "Encrypted PDF", tint = AmberWarm)
            },
            title = {
                Text("Password Required", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This PDF file is encrypted. Enter the document password to unlock and import it:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = passwordInputText,
                        onValueChange = {
                            passwordInputText = it
                            isPasswordError = false
                        },
                        singleLine = true,
                        isError = isPasswordError,
                        placeholder = { Text("Enter password...") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isPasswordError) {
                        Text("Incorrect password or decryption failed. Try again.", color = Color.Red, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val att = pendingPasswordAttachment ?: return@Button
                        viewModel.importBook(
                            attachment = att,
                            password = passwordInputText,
                            onPasswordRequired = { _, _ ->
                                isPasswordError = true
                            }
                        ) { importedBook, chunks ->
                            pendingPasswordAttachment = null
                            ReaderSessionStore.loadBook(importedBook, chunks)
                            navController.navigate(Screen.BookReader.route)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarm)
                ) {
                    Text("Unlock & Import", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingPasswordAttachment = null
                    passwordInputText = ""
                    isPasswordError = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    var selectedBookIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedBookIds.isNotEmpty()

    var selectedBookForDetails by remember { mutableStateOf<LibraryBook?>(null) }
    val scheme = androidx.compose.material3.MaterialTheme.colorScheme


    val displayedBooks = remember(LibraryStore.books, searchQuery, sortBySetting, sortAscending, groupBySetting) {
        var list = LibraryStore.books
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.author.contains(searchQuery, ignoreCase = true)
            }
        }
        if (groupBySetting == "Authors") {
            list = list.sortedBy { it.author.lowercase() }
        }
        val sorted = when (sortBySetting) {
            "Title" -> list.sortedBy { it.title.lowercase() }
            "Author" -> list.sortedBy { it.author.lowercase() }
            "Progress Read" -> list.sortedBy { it.progress }
            else -> list
        }
        if (!sortAscending) sorted.reversed() else sorted
    }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { paddingValues ->
                GradientBackground(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top search and actions bar or Batch Selection Bar
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF2B1A1A))
                        .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { selectedBookIds = emptySet() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Cancel Selection", tint = scheme.onSurfaceVariant)
                        }
                        Text("${selectedBookIds.size} Selected", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val selectedBooks = LibraryStore.books.filter { selectedBookIds.contains(it.id) }
                                selectedBooks.forEach { LibraryStore.deleteBook(it, viewModel.bookDao, scope, viewModel.bookChunkDao) }
                                selectedBookIds = emptySet()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, "Delete Selected", tint = Color.Red)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF161616).copy(alpha = 0.3f))
                            .border(1.dp, Color(0xFF2C2C2C).copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search library books...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.5f)
                                .width(1.dp)
                                .background(Color(0xFF2C2C2C).copy(alpha = 0.2f))
                        )
                        IconButton(
                            onClick = { mediaActions.pickFile() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Import",
                                tint = AmberWarm,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // More Options Menu
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = !showMoreMenu },
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFF161616).copy(alpha = 0.3f), RoundedCornerShape(23.dp))
                                .border(1.dp, Color(0xFF2C2C2C).copy(alpha = 0.15f), RoundedCornerShape(23.dp))
                        ) {
                            Icon(Icons.Default.MoreHoriz, null, tint = scheme.onSurface, modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .width(240.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("List View", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (!isGridView) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { isGridView = false; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Grid View", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (isGridView) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { isGridView = true; showMoreMenu = false }
                            )
                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            Text("Columns", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Auto Columns", color = MaterialTheme.colorScheme.onSurface)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(columnCount.toString(), color = MaterialTheme.colorScheme.onSurface)
                                            IconButton(onClick = { columnCount = (columnCount - 1).coerceAtLeast(1) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Remove, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(onClick = { columnCount = (columnCount + 1).coerceAtMost(5) }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Add, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                },
                                onClick = {}
                            )
                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            Text("Book Covers", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Crop to Card", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (coverCropFit == "Crop") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { coverCropFit = "Crop"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit Entire Cover", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (coverCropFit == "Fit") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { coverCropFit = "Fit"; showMoreMenu = false }
                            )

                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            Text("Group by...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Authors", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (groupBySetting == "Authors") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { groupBySetting = "Authors"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Books", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (groupBySetting == "Books") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { groupBySetting = "Books"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Groups", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (groupBySetting == "Groups") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { groupBySetting = "Groups"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Series", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (groupBySetting == "Series") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { groupBySetting = "Series"; showMoreMenu = false }
                            )

                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            Text("Sort by...", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Title", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (sortBySetting == "Title") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { sortBySetting = "Title"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Author", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (sortBySetting == "Author") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { sortBySetting = "Author"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Date Read", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (sortBySetting == "Date Read") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { sortBySetting = "Date Read"; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Progress Read", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (sortBySetting == "Progress Read") { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { sortBySetting = "Progress Read"; showMoreMenu = false }
                            )

                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            DropdownMenuItem(
                                text = { Text("Ascending", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (sortAscending) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { sortAscending = true; showMoreMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Descending", color = MaterialTheme.colorScheme.onSurface) },
                                trailingIcon = if (!sortAscending) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { sortAscending = false; showMoreMenu = false }
                            )
                        }
                    }

                    // Main Navigation/Settings Menu
                    Box {
                        IconButton(
                            onClick = { showMainMenu = !showMainMenu },
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFF161616).copy(alpha = 0.3f), RoundedCornerShape(23.dp))
                                .border(1.dp, Color(0xFF2C2C2C).copy(alpha = 0.15f), RoundedCornerShape(23.dp))
                        ) {
                            Icon(Icons.Default.Menu, null, tint = scheme.onSurface, modifier = Modifier.size(20.dp))
                        }

                        DropdownMenu(
                            expanded = showMainMenu,
                            onDismissRequest = { showMainMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .width(220.dp)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Person, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Text("Sync & Account", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    showMainMenu = false
                                }
                            )
                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            DropdownMenuItem(
                                text = { Text("Auto Sync Books", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) },
                                trailingIcon = if (autoUploadCloud) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { autoUploadCloud = !autoUploadCloud }
                            )
                            DropdownMenuItem(
                                text = { Text("Auto Import on Open", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) },
                                trailingIcon = if (autoImportFile) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { autoImportFile = !autoImportFile }
                            )
                            DropdownMenuItem(
                                text = { Text("Open Last Book on Start", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp) },
                                trailingIcon = if (openLastBookOnStart) { { Icon(Icons.Default.Check, "Selected", tint = AmberWarm, modifier = Modifier.size(18.dp)) } } else null,
                                onClick = { openLastBookOnStart = !openLastBookOnStart }
                            )
                            HorizontalDivider(color = Color(0xFF2C2C2C))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Settings, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Text("Reader Settings", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    showMainMenu = false
                                    navController.navigate(Screen.ReaderFont.route)
                                }
                            )
                        }
                    }
                }
            }

            // Conditional View: Display Empty Library State or Grid of Books
            if (displayedBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF161616).copy(alpha = 0.3f))
                        .border(1.dp, Color(0xFF2C2C2C).copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(AmberWarm.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Empty Library",
                                tint = AmberWarm,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotBlank()) "No Matching Books Found" else "Your Library is Empty",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotBlank())
                                "Try searching for a different title or author keyword."
                            else
                                "Import your favorite EPUB, PDF, or text documents to start your immersive reading experience.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { mediaActions.pickFile() },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarm),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import Document", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    BookItemGrid(
                        books = displayedBooks,
                        navController = navController,
                        mediaActions = mediaActions,
                        viewModel = viewModel,
                        columnCount = if (isGridView) columnCount else 1,
                        selectedBookIds = selectedBookIds,
                        onToggleSelect = { id ->
                            selectedBookIds = if (selectedBookIds.contains(id)) selectedBookIds - id else selectedBookIds + id
                        },
                        onBookLongClick = { book -> selectedBookForDetails = book }
                    )
                }
            }

            if (selectedBookForDetails != null) {
                val book = selectedBookForDetails!!
                BookDetailsSheet(
                    showBookDetailsSheet = true,
                    onDismiss = { selectedBookForDetails = null },
                    session = ReaderSessionState(
                        title = book.title,
                        author = book.author,
                        progress = book.progress,
                        bookId = book.id
                    )
                )
            }

            if (viewModel.isImportingBook) {
                androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1E22))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { viewModel.importProgress },
                                color = AmberWarm,
                                modifier = Modifier.size(44.dp)
                            )
                            Text("Importing Book...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Parsing ${viewModel.importBookName} metadata and chapters...", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center)

                            LinearProgressIndicator(
                                progress = { viewModel.importProgress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = AmberWarm,
                                trackColor = Color(0xFF333333)
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookItemGrid(
    books: List<LibraryBook>,
    navController: NavController,
    mediaActions: com.creativeblisslabs.omnimind.core.media.MediaActions,
    viewModel: ReaderHubViewModel,
    columnCount: Int = 3,
    coverCropFit: String = "Crop",
    selectedBookIds: Set<String> = emptySet(),
    onToggleSelect: (String) -> Unit = {},
    onBookLongClick: (LibraryBook) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val isSelectionMode = selectedBookIds.isNotEmpty()
    val scheme = MaterialTheme.colorScheme


    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount.coerceIn(1, 5)),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(books) { book ->
            val isSelected = selectedBookIds.contains(book.id)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelect(book.id)
                            } else {
                                viewModel.updateProgress(book.id, book.progress, 0)
                                scope.launch {
                                    val chunkEntities = withContext(Dispatchers.IO) {
                                        viewModel.bookChunkDao.getChunksListForBook(book.id)
                                    }
                                    val chunkTexts = chunkEntities.map { it.text }
                                    println("READER_LOG: ReaderHubScreen onClick fetched ${chunkEntities.size} chunks for bookId=${book.id}")
                                    if (chunkTexts.isNotEmpty()) {
                                        ReaderSessionStore.loadBook(book, chunkTexts)
                                    }
                                    viewModel.loadBookChunks(book.id)
                                    navController.navigate(Screen.BookReader.route)
                                }
                            }
                        },
                        onLongClick = {
                            onToggleSelect(book.id)
                            onBookLongClick(book)
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) AmberWarm else Color(0xFF2C2C2C), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.coverImagePath.isNotBlank()) {
                        coil3.compose.AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = book.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (isSelected) AmberWarm else AmberWarm.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = book.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = book.author,
                                color = Color(0xFF888888),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AmberWarm),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (book.isFinished) Color(0xFF1B3D2B) else Color(0xFF2D3E50))
                            .padding(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (book.isFinished) "FINISHED" else "${book.progress}% READ",
                            color = if (book.isFinished) Color(0xFF61D395) else Color(0xFF5DADE2),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Sync Cloud",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Dotted Add/Import Box Card
        item {
            val strokeColor = Color(0xFF333333)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .drawBehind {
                        drawRoundRect(
                            color = strokeColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            ),
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        )
                    }
                    .clickable { mediaActions.pickFile() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Import book",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Add Book",
                        color = Color(0xFF666666),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}