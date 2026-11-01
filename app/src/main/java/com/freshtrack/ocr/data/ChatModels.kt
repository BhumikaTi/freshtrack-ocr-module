package com.freshtrack.ocr.data

import com.google.gson.annotations.SerializedName


data class ChatContent(
    val role: String,
    val parts: List<GeminiPart>
)

data class ChatSystemInstruction(
    val parts: List<GeminiPart>
)

data class ChatGenerationConfig(
    val maxOutputTokens: Int = 1024
)

data class ChatRequest(
    @SerializedName("systemInstruction") val systemInstruction: ChatSystemInstruction,
    val contents: List<ChatContent>,
    val generationConfig: ChatGenerationConfig = ChatGenerationConfig()
)

/** What the chat screen shows. */
enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)
