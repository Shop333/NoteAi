package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.MODEL_NAME
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.ChatDao
import com.example.data.db.ChatMessageEntity
import com.example.data.db.NoteDao
import com.example.data.db.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WorkspaceRepository(
    private val noteDao: NoteDao,
    private val chatDao: ChatDao
) {
    // --- Room Local Data Access ---

    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotesFlow()
    val allChats: Flow<List<ChatMessageEntity>> = chatDao.getAllChatsFlow()

    suspend fun saveNote(note: NoteEntity): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Long) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(id)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        chatDao.clearAllChats()
    }

    // --- Gemini API integrations ---

    /**
     * Sends the current conversation history + a new prompt to Gemini,
     * receives a response, persists both to local database, and returns the response string.
     */
    suspend fun sendChatMessage(
        history: List<ChatMessageEntity>,
        userMessageText: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key missing! Please set your GEMINI_API_KEY in the Secrets panel in AI Studio."
        }

        // Save User Message to local DB
        val userEntity = ChatMessageEntity(sender = "user", text = userMessageText)
        chatDao.insertChat(userEntity)

        // Build list of message contents for Gemini (alternating turns)
        val contentsList = mutableListOf<Content>()
        
        // Add historical turns
        for (msg in history) {
            val role = if (msg.sender == "user") "user" else "model"
            contentsList.add(Content(parts = listOf(Part(text = msg.text)), role = role))
        }

        // Add current turn
        contentsList.add(Content(parts = listOf(Part(text = userMessageText)), role = "user"))

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are MindSpace, a highly intelligent, empathetic, and responsive AI co-thinker. " +
                                "Help the user outline ideas, organize thoughts, draft notes, or solve complex problems. " +
                                "Maintain a concise, clear, and modern tone."
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )

            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I apologize, but I could not formulate a response at this time."

            // Save Model Response to local DB
            val modelEntity = ChatMessageEntity(sender = "model", text = replyText)
            chatDao.insertChat(modelEntity)

            replyText
        } catch (e: Exception) {
            val errorMsg = "Connectivity issue: ${e.localizedMessage ?: "Please try again."}"
            // Optional: insert error into chat to keep user informed or just return it to view model
            errorMsg
        }
    }

    /**
     * Uses Gemini to review a note's title & body, then generates
     * a 1-sentence summary and key bullet points, saving them back to Room database.
     */
    suspend fun analyzeNoteWithAI(noteId: Long): Boolean = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext false
        }

        val note = noteDao.getNoteById(noteId) ?: return@withContext false
        if (note.content.isBlank()) return@withContext false

        val prompt = "Review this note:\n\n" +
                "Title: ${note.title}\n" +
                "Content:\n${note.content}\n\n" +
                "Provide two distinct lines of output strictly in this exact format, with NO styling or extra conversational text:\n" +
                "SUMMARY: <A concise 1-sentence summary of the note, max 15 words>\n" +
                "KEYPOINTS: <A list of 3-4 bullet points key takeaways separated by double semi-colons ';;'>"

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)), role = "user")
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are an analytical assistant. You analyze note transcripts and summarize them briefly and extract actionable bullet points."))
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )

            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            if (resultText.isNotBlank()) {
                var summary: String? = null
                var keyPointsStr: String? = null

                val lines = resultText.lines()
                for (line in lines) {
                    if (line.startsWith("SUMMARY:", ignoreCase = true)) {
                        summary = line.substringAfter("SUMMARY:").trim()
                    }
                    if (line.startsWith("KEYPOINTS:", ignoreCase = true)) {
                        val items = line.substringAfter("KEYPOINTS:").trim()
                        keyPointsStr = items
                    }
                }

                // If not formatted strictly, try manual extraction
                if (summary == null) {
                    summary = if (resultText.contains("KEYPOINTS")) {
                        resultText.substringBefore("KEYPOINTS").replace("SUMMARY:", "").trim()
                    } else {
                        resultText.take(100).trim()
                    }
                }
                if (keyPointsStr == null && resultText.contains("KEYPOINTS:")) {
                    keyPointsStr = resultText.substringAfter("KEYPOINTS:").trim()
                }

                // Update note with outputs
                val updatedNote = note.copy(
                    aiSummary = summary,
                    aiKeyPoints = keyPointsStr
                )
                noteDao.updateNote(updatedNote)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Menghasilkan ringkasan singkat dari konten catatan menggunakan Gemini.
     */
    suspend fun generateSummary(content: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Kunci API Gemini belum dikonfigurasi! Harap atur GEMINI_API_KEY di panel Secrets AI Studio."
        }
        if (content.isBlank()) return@withContext "Konten kosong, tidak bisa meringkas."

        val prompt = "Tolong ringkas catatan berikut menjadi poin-poin penting yang singkat dan padat (maksimal 3 poin):\n\n$content"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            systemInstruction = Content(parts = listOf(Part(text = "Anda adalah asisten profesional yang ahli dalam meringkas catatan panjang secara singkat dan jelas.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Tidak dapat menghasilkan ringkasan."
        } catch (e: Exception) {
            "Gagal meringkas: ${e.localizedMessage}"
        }
    }

    /**
     * Menghasilkan judul otomatis berdasarkan konten catatan yang ditulis.
     */
    suspend fun generateAutoTitle(content: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ""
        }
        if (content.isBlank()) return@withContext ""

        val prompt = "Hasilkan judul yang sangat singkat (maksimal 5 kata) yang secara akurat menjelaskan isi catatan berikut. Berikan HANYA judul saja sebagai respons, tanpa tanda kutip atau dekorasi lainnya:\n\n$content"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user"))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Menganalisis konten catatan dan merumuskan tag pintar (smart tags).
     */
    suspend fun generateSmartTags(content: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ""
        }
        if (content.isBlank()) return@withContext ""

        val prompt = "Analisis isi catatan berikut dan berikan 2 hingga 4 kata kunci/tag yang paling relevan (misal: Keuangan, Pengembangan, Ide, Pribadi), pisahkan hanya dengan tanda koma (tanpa spasi setelah koma, contoh: Ide,Kerja):\n\n$content"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user"))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Melanjutkan tulisan secara otomatis berdasarkan konteks judul dan konten yang sudah diinput.
     */
    suspend fun continueWriting(title: String, content: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Kunci API Gemini belum dikonfigurasi!"
        }

        val prompt = "Lanjutkan penulisan catatan berikut berdasarkan konteks dan gaya bahasanya. Berikan HANYA teks tambahannya yang mengalir mulus tanpa mengulang kalimat yang sudah ada atau memberikan penjelasan tambahan:\n\nJudul: $title\nKonten:\n$content"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user")),
            systemInstruction = Content(parts = listOf(Part(text = "Anda adalah asisten kreatif yang menulis teks lanjutan secara mulus dan bergaya alami.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
        } catch (e: Exception) {
            " Gagal memproses teks lanjutan."
        }
    }

    /**
     * Menerjemahkan teks catatan ke bahasa target yang ditentukan.
     */
    suspend fun translateText(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Kunci API Gemini belum dikonfigurasi!"
        }
        if (text.isBlank()) return@withContext ""

        val prompt = "Terjemahkan teks berikut ke dalam bahasa $targetLanguage dengan gaya bahasa yang alami, akurat, dan profesional. Hanya berikan hasil terjemahannya saja tanpa penjelasan apapun:\n\n$text"
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)), role = "user"))
        )

        try {
            val response = RetrofitClient.service.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Tidak dapat menerjemahkan."
        } catch (e: Exception) {
            "Gagal menerjemahkan: ${e.localizedMessage}"
        }
    }
}
