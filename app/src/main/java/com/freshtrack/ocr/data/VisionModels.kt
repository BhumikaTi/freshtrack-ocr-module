package com.freshtrack.ocr.data

import com.google.gson.annotations.SerializedName

// Request pieces for sending a photo to Gemini (the response reuses GeminiResponse).

data class VisionInlineData(
    @SerializedName("mime_type") val mimeType: String,
    val data: String
)

data class VisionPart(
    val text: String? = null,
    @SerializedName("inline_data") val inlineData: VisionInlineData? = null
)

data class VisionContent(val parts: List<VisionPart>)

data class VisionGenerationConfig(
    val temperature: Float = 0f,
    val maxOutputTokens: Int = 150
)

data class VisionRequest(
    val contents: List<VisionContent>,
    val generationConfig: VisionGenerationConfig = VisionGenerationConfig()
)