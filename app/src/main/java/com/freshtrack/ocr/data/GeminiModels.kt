package com.freshtrack.ocr.data

import com.google.gson.annotations.SerializedName


data class GeminiRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null
)

data class GeminiContent(val parts: List<GeminiPart>?)

data class GeminiPart(val text: String?)

data class GeminiTool(
    @SerializedName("google_search") val googleSearch: Map<String, String> = emptyMap()
)


data class GeminiResponse(val candidates: List<GeminiCandidate>?)

data class GeminiCandidate(
    val content: GeminiContent?,
    // Present only when Google Search was really used to build the answer.
    val groundingMetadata: com.google.gson.JsonElement? = null
)


data class GeminiAnswer(
    val found: Boolean?,
    val name: String?,
    val brand: String?,
    val category: String?,
    val ingredients: String?
)