package com.freshtrack.ocr.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.google.gson.JsonElement

sealed class ProductLookupResult {

    data class Found(val product: ProductInfo) : ProductLookupResult()


    data class AiSuggestion(val product: ProductInfo) : ProductLookupResult()


    data class NotFound(val aiNote: String? = null) : ProductLookupResult()


    data class Error(val message: String) : ProductLookupResult()
}

class ProductRepository(
    private val service: OpenFoodFactsService = OpenFoodFactsService.create(),
    private val geminiFinder: GeminiProductFinder = GeminiProductFinder()
) {

    suspend fun lookup(barcode: String): ProductLookupResult {
        val openFacts = lookupOpenFacts(barcode)

        // Open Food Facts stays the main source. Gemini is asked ONLY when Open
        // Food Facts clearly says "not in my database". A network or server
        // error is never treated as "the product doesn't exist".
        if (openFacts !is ProductLookupResult.NotFound) return openFacts

        return when (val ai = geminiFinder.find(barcode)) {
            is GeminiResult.Suggestion -> ProductLookupResult.AiSuggestion(ai.product)
            GeminiResult.NotIdentified -> ProductLookupResult.NotFound()
            is GeminiResult.Unavailable -> ProductLookupResult.NotFound(aiNote = ai.reason)
        }
    }

    private suspend fun lookupOpenFacts(barcode: String): ProductLookupResult =
        withContext(Dispatchers.IO) {
            try {
                val response = service.getProduct(barcode)
                Log.d(TAG, "lookup($barcode): httpCode=${response.code()}, successful=${response.isSuccessful}")

                if (!response.isSuccessful) {
                    // Open Food Facts answers 404 when the barcode isn't in any of its databases.
                    if (response.code() == 404) {
                        return@withContext ProductLookupResult.NotFound()
                    }
                    return@withContext ProductLookupResult.Error("Server error (${response.code()})")
                }

                val body = response.body()
                    ?: return@withContext ProductLookupResult.Error("Empty response from server")

                Log.d(TAG, "lookup($barcode): status=${body.status}, productType=${body.product?.productType}")

                val status = body.status?.let {
                    if (it.isJsonPrimitive) it.asString else null
                }

                if (
                    body.product == null ||
                    (status != "1" && status != "success")
                ) {
                    return@withContext ProductLookupResult.NotFound()
                }

                ProductLookupResult.Found(body.product.toProductInfo(barcode))

            } catch (e: IOException) {
                Log.e(TAG, "lookup($barcode) network failure", e)
                ProductLookupResult.Error("No internet connection. Check your network and try again.")
            } catch (e: Exception) {
                Log.e(TAG, "lookup($barcode) unexpected failure", e)
                ProductLookupResult.Error(e.message ?: "Unknown error looking up this product")
            }
        }

    companion object {
        private const val TAG = "FreshTrackLookup"
    }
}

private fun OffProduct.toProductInfo(barcode: String): ProductInfo = ProductInfo(
    barcode = barcode,
    name = productName?.takeIf { it.isNotBlank() } ?: "Unknown product",
    brand = brands,
    imageUrl = imageUrl,
    quantity = quantity,
    productType = productType,
    categories = categories,
    nutriments = nutriments?.let {
        Nutriments(
            energyKcalPer100g = it.energyKcal100g,
            proteinsPer100g = it.proteins100g,
            carbohydratesPer100g = it.carbohydrates100g,
            sugarsPer100g = it.sugars100g,
            fatPer100g = it.fat100g,
            saturatedFatPer100g = it.saturatedFat100g,
            fiberPer100g = it.fiber100g,
            saltPer100g = it.salt100g
        )
    },
    ingredientsText = ingredientsText,
    allergens = allergensTags?.map { it.removePrefix("en:") } ?: emptyList()
)