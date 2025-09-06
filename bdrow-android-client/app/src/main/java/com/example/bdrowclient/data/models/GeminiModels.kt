package com.example.bdrowclient.data.models

import com.google.gson.annotations.SerializedName

// Request models
data class GeminiRequest(
    val model: String,
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val role: String,
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded
)

data class GenerationConfig(
    val responseModalities: List<String>? = null,
    val temperature: Float? = null,
    val topK: Int? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

// Response models
data class GeminiResponse(
    val candidates: List<Candidate>?,
    val promptFeedback: PromptFeedback?,
    val error: ErrorResponse?
)

data class Candidate(
    val content: Content?,
    val finishReason: String?,
    val safetyRatings: List<SafetyRating>?
)

data class SafetyRating(
    val category: String,
    val probability: String
)

data class PromptFeedback(
    val blockReason: String?,
    val safetyRatings: List<SafetyRating>?
)

data class ErrorResponse(
    val code: Int,
    val message: String,
    val status: String
)
