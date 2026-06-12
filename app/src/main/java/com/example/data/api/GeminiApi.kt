package com.example.data.api

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * MODEL_NAME adalah nama model Gemini yang digunakan secara global di seluruh aplikasi.
 * Di sini kita menggunakan model "gemini-3.1-flash-lite-preview" (Flash-Lite), varian paling ringan
 * dan efisien yang tersedia.
 * 
 * Alasan menggunakan Gemini Flash-Lite:
 * 1. Rate Limit Lebih Tinggi: Mengurangi risiko pemblokiran kuota (quota limit) selama proses pengembangan dan pengujian.
 * 2. Respons Lebih Cepat: Waktu tunda (latency) yang sangat rendah, memberikan pengalaman pengguna yang responsif.
 * 3. Biaya Lebih Rendah: Jauh lebih hemat dalam konsumsi kuota token dibandingkan versi Pro atau default.
 * 4. Sangat Sesuai untuk Tugas Ringan: Sangat efisien untuk memproses teks, pengelompokan (smart tag), pembuatan judul otomatis (auto-title), dan peringkasan konten (summarization).
 */
const val MODEL_NAME = "gemini-3.1-flash-lite-preview"

// --- Moshi Data Classes for Gemini API ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit client singleton ---

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}
