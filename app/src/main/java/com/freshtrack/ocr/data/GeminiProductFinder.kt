package com.freshtrack.ocr.data

import android.util.Log
import com.freshtrack.ocr.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed class GeminiResult {
    /** The AI found something. Always shown to the user as unverified. */
    data class Suggestion(val product: ProductInfo) : GeminiResult()

    /** The AI searched and could not tie this barcode to a product. */
    data object NotIdentified : GeminiResult()

    /** The AI step could not run (no key, limit reached, no connection...). Says nothing about the product. */
    data class Unavailable(val reason: String) : GeminiResult()
}

/**
 * Fallback for barcodes that Open Food Facts doesn't know. It asks Gemini,
 * with Google Search switched on, which product the barcode belongs to.
 *
 * What it can give: name, brand, a short category, and an ingredients list
 * if a web page states one. What it cannot give: a product photo, a nutrition
 * table, or trustworthy allergen data. None of those are filled in.
 */
class GeminiProductFinder(
    private val service: GeminiService = GeminiClient.service,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    private val gson = Gson()

    suspend fun find(barcode: String): GeminiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Unavailable("The AI lookup isn't set up in this build.")
        }

        try {
            val response = service.generate(MODEL, buildRequest(barcode))
            Log.d(TAG, "find($barcode): httpCode=${response.code()}")

            if (!response.isSuccessful) {
                Log.w(TAG, "find($barcode) failed: ${response.code()} ${response.errorBody()?.string()}")
                return@withContext GeminiResult.Unavailable(describeHttpError(response.code()))
            }

            val text = response.body()
                ?.candidates?.firstOrNull()
                ?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.trim()
                .orEmpty()

            if (text.isBlank()) {
                return@withContext GeminiResult.Unavailable("The AI lookup gave no answer.")
            }

            parse(barcode, text)

        } catch (e: IOException) {
            Log.e(TAG, "find($barcode) network failure", e)
            GeminiResult.Unavailable("There was no connection for the AI lookup.")
        } catch (e: Exception) {
            Log.e(TAG, "find($barcode) unexpected failure", e)
            GeminiResult.Unavailable("The AI lookup failed.")
        }
    }

    private fun buildRequest(barcode: String): GeminiRequest {
        val prompt = """
            You are helping an expiry-tracking app identify a packaged product from its barcode number.

            Barcode (EAN/UPC): $barcode

            Use Google Search to look up this exact barcode number. Only report details that a search result clearly ties to this exact barcode. If you cannot find it, or you are not sure, do not guess.

            Reply with ONLY one JSON object. No other text, no markdown, no code fences:
            {"found": true, "name": "", "brand": "", "category": "", "ingredients": ""}

            Rules:
            - Set "found" to false unless a source clearly matches this exact barcode.
            - Use an empty string for any field you could not confirm.
            - Copy "ingredients" from a source. Never write it from your own knowledge.
            - "category" is a short product type, such as "face wash" or "biscuits".
        """.trimIndent()

        return GeminiRequest(
            contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
            tools = listOf(GeminiTool())
        )
    }

    private fun parse(barcode: String, text: String): GeminiResult {
        // The model might still wrap the JSON in extra words, so cut out the {...} part.
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end <= start) {
            return GeminiResult.Unavailable("The AI answer couldn't be read.")
        }

        val answer: GeminiAnswer? = try {
            gson.fromJson(text.substring(start, end + 1), GeminiAnswer::class.java)
        } catch (e: Exception) {
            null
        }
        if (answer == null) {
            return GeminiResult.Unavailable("The AI answer couldn't be read.")
        }

        val name = answer.name?.trim().orEmpty()
        if (answer.found != true || name.isBlank()) {
            return GeminiResult.NotIdentified
        }

        return GeminiResult.Suggestion(
            ProductInfo(
                barcode = barcode,
                name = name,
                brand = answer.brand?.trim()?.takeIf { it.isNotBlank() },
                imageUrl = null,       // the AI cannot give a reliable image, so none is invented
                quantity = null,
                productType = null,
                categories = answer.category?.trim()?.takeIf { it.isNotBlank() },
                nutriments = null,     // no nutrition table from an AI search
                ingredientsText = answer.ingredients?.trim()?.takeIf { it.isNotBlank() },
                allergens = emptyList() // never derived from an AI answer
            )
        )
    }

    private fun describeHttpError(code: Int): String = when (code) {
        429 -> "The AI lookup limit has been reached for now."
        400, 403 -> "The AI lookup was refused."
        404 -> "The AI model is no longer available."
        else -> "The AI lookup is unavailable right now."
    }

    companion object {
        private const val TAG = "FreshTrackGemini"


        private const val MODEL = "gemini-3.1-flash-lite"
    }
}