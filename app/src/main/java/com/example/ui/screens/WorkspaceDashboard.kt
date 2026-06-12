package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.data.db.ChatMessageEntity
import com.example.data.db.NoteEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.WorkspaceViewModel
import com.example.ui.components.MarkdownText
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkspaceDashboard(
    viewModel: WorkspaceViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.filteredNotesList.collectAsStateWithLifecycle()
    val chats by viewModel.chatMessages.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSpaceBg),
        containerColor = DeepSpaceBg,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header with App Title and Custom Spark indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(AccentTeal, TextPurple)
                                    )
                                )
                                .testTag("app_logo"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Spark",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ThinkSpace AI",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Tombol siklus tema dinamis: Sistem -> Terang -> Gelap -> Sistem
                        IconButton(
                            onClick = {
                                viewModel.themeMode = when (viewModel.themeMode) {
                                    "System" -> "Light"
                                    "Light" -> "Dark"
                                    else -> "System"
                                }
                            }
                        ) {
                            val themeIcon = when (viewModel.themeMode) {
                                "Light" -> Icons.Default.PlayArrow
                                "Dark" -> Icons.Default.Favorite
                                else -> Icons.Default.Settings
                            }
                            val themeLabel = when (viewModel.themeMode) {
                                "Light" -> "Terang"
                                "Dark" -> "Gelap"
                                else -> "Sistem"
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = "Simbol Tema",
                                    tint = AccentTealBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = themeLabel,
                                    fontSize = 8.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Informasi Keamanan & Tip Kunci API
                        IconButton(
                            onClick = {
                                // Aksi detail singkat
                            },
                            modifier = Modifier.testTag("info_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Project Details",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Rounded Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SlateCard)
                        .padding(4.dp)
                ) {
                    // Notes Tab Option
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (viewModel.activeTab == "notes") SlateCardBorder else Color.Transparent)
                            .clickable { viewModel.activeTab = "notes" }
                            .testTag("tab_notes"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Notes list",
                            tint = if (viewModel.activeTab == "notes") AccentTealBright else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Insights Hub",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (viewModel.activeTab == "notes") TextPrimary else TextMuted
                        )
                    }

                    // Chat Tab Option
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (viewModel.activeTab == "ai_chat") SlateCardBorder else Color.Transparent)
                            .clickable { viewModel.activeTab = "ai_chat" }
                            .testTag("tab_ai_chat"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "MindSpace Chat",
                            tint = if (viewModel.activeTab == "ai_chat") TextPurple else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MindSpace Chat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (viewModel.activeTab == "ai_chat") TextPrimary else TextMuted
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Edge-to-edge safe area navigation spacing
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sliding navigation tab render
            AnimatedContent(
                targetState = viewModel.activeTab,
                transitionSpec = {
                    if (targetState == "ai_chat") {
                        slideInHorizontally { width -> width }.plus(fadeIn()) with
                                slideOutHorizontally { width -> -width }.plus(fadeOut())
                    } else {
                        slideInHorizontally { width -> -width }.plus(fadeIn()) with
                                slideOutHorizontally { width -> width }.plus(fadeOut())
                    }.using(SizeTransform(clip = false))
                },
                label = "tab_animation"
            ) { tab ->
                when (tab) {
                    "notes" -> {
                        NotesTabContent(
                            notes = notes,
                            searchQuery = searchQuery,
                            onQueryChange = { viewModel.searchQuery.value = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { viewModel.selectedCategory.value = it },
                            viewModel = viewModel
                        )
                    }
                    "ai_chat" -> {
                        ChatTabContent(
                            chats = chats,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Floating creation button for Notes tab
            if (viewModel.activeTab == "notes") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    LargeFloatingActionButton(
                        onClick = { viewModel.openNoteEditor() },
                        containerColor = AccentTeal,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .testTag("add_note_fab")
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add insights note",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet Editor Dialog for note editing or viewing
    if (viewModel.isEditorVisible) {
        NoteEditorDialog(viewModel = viewModel)
    }
}

@Composable
fun NotesTabContent(
    notes: List<NoteEntity>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    viewModel: WorkspaceViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Mengamati daftar kategori dinamis dari ViewModel
    val categories by viewModel.categoriesList.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Kotak Pencarian Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("notes_search_input"),
            placeholder = { Text("Telusuri catatan atau kata kunci...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = TextMuted
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = TextMuted
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateCard,
                unfocusedContainerColor = SlateCard,
                focusedBorderColor = AccentTeal,
                unfocusedBorderColor = SlateCardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = AccentTeal
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboardController?.hide()
                focusManager.clearFocus()
            })
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Pemilih Kategori Dinamis secara Horizontal
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) AccentTeal else SlateCard)
                        .border(
                            1.dp,
                            if (isSelected) AccentTealBright else SlateCardBorder,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onCategoryChange(category) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("category_chip_$category"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) Color.White else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Kontrol Penyortiran dan Pengalih Tampilan Grid / List
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Urutkan:",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                listOf("Terbaru", "Terlama", "A-Z").forEach { sortOption ->
                    val isSelected = sortBy == sortOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AccentTeal.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                0.5.dp,
                                if (isSelected) AccentTealBright else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.sortBy.value = sortOption }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = sortOption,
                            color = if (isSelected) AccentTealBright else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tombol Switcher Grid / List
            IconButton(
                onClick = { viewModel.isGridView = !viewModel.isGridView },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.isGridView) Icons.Default.List else Icons.Default.Menu,
                    contentDescription = "Toggle Tampilan",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rendering Tumpukan Catatan (Notes list/grid stack)
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tidak ada data",
                        tint = TextPurple.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Belum Ada Catatan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "Tidak ada catatan yang cocok dengan pencarian Anda."
                        else "Ketuk '+ di pojok bawah untuk membuat catatan baru atau merekomendasikan tag pintar via AI.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        } else {
            if (viewModel.isGridView) {
                val chunkedNotes = notes.chunked(2)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_grid"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(chunkedNotes) { rowNotes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowNotes.forEach { note ->
                                Box(modifier = Modifier.weight(1f)) {
                                    NoteCard(note = note, viewModel = viewModel)
                                }
                            }
                            if (rowNotes.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("notes_list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note = note, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(note: NoteEntity, viewModel: WorkspaceViewModel) {
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            viewModel.exportNoteToUri(context, uri, note)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.openNoteEditor(note) }
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (note.isPinned) AccentTeal.copy(alpha = 0.8f) // Highlight warna khusus jika disematkan (pinned)
            else if (viewModel.isAnalyzingNoteId == note.id) AccentTeal
            else SlateCardBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Baris Atas: Kategori, Tombol Pin, Tombol Favorit, Tombol Hapus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge Kategori
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentTeal.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = note.category,
                            color = AccentTealBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tampilkan indikator "Tersemat" secara visual
                    if (note.isPinned) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentTealBright.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "📌 Pinned",
                                color = AccentTealBright,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Tombol Sematkan (Pin)
                    IconButton(
                        onClick = { viewModel.toggleNotePin(note) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place, // Place bertindak sebagai simbol pin lokasi/sematan
                            contentDescription = "Sematkan catatan",
                            tint = if (note.isPinned) AccentTealBright else TextMuted.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol Favorit (Bintang/Hati)
                    IconButton(
                        onClick = { viewModel.toggleNoteFavorite(note) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritkan catatan",
                            tint = if (note.isFavorite) AccentRose else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol Ekspor (Markdown)
                    IconButton(
                        onClick = {
                            val cleanTitle = note.title.ifBlank { "Catatan" }.replace("[\\s\\\\/:*?\"<>|]+".toRegex(), "_")
                            exportLauncher.launch("$cleanTitle.md")
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Ekspor catatan ini ke format Markdown",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tombol Hapus (Sampah)
                    IconButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus catatan",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Judul dan Isi Konten Catatan
            Text(
                text = note.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            MarkdownText(
                markdown = note.content,
                baseColor = TextMuted,
                baseFontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Menampilkan Tag Pintar AI jika tersedia
            if (!note.tags.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    note.tags.split(",").forEach { tag ->
                        if (tag.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TextPurple.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "#${tag.trim()}",
                                    color = TextPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Kotak Ringkasan AI Pintar jika tersedia setelah ekstraksi analisa dijalankan
            if (note.aiSummary != null || note.aiKeyPoints != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SlateCardBorder, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TextPurple.copy(alpha = 0.08f))
                        .border(0.5.dp, TextPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "AI sparkles",
                            tint = TextPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "AURA SMART INSIGHT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPurple,
                            letterSpacing = 1.sp
                        )
                    }

                    if (note.aiSummary != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.aiSummary,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (note.aiKeyPoints != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val bulletPoints = note.aiKeyPoints.split(";;").filter { it.isNotBlank() }
                        bulletPoints.take(2).forEach { point ->
                            Row(
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("•", color = TextPurple, fontSize = 11.sp)
                                Text(
                                    text = point.trim().removePrefix("-").trim(),
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Pengontrol Riwayat Pengeditan Bawah
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Format Waktu yang indah
                val formattedTime = remember(note.timestamp) {
                    val date = java.util.Date(note.timestamp)
                    val format = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                    format.format(date)
                }

                Text(
                    text = "Edited $formattedTime",
                    fontSize = 11.sp,
                    color = TextMuted.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )

                // Tombol Ekstrak Ringkasan AI Cepat jika belum ada dokumen analitis
                if (note.aiSummary == null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (viewModel.isAnalyzingNoteId == note.id) SlateCardBorder else TextPurple.copy(alpha = 0.15f))
                            .clickable(enabled = viewModel.isAnalyzingNoteId != note.id) {
                                viewModel.analyzeNoteWithAI(note.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (viewModel.isAnalyzingNoteId == note.id) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                color = TextPurple,
                                strokeWidth = 1.dp
                            )
                            Text(
                                text = "Running AI...",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "AI Spark",
                                tint = TextPurple,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Aura Analyze",
                                fontSize = 11.sp,
                                color = TextPurple,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Delete Dialog confirmation
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete insight thought?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently discard the note and its generated smart insights.", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
                        confirmDelete = false
                    }
                ) {
                    Text("Discard", color = AccentRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = SlateCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChatTabContent(
    chats: List<ChatMessageEntity>,
    viewModel: WorkspaceViewModel
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto scroll bottom when active response arrives
    LaunchedEffect(chats.size) {
        if (chats.isNotEmpty()) {
            scrollState.animateScrollToItem(chats.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Chat list timeline scroll content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(TextPurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "AI Sparkle",
                                tint = TextPurple,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Meet MindSpace AI",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Begin an offline dialogue thread with our model companion. Ask to brainstorm outlines, summarize complex topics, or draft layouts.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_messages_list"),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
                ) {
                    item {
                        // Compact diagnostic tip bar for local simulation warning
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SlateCard)
                                .border(0.5.dp, SlateCardBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Note information",
                                    tint = AccentTealBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "MindSpace Sandbox Engine",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Your conversation threads are stored locally in Room. " +
                                                "Active model operations leverage Google Gemini API on-device.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(chats, key = { it.id }) { chat ->
                        val isUser = chat.sender == "user"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            // Chat speech bubble wrapper with modern organic shape asymmetry
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        )
                                    )
                                    .background(
                                        if (isUser) {
                                            Brush.linearGradient(
                                                colors = listOf(AccentTeal, TextPurple)
                                            )
                                        } else {
                                            SolidColor(SlateCard)
                                        }
                                    )
                                    .let {
                                        if (!isUser) {
                                            it.border(0.5.dp, SlateCardBorder, RoundedCornerShape(
                                                topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp
                                            ))
                                        } else it
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = chat.text,
                                    fontSize = 13.sp,
                                    color = if (isUser) Color.White else TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }

                            // Meta timestamp details
                            val formattedTime = remember(chat.timestamp) {
                                val date = java.util.Date(chat.timestamp)
                                val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                format.format(date)
                            }
                            Text(
                                text = (if (isUser) "You" else "MindSpace") + " • $formattedTime",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Floating loader dots
                    if (viewModel.isAILoading) {
                        item {
                            AILoadingBubble()
                        }
                    }
                }
            }
        }

        // Input Tray Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Context Clear Dialog button
            if (chats.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearChatHistory() },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = SlateCard),
                    modifier = Modifier
                        .size(48.dp)
                        .border(0.5.dp, SlateCardBorder, RoundedCornerShape(12.dp))
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Clear thread",
                        tint = TextMuted
                    )
                }
            }

            // Input TextField
            OutlinedTextField(
                value = viewModel.chatInputText,
                onValueChange = { viewModel.chatInputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text"),
                placeholder = { Text("Send prompts to Aura...", color = TextMuted, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateCard,
                    unfocusedContainerColor = SlateCard,
                    focusedBorderColor = AccentTeal,
                    unfocusedBorderColor = SlateCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentTeal
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    viewModel.sendChatMessage()
                    keyboardController?.hide()
                })
            )

            // Submit Send Floating indicator
            IconButton(
                onClick = {
                    viewModel.sendChatMessage()
                    keyboardController?.hide()
                },
                enabled = viewModel.chatInputText.isNotBlank() && !viewModel.isAILoading,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = AccentTeal,
                    disabledContainerColor = SlateCard
                ),
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        0.5.dp,
                        if (viewModel.chatInputText.isNotBlank()) AccentTealBright else SlateCardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("send_chat_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit chat",
                    tint = if (viewModel.chatInputText.isNotBlank()) Color.White else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AILoadingBubble() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(SlateCard)
                    .border(0.5.dp, SlateCardBorder, RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp
                    ))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(TextPurple.copy(alpha = alpha1)))
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(TextPurple.copy(alpha = alpha2)))
                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(TextPurple.copy(alpha = alpha3)))
                }
            }
            Text(
                text = "MindSpace is formulating thoughts...",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun AssistantChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SlateCardBorder)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = AccentTealBright,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NoteEditorDialog(viewModel: WorkspaceViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isPreviewMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { viewModel.closeNoteEditor() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceBg),
            containerColor = DeepSpaceBg,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.closeNoteEditor() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close editor",
                                tint = TextPrimary
                            )
                        }
                        Text(
                            text = if (viewModel.noteBeingEdited == null) "Buat Catatan" else "Edit Catatan",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Tombol Simpan
                    Button(
                        onClick = {
                            viewModel.saveNote()
                            keyboardController?.hide()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("save_note_button")
                    ) {
                        Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Pemilih Kategori Utama
                Text(
                    text = "KATEGORI",
                    fontSize = 11.sp,
                    color = TextPurple,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val categories = listOf("Notes", "Ideas", "Tasks", "Personal")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { category ->
                        val isSelected = viewModel.editorCategory == category
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentTeal else SlateCard)
                                .border(
                                    0.5.dp,
                                    if (isSelected) AccentTealBright else SlateCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.editorCategory = category }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // BILAH ALAT ASISTEN AI (Aura AI Assistant Toolbar)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, SlateCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "AI Sparkle",
                                tint = TextPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "AURA AI ASISTEN PINTAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPurple,
                                letterSpacing = 1.sp
                            )
                            if (viewModel.isAIOperationsLoading) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = TextPurple,
                                    strokeWidth = 1.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Berbagai Fitur AI Berdasarkan Konteks Teks
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                AssistantChip(
                                    icon = Icons.Default.Edit,
                                    label = "Lanjutkan Tulisan",
                                    onClick = { viewModel.continueWritingInEditor() },
                                    enabled = !viewModel.isAIOperationsLoading
                                )
                            }
                            item {
                                AssistantChip(
                                    icon = Icons.Default.Refresh,
                                    label = "Buat Judul Otomatis",
                                    onClick = { viewModel.generateTitleForEditor() },
                                    enabled = !viewModel.isAIOperationsLoading
                                )
                            }
                            item {
                                AssistantChip(
                                    icon = Icons.Default.Check,
                                    label = "Saran Tag Pintar",
                                    onClick = { viewModel.generateTagsForEditor() },
                                    enabled = !viewModel.isAIOperationsLoading
                                )
                            }
                            item {
                                AssistantChip(
                                    icon = Icons.Default.List,
                                    label = "Meringkas Teks",
                                    onClick = { viewModel.summarizeEditorContent() },
                                    enabled = !viewModel.isAIOperationsLoading
                                )
                            }
                        }

                        // Fitur AI Penerjemah (Translate)
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = SlateCardBorder, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Terjemahkan ke:",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            listOf(
                                "Inggris" to "English",
                                "Sunda" to "Sundanese",
                                "Jawa" to "Javanese",
                                "Bali" to "Balinese",
                                "Arab" to "Arabic"
                            ).forEach { (label, lang) ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SlateCardBorder)
                                        .clickable(enabled = !viewModel.isAIOperationsLoading) {
                                            viewModel.translateEditorText(lang)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TABS FOR EDIT OR PREVIEW
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isPreviewMode) AccentTeal.copy(alpha = 0.2f) else SlateCard)
                            .border(0.5.dp, if (!isPreviewMode) AccentTealBright else SlateCardBorder, RoundedCornerShape(8.dp))
                            .clickable { isPreviewMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✍️ Edit Catatan",
                            color = if (!isPreviewMode) AccentTealBright else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPreviewMode) AccentTeal.copy(alpha = 0.2f) else SlateCard)
                            .border(0.5.dp, if (isPreviewMode) AccentTealBright else SlateCardBorder, RoundedCornerShape(8.dp))
                            .clickable { isPreviewMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👁️ Pratinjau Markdown",
                            color = if (isPreviewMode) AccentTealBright else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isPreviewMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = viewModel.editorTitle.ifBlank { "Catatan Tanpa Judul" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (viewModel.editorTags.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    viewModel.editorTags.split(",").forEach { tag ->
                                        if (tag.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(TextPurple.copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "#${tag.trim()}",
                                                    color = TextPurple,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = SlateCardBorder, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            MarkdownText(
                                markdown = viewModel.editorContent.ifBlank { "*Belum ada isi catatan. Tuangkan ide cemerlang Anda di tab Edit Catatan.*" },
                                baseColor = TextPrimary,
                                baseFontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                } else {
                    // Input Judul Catatan
                    OutlinedTextField(
                        value = viewModel.editorTitle,
                        onValueChange = { viewModel.editorTitle = it },
                        placeholder = { Text("Tuliskan judul catatan...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentTeal
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Tag Manual / Hasil AI
                    OutlinedTextField(
                        value = viewModel.editorTags,
                        onValueChange = { viewModel.editorTags = it },
                        placeholder = { Text("Tag pintar (pisahkan dengan koma, misal: Agenda, Ide)...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentTeal
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_tags_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Isi Konten Catatan
                    OutlinedTextField(
                        value = viewModel.editorContent,
                        onValueChange = { viewModel.editorContent = it },
                        placeholder = { Text("Tuangkan ide cemerlang, daftar tugas pribadi, atau catatan penting di sini...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SlateCard,
                            unfocusedContainerColor = SlateCard,
                            focusedBorderColor = AccentTeal,
                            unfocusedBorderColor = SlateCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentTeal
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp)
                            .testTag("editor_content_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kotak Tampilan Ringkasan AI Pintar (jika ada)
                val activeNote = viewModel.noteBeingEdited
                if (activeNote != null && (activeNote.aiSummary != null || activeNote.aiKeyPoints != null)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, TextPurple.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "AI Stars",
                                    tint = TextPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RINGKASAN SMART AURA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPurple,
                                    letterSpacing = 1.sp
                                )
                            }

                            if (activeNote.aiSummary != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = activeNote.aiSummary,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (activeNote.aiKeyPoints != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Poin-poin Penting:",
                                    fontSize = 12.sp,
                                    color = TextPurple,
                                    fontWeight = FontWeight.Bold
                                )
                                val bullets = activeNote.aiKeyPoints.split(";;").filter { it.isNotBlank() }
                                Spacer(modifier = Modifier.height(4.dp))
                                bullets.forEach { bullet ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("•", color = TextPurple, fontSize = 13.sp)
                                        Text(
                                            text = bullet.trim().removePrefix("-").trim(),
                                            fontSize = 12.sp,
                                            color = TextMuted,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Jika sedang mengedit dan belum memiliki ringkasan analitis AI
                if (activeNote != null && activeNote.aiSummary == null && activeNote.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    val isAnalyzing = viewModel.isAnalyzingNoteId == activeNote.id

                    Button(
                        onClick = { viewModel.analyzeNoteWithAI(activeNote.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard),
                        border = BorderStroke(1.dp, if (isAnalyzing) AccentTeal else TextPurple.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("editor_analyze_button"),
                        enabled = !isAnalyzing
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TextPurple,
                                strokeWidth = 1.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Aura sedang menganalisis dokumen...", color = TextMuted, fontSize = 13.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "AI sparkle",
                                tint = TextPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ekstrak Ringkasan Aura AI",
                                color = TextPurple,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// Minimal scroll helper
@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
