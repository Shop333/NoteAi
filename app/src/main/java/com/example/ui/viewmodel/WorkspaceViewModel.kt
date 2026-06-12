package com.example.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessageEntity
import com.example.data.db.NoteEntity
import com.example.data.repository.WorkspaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkspaceViewModel(private val repository: WorkspaceRepository) : ViewModel() {

    // --- Observasi Data ---

    // Mengamati pesan obrolan AI
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.allChats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _rawNotesList = repository.allNotes
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val sortBy = MutableStateFlow("Terbaru") // Opsi sort: "Terbaru", "Terlama", "A-Z"

    // Menyaring dan mengurutkan catatan secara otomatis
    val filteredNotesList: StateFlow<List<NoteEntity>> = combine(
        _rawNotesList,
        searchQuery,
        selectedCategory,
        sortBy
    ) { notes, query, category, sort ->
        val filtered = notes.filter { note ->
            val matchesQuery = note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true) ||
                    (note.tags?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "All" || note.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }

        // Catatan yang disematkan (pinned) akan selalu berada paling atas
        when (sort) {
            "Terlama" -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.timestamp })
            "A-Z" -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenBy { it.title.lowercase() })
            else -> filtered.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.timestamp }) // Terbaru
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Menyediakan daftar kategori secara dinamis (Default + Kategori Kustom yang sudah ada)
    val categoriesList: StateFlow<List<String>> = _rawNotesList.map { notes ->
        val existing = notes.map { it.category }.distinct().filter { it.isNotBlank() && it != "Notes" }
        val defaults = listOf("All", "Notes", "Kerja", "Ide", "Pribadi")
        (defaults + existing).distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("All", "Notes", "Kerja", "Ide", "Pribadi")
    )

    // --- Status Dynamic UI ---

    var activeTab by mutableStateOf("notes") // Tab aktif: "notes" atau "ai_chat"
    var isAILoading by mutableStateOf(false) // Indikator loading untuk AI Chat
    var isAIOperationsLoading by mutableStateOf(false) // Indikator loading untuk operasi AI asisten (summary/title/tag/dll)
    var isAnalyzingNoteId by mutableStateOf<Long?>(null)

    // Opsi Visual UI
    var isGridView by mutableStateOf(false) // Toggle tampilan Grid (true) atau List (false)
    var themeMode by mutableStateOf("System") // Pilihan Tema: "System", "Light", "Dark"

    // State untuk Notes Editor (Lembar Edit/Tambah Catatan)
    var isEditorVisible by mutableStateOf(false)
    var noteBeingEdited by mutableStateOf<NoteEntity?>(null)
    var editorTitle by mutableStateOf("")
    var editorContent by mutableStateOf("")
    var editorCategory by mutableStateOf("Notes")
    var editorTags by mutableStateOf("")

    // Input obrolan AI
    var chatInputText by mutableStateOf("")

    // Notifikasi Peringatan Keamanan
    val securityWarning = "Peringatan Keamanan: AI Studio menginjeksikan kunci API dengan aman via BuildConfig. Hindari publikasi APK produksi yang berisi kunci rahasia secara hardcode."

    // --- Aksi / Fungsi Kontrol ---

    fun openNoteEditor(note: NoteEntity? = null) {
        noteBeingEdited = note
        if (note != null) {
            editorTitle = note.title
            editorContent = note.content
            editorCategory = note.category
            editorTags = note.tags ?: ""
        } else {
            editorTitle = ""
            editorContent = ""
            editorCategory = "Notes"
            editorTags = ""
        }
        isEditorVisible = true
    }

    fun closeNoteEditor() {
        isEditorVisible = false
        noteBeingEdited = null
        editorTitle = ""
        editorContent = ""
        editorCategory = "Notes"
        editorTags = ""
    }

    fun saveNote() {
        if (editorTitle.isBlank() && editorContent.isBlank()) {
            closeNoteEditor()
            return
        }

        viewModelScope.launch {
            val noteToSave = noteBeingEdited?.copy(
                title = editorTitle.ifBlank { "Catatan Tanpa Judul" },
                content = editorContent,
                category = editorCategory,
                tags = editorTags.ifBlank { null },
                timestamp = System.currentTimeMillis()
            ) ?: NoteEntity(
                title = editorTitle.ifBlank { "Catatan Tanpa Judul" },
                content = editorContent,
                category = editorCategory,
                tags = editorTags.ifBlank { null }
            )

            repository.saveNote(noteToSave)
            closeNoteEditor()
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleNoteFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    // Toggle Pin Catatan (Sematkan ke paling atas)
    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    // --- Operasi AI Pintar (Gemini API) ---

    // 1. Meringkas Catatan (Summarize)
    fun summarizeNote(note: NoteEntity) {
        viewModelScope.launch {
            isAIOperationsLoading = true
            val summary = repository.generateSummary(note.content)
            repository.updateNote(note.copy(aiSummary = summary))
            isAIOperationsLoading = false
        }
    }

    // Ringkas konten di dalam editor langsung
    fun summarizeEditorContent() {
        if (editorContent.isBlank()) return
        viewModelScope.launch {
            isAIOperationsLoading = true
            val summary = repository.generateSummary(editorContent)
            editorContent = if (editorContent.endsWith("\n")) {
                editorContent + "\n=== Ringkasan AI ===\n" + summary
            } else {
                editorContent + "\n\n=== Ringkasan AI ===\n" + summary
            }
            isAIOperationsLoading = false
        }
    }

    // 2. Generate Judul Otomatis (Auto-title)
    fun autoTitleNote(note: NoteEntity) {
        viewModelScope.launch {
            isAIOperationsLoading = true
            val newTitle = repository.generateAutoTitle(note.content)
            if (newTitle.isNotBlank()) {
                repository.updateNote(note.copy(title = newTitle))
            }
            isAIOperationsLoading = false
        }
    }

    // Prediksi judul langsung di editor
    fun generateTitleForEditor() {
        if (editorContent.isBlank()) return
        viewModelScope.launch {
            isAIOperationsLoading = true
            val newTitle = repository.generateAutoTitle(editorContent)
            if (newTitle.isNotBlank()) {
                editorTitle = newTitle
            }
            isAIOperationsLoading = false
        }
    }

    // 3. Rekomendasikan Tag Secara Pintar (Smart Tag)
    fun smartTagNote(note: NoteEntity) {
        viewModelScope.launch {
            isAIOperationsLoading = true
            val recommendedTags = repository.generateSmartTags(note.content)
            if (recommendedTags.isNotBlank()) {
                repository.updateNote(note.copy(tags = recommendedTags))
            }
            isAIOperationsLoading = false
        }
    }

    // Tag otomatis langsung di editor
    fun generateTagsForEditor() {
        if (editorContent.isBlank()) return
        viewModelScope.launch {
            isAIOperationsLoading = true
            val recommendedTags = repository.generateSmartTags(editorContent)
            if (recommendedTags.isNotBlank()) {
                editorTags = recommendedTags
            }
            isAIOperationsLoading = false
        }
    }

    // 4. Lanjutkan Penulisan (Continue Writing)
    fun continueWritingNote(note: NoteEntity) {
        viewModelScope.launch {
            isAIOperationsLoading = true
            val addedText = repository.continueWriting(note.title, note.content)
            if (addedText.isNotBlank()) {
                val updatedContent = if (note.content.endsWith(" ") || note.content.isEmpty()) {
                    note.content + addedText
                } else {
                    note.content + " " + addedText
                }
                repository.updateNote(note.copy(content = updatedContent))
            }
            isAIOperationsLoading = false
        }
    }

    // Melanjutkan tulisan di dalam editor
    fun continueWritingInEditor() {
        viewModelScope.launch {
            isAIOperationsLoading = true
            val addedText = repository.continueWriting(editorTitle, editorContent)
            if (addedText.isNotBlank()) {
                editorContent = if (editorContent.endsWith(" ") || editorContent.isEmpty()) {
                    editorContent + addedText
                } else {
                    editorContent + " " + addedText
                }
            }
            isAIOperationsLoading = false
        }
    }

    // 5. Menerjemahkan Catatan (Translate)
    fun translateNote(note: NoteEntity, targetLang: String) {
        viewModelScope.launch {
            isAIOperationsLoading = true
            val translatedContent = repository.translateText(note.content, targetLang)
            if (translatedContent.isNotBlank()) {
                val translatedTitle = if (note.title.isNotBlank() && note.title != "Catatan Tanpa Judul") {
                    repository.translateText(note.title, targetLang)
                } else {
                    note.title
                }
                repository.updateNote(note.copy(
                    title = translatedTitle,
                    content = translatedContent
                ))
            }
            isAIOperationsLoading = false
        }
    }

    // Terjemahkan langsung konten di editor
    fun translateEditorText(targetLang: String) {
        if (editorContent.isBlank()) return
        viewModelScope.launch {
            isAIOperationsLoading = true
            val translatedContent = repository.translateText(editorContent, targetLang)
            if (translatedContent.isNotBlank()) {
                editorContent = translatedContent
                if (editorTitle.isNotBlank() && editorTitle != "Catatan Tanpa Judul") {
                    val translatedTitle = repository.translateText(editorTitle, targetLang)
                    editorTitle = translatedTitle
                }
            }
            isAIOperationsLoading = false
        }
    }

    // --- Lama: Chat MindSpace AI ---

    fun analyzeNoteWithAI(noteId: Long) {
        viewModelScope.launch {
            isAnalyzingNoteId = noteId
            repository.analyzeNoteWithAI(noteId)
            isAnalyzingNoteId = null
        }
    }

    fun sendChatMessage() {
        val message = chatInputText.trim()
        if (message.isBlank() || isAILoading) return

        chatInputText = ""
        isActiveTabAI()

        viewModelScope.launch {
            isAILoading = true
            val currentHistory = chatMessages.value
            repository.sendChatMessage(currentHistory, message)
            isAILoading = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
        }
    }

    private fun isActiveTabAI() {
        if (activeTab != "ai_chat") {
            activeTab = "ai_chat"
        }
    }

    // --- Ekspor Catatan sebagai File Markdown (.md) ---
    fun exportNoteToUri(context: android.content.Context, uri: android.net.Uri, note: NoteEntity) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val builder = java.lang.StringBuilder()
                    builder.append("# ${note.title.ifBlank { "Catatan Tanpa Judul" }}\n\n")
                    if (!note.tags.isNullOrBlank()) {
                        val tagsList = note.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ") { "#$it" }
                        if (tagsList.isNotBlank()) {
                            builder.append("Tags: $tagsList\n\n")
                        }
                    }
                    builder.append("Kategori: ${note.category}\n")
                    val formattedTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp))
                    builder.append("Tanggal Hubungan: $formattedTime\n\n")
                    builder.append("---\n\n")
                    builder.append(note.content)
                    
                    if (note.aiSummary != null) {
                        builder.append("\n\n---\n\n")
                        builder.append("### Ringkasan Aura AI\n\n")
                        builder.append("${note.aiSummary}\n")
                    }
                    if (note.aiKeyPoints != null) {
                        builder.append("\n\n### Poin-poin Penting\n\n")
                        val bulletPoints = note.aiKeyPoints.split(";;").filter { it.isNotBlank() }
                        bulletPoints.forEach { point ->
                            builder.append("- ${point.trim().removePrefix("-").trim()}\n")
                        }
                    }
                    outputStream.write(builder.toString().toByteArray())
                    android.widget.Toast.makeText(context, "Catatan berhasil diekspor ke penyimpanan!", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(context, "Gagal mengekspor catatan: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// --- ViewModel Factory ---

class WorkspaceViewModelFactory(private val repository: WorkspaceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkspaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkspaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
