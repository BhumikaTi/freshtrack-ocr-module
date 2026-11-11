package com.freshtrack.ocr.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.freshtrack.ocr.BuildConfig
import com.freshtrack.ocr.ExpiryDateParser
import com.freshtrack.ocr.ExpiryDateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream

sealed class VisionDateResult {
    data class Found(val date: ExpiryDateResult, val text: String) : VisionDateResult()
    data class NotFound(val reason: String) : VisionDateResult()
}

/**
 * Last resort when on-device OCR + the parser found no date: send the (cropped) photo to
 * Gemini, ask it to copy the printed date text, and run that text through the same
 * [ExpiryDateParser] so the result has the same format and checks as every other date.
 *
 * Needs internet. Any failure returns NotFound so the caller can show the manual-entry dialog.
 */
object GeminiVisionDateReader {

    private const val TAG = "FreshTrackVision"
    private const val MAX_SIDE = 1280

    // Whole attempt (all retries) must finish in this time, so the dialog never hangs.
    private const val TOTAL_TIMEOUT_MS = 30_000L
    private const val BACKUP_MODEL = "gemini-3.5-flash-lite"
    private val RETRYABLE = setOf(429, 500, 502, 503, 504)

    private const val PROMPT =
        "This is a photo of a product package, cropped near its printed dates. " +
                "The text is often dot-matrix or inkjet printed and may be faint or tilted. " +
                "Copy the printed date information exactly as it appears, one short line per item, " +
                "with its label if one is visible. Examples of lines: \"MFG 05/2026\", " +
                "\"EXP 11/2026\", \"BEST BEFORE 12 MONTHS FROM MFG\", \"USE BY 15 NOV 2026\". " +
                "Copy the digits exactly; do not guess or invent a date. " +
                "If no date is printed or readable, reply with exactly: NONE. " +
                "Reply with the lines only, no other words."

    suspend fun read(
        bitmap: Bitmap,
        service: GeminiService = GeminiClient.service,
        apiKey: String = BuildConfig.GEMINI_API_KEY
    ): VisionDateResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext VisionDateResult.NotFound("AI is not set up in this build")
        }
        withTimeoutOrNull(TOTAL_TIMEOUT_MS) {
            readOnce(bitmap, service)
        } ?: run {
            Log.w(TAG, "timed out after ${TOTAL_TIMEOUT_MS / 1000}s with no answer")
            VisionDateResult.NotFound("AI took too long")
        }
    }

    private suspend fun readOnce(
        bitmap: Bitmap,
        service: GeminiService
    ): VisionDateResult {
        return try {
            val request = VisionRequest(
                contents = listOf(
                    VisionContent(
                        parts = listOf(
                            VisionPart(text = PROMPT),
                            VisionPart(inlineData = VisionInlineData("image/jpeg", encode(bitmap)))
                        )
                    )
                )
            )

            var model = BuildConfig.GEMINI_CHAT_MODEL
            Log.d(TAG, "sending photo ${bitmap.width}x${bitmap.height} to $model")
            var response = service.vision(model, request)
            Log.d(TAG, "answer from $model: ${response.code()}")
            if (response.code() in RETRYABLE) {
                delay(1500)
                response = service.vision(model, request)
                Log.d(TAG, "retry answer from $model: ${response.code()}")
            }
            if (response.code() in RETRYABLE && BACKUP_MODEL != model) {
                model = BACKUP_MODEL
                response = service.vision(model, request)
                Log.d(TAG, "backup answer from $model: ${response.code()}")
            }

            if (!response.isSuccessful) {
                Log.w(TAG, "vision failed ($model): ${response.code()} ${response.errorBody()?.string()}")
                return VisionDateResult.NotFound("AI error ${response.code()}")
            }

            val text = response.body()
                ?.candidates?.firstOrNull()
                ?.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("\n")
                ?.trim()
                .orEmpty()

            Log.d(TAG, "AI read: $text")

            if (text.isBlank() || text.equals("NONE", ignoreCase = true)) {
                return VisionDateResult.NotFound("No date visible")
            }

            val date = ExpiryDateParser.extract(text)
                ?: return VisionDateResult.NotFound("AI text had no usable date")

            VisionDateResult.Found(date, text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "vision error: ${e.javaClass.simpleName} ${e.message}")
            VisionDateResult.NotFound("No connection or AI unavailable")
        }
    }

    private fun encode(bitmap: Bitmap): String {
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled =
            if (longest > MAX_SIDE) {
                val scale = MAX_SIDE.toFloat() / longest
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}