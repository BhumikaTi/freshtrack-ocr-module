package com.freshtrack.ocr.data

import android.util.Log
import com.freshtrack.ocr.BuildConfig
import com.freshtrack.ocr.auth.CurrentUserProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

sealed class ChatResult {
    data class Reply(val text: String) : ChatResult()
    data class Failure(val message: String) : ChatResult()
}

class ChatRepository(
    private val productDao: ProductDao,
    private val service: GeminiService = GeminiClient.service,
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    suspend fun send(
        history: List<ChatContent>,
        sharePantry: Boolean
    ): ChatResult = withContext(Dispatchers.IO) {

        if (apiKey.isBlank()) {
            return@withContext ChatResult.Failure(
                "The chatbot isn't set up in this build. " +
                        "Add GEMINI_API_KEY to local.properties and rebuild."
            )
        }

        // Gemini expects the conversation to start with a user turn.
        val turns = history.takeLast(MAX_TURNS)
            .dropWhile { it.role != ROLE_USER }

        if (turns.isEmpty()) {
            return@withContext ChatResult.Failure(
                "There is nothing to send yet."
            )
        }

        try {
            val request = ChatRequest(
                systemInstruction = buildSystemInstruction(sharePantry),
                contents = turns
            )

            // Retry the main model once, then try the backup model.
            var usedModel = MODEL
            var response = service.chat(usedModel, request)

            if (response.code() in RETRYABLE) {
                delay(1500)
                response = service.chat(usedModel, request)
            }

            if (response.code() in RETRYABLE && BACKUP_MODEL != MODEL) {
                usedModel = BACKUP_MODEL
                response = service.chat(usedModel, request)
            }

            if (!response.isSuccessful) {
                val body = response.errorBody()?.string()

                Log.w(
                    TAG,
                    "chat failed ($usedModel): ${response.code()} $body"
                )

                val base = describeHttpError(response.code())

                return@withContext ChatResult.Failure(
                    if (BuildConfig.DEBUG) {
                        "$base\n[${response.code()}] ${apiErrorReason(body)}"
                    } else {
                        base
                    }
                )
            }

            val text = response.body()
                ?.candidates?.firstOrNull()
                ?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.trim()
                .orEmpty()

            if (text.isBlank()) {
                ChatResult.Failure(
                    "I couldn't come up with an answer to that. Try rephrasing it."
                )
            } else {
                ChatResult.Reply(cleanFormatting(text))
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "chat network failure", e)
            ChatResult.Failure(
                "No internet connection. Check your network and try again."
            )
        } catch (e: Exception) {
            Log.e(TAG, "chat unexpected failure", e)
            ChatResult.Failure(
                "Something went wrong. Please try again."
            )
        }
    }

    private suspend fun buildSystemInstruction(
        sharePantry: Boolean
    ): ChatSystemInstruction {

        val today = LocalDate.now()
            .format(
                DateTimeFormatter.ofPattern(
                    "dd MMM yyyy",
                    Locale.ENGLISH
                )
            )

        val pantry = when {
            !sharePantry ->
                "The user has chosen NOT to share their product list. If they ask about " +
                        "their own products, tell them to turn on \"Use my product list\" " +
                        "at the top of the screen."

            else -> {
                val userId = CurrentUserProvider.getUserId()

                if (userId == null) {
                    "Please log in to use your product list."
                } else {
                    val products = productDao.getAllProducts(userId)
                        .take(MAX_PRODUCTS)

                    if (products.isEmpty()) {
                        "The user has not saved any products yet."
                    } else {
                        products.joinToString("\n") { product ->
                            "- ${product.productName.oneLine()} | " +
                                    "${product.category.oneLine()} " +
                                    "| expires: ${product.expiryDate.oneLine()}"
                        }
                    }
                }
            }
        }

        val prompt = """
            You are FreshTrack Assistant, a friendly helper inside an Android app that tracks
            the expiry dates of household products (food, cosmetics, medicines, pet supplies).

            Today's date is $today.

            What you help with: food storage and shelf life, what to use first, meal ideas
            using the user's items, shopping suggestions, reducing waste, and how to read
            expiry labels (for example "best before" vs "use by"). If a question is unrelated,
            politely say you can only help with these topics.

            Style: reply in plain text only. Do not use markdown, asterisks or headings.
            Keep answers short (under about 120 words) unless the user asks for a recipe.
            If you list things, put each on its own line starting with "• ".

            Safety rules:
            - Never say an expired or doubtful food, medicine or cosmetic is definitely safe.
              When unsure, advise throwing it away.
            - For medicines, tell the user to ask a pharmacist or doctor.
            - Do not invent expiry dates. If something is not in the list below, say you
              don't know.

            The user's saved products follow. Dates are exactly as the user typed or scanned
            them and the formats vary. Read "06/28" as June 2028, "SEP 26" as September 2026,
            and a date with only a month and year as the last day of that month.
        """.trimIndent()

        return ChatSystemInstruction(
            parts = listOf(
                GeminiPart("$prompt\n\n$pantry")
            )
        )
    }

    /** The model sometimes ignores "no markdown", so tidy common cases. */
    private fun cleanFormatting(text: String): String =
        text
            .replace("**", "")
            .replace(Regex("(?m)^[ \\t]*[*-][ \\t]+"), "• ")
            .replace(Regex("(?m)^#{1,6}[ \\t]*"), "")
            .trim()

    private fun String.oneLine(): String =
        replace(Regex("\\s+"), " ").trim()

    private fun apiErrorReason(body: String?): String =
        try {
            com.google.gson.JsonParser.parseString(body)
                .asJsonObject
                .getAsJsonObject("error")
                .get("message")
                .asString
                .take(300)
        } catch (e: Exception) {
            body.orEmpty().take(300)
        }

    private fun describeHttpError(code: Int): String =
        when (code) {
            429 ->
                "You've reached the free-tier limit for now. Wait a minute and try again."

            400, 403 ->
                "Gemini refused the request. Check that your API key is valid."

            404 ->
                "The AI model is no longer available."

            in 500..599 ->
                "Gemini is busy right now. Please try again shortly."

            else ->
                "The assistant is unavailable right now."
        }

    companion object {
        private const val TAG = "FreshTrackChat"

        private val MODEL = BuildConfig.GEMINI_CHAT_MODEL
        private const val BACKUP_MODEL = "gemini-3.5-flash-lite"

        private val RETRYABLE = setOf(429, 500, 502, 503, 504)

        private const val ROLE_USER = "user"

        /** How many past messages are sent each time. */
        private const val MAX_TURNS = 20

        /** How many saved products are included in the prompt. */
        private const val MAX_PRODUCTS = 60
    }
}
