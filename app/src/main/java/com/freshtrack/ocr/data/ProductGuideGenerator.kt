package com.freshtrack.ocr.data

import android.util.Log
import com.freshtrack.ocr.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Where the ingredient information behind a guide came from. Shown to the user. */
enum class GuideBasis {
    /** Ingredient list from Open Food Facts. */
    OPEN_FACTS_INGREDIENTS,

    /** Ingredient list found by an AI web search (Google Search grounding). May be wrong. */
    AI_SEARCH_INGREDIENTS,

    /** No ingredient list at all: only general facts about this type of product. */
    GENERAL_ONLY
}

sealed class GuideResult {
    data class Ready(val text: String, val basis: GuideBasis) : GuideResult()
    data class Unavailable(val reason: String) : GuideResult()
}

/**
 * Writes a short, plain-language "product guide": what the product is for, who it suits,
 * who should be careful, and whether regular use is fine. Text only, no photo is sent.
 *
 * It never invents an ingredient list: it uses the Open Facts list if there is one, an
 * AI-searched list only when Google Search really returned sources, and otherwise it
 * sticks to general facts about the product type.
 */
class ProductGuideGenerator(
    private val service: GeminiService = GeminiClient.service,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    suspend fun generate(product: ProductInfo): GuideResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GuideResult.Unavailable("The AI guide isn't set up in this build.")
        }

        val ingredients = product.ingredientsText?.trim()?.takeIf { it.isNotBlank() }

        try {
            when {
                ingredients != null -> {
                    // Ingredient list in hand (Open Facts, or an earlier AI search): explain it.
                    run(product, ingredients, search = false, basis = GuideBasis.OPEN_FACTS_INGREDIENTS)
                }

                BuildConfig.GEMINI_USE_SEARCH -> {
                    val searched = run(product, null, search = true, basis = GuideBasis.AI_SEARCH_INGREDIENTS)
                    // If the search found no sources, the answer is a guess: fall back to general facts.
                    if (searched is GuideResult.Unavailable && searched.reason == NO_SOURCES) {
                        run(product, null, search = false, basis = GuideBasis.GENERAL_ONLY)
                    } else searched
                }

                else -> run(product, null, search = false, basis = GuideBasis.GENERAL_ONLY)
            }
        } catch (e: IOException) {
            Log.e(TAG, "guide network failure", e)
            GuideResult.Unavailable("There was no connection for the AI guide.")
        } catch (e: Exception) {
            Log.e(TAG, "guide failure", e)
            GuideResult.Unavailable("The AI guide failed. Please try again.")
        }
    }

    private suspend fun run(
        product: ProductInfo,
        ingredients: String?,
        search: Boolean,
        basis: GuideBasis
    ): GuideResult {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(listOf(GeminiPart(prompt(product, ingredients, search, basis))))),
            tools = if (search) listOf(GeminiTool()) else null
        )

        val response = service.generate(BuildConfig.GEMINI_CHAT_MODEL, request)
        Log.d(TAG, "guide(${product.name}, $basis): httpCode=${response.code()}")

        if (!response.isSuccessful) {
            Log.w(TAG, "guide failed: ${response.code()} ${response.errorBody()?.string()}")
            return GuideResult.Unavailable(
                when (response.code()) {
                    429 -> "The AI limit has been reached for now. Try again later."
                    else -> "The AI guide is unavailable right now."
                }
            )
        }

        val candidate = response.body()?.candidates?.firstOrNull()
        val text = candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("")?.trim().orEmpty()
        if (text.isBlank()) return GuideResult.Unavailable("The AI gave no answer.")

        if (search && !usedWebSearch(candidate?.groundingMetadata)) {
            return GuideResult.Unavailable(NO_SOURCES)
        }

        // Strip any markdown the model adds despite being told not to.
        val clean = text.replace("**", "").replace("```", "").replace(Regex("(?m)^#+\\s*"), "").trim()
        return GuideResult.Ready(clean, basis)
    }

    private fun prompt(product: ProductInfo, ingredients: String?, search: Boolean, basis: GuideBasis): String {
        val who = buildString {
            append(product.name)
            product.brand?.let { append(" by ").append(it) }
            product.categories?.let { append(" (").append(it.take(80)).append(")") }
        }

        val source = when {
            ingredients != null -> "Ingredient list (from the product database):\n$ingredients\n\nBase every ingredient comment ONLY on this list."
            search -> "Use Google Search to find this product's real ingredient list. Only use an ingredient list a source clearly ties to this exact product. If you cannot find one, say so and do not guess."
            else -> "No ingredient list is available. Do NOT list or guess ingredients. Talk only about what this TYPE of product generally does."
        }

        return """
            You write short, plain-language product guides for an expiry-tracking app. Readers are ordinary shoppers, not experts.

            Product: $who

            $source

            Write these sections, each 1-2 short lines, in simple English:
            What it is for:
            Suits:  (for skin products: dry, oily, sensitive skin; for food/other: who it suits)
            Good to know:  (what the main ingredients do, only if an ingredient list is available)
            Be careful if:  (allergies, sensitive skin, pregnancy, children, health conditions)
            Regular use:  (is daily use fine, or only now and then)

            Rules:
            - Plain text only. No markdown, no asterisks, no bullet symbols.
            - Be honest and balanced: say where it helps and where it may harm.
            - Do not claim it cures or treats any disease. Do not give medical advice.
            - If you are not sure about something, say "not sure" instead of guessing.
            - Keep the whole answer under 120 words.
        """.trimIndent()
    }

    private fun usedWebSearch(metadata: com.google.gson.JsonElement?): Boolean {
        if (metadata == null || !metadata.isJsonObject) return false
        val chunks = metadata.asJsonObject.getAsJsonArray("groundingChunks")
        return chunks != null && chunks.size() > 0
    }

    companion object {
        private const val TAG = "FreshTrackGuide"
        private const val NO_SOURCES = "no-search-sources"
    }
}