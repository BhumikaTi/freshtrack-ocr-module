package com.freshtrack.ocr.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class OffProductResponse(
    val code: String?,
    val status: JsonElement?,
    @SerializedName("status_verbose")
    val statusVerbose: String?,
    val product: OffProduct?
)

data class OffProduct(
    @SerializedName("product_name") val productName: String?,
    val brands: String?,
    @SerializedName("image_url") val imageUrl: String?,
    val quantity: String?,
    @SerializedName("product_type") val productType: String?,
    val categories: String?,
    val nutriments: OffNutriments?,
    @SerializedName("ingredients_text") val ingredientsText: String?,
    @SerializedName("allergens_tags") val allergensTags: List<String>?
)

data class OffNutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double?,
    @SerializedName("sugars_100g") val sugars100g: Double?,
    @SerializedName("fat_100g") val fat100g: Double?,
    @SerializedName("saturated-fat_100g") val saturatedFat100g: Double?,
    @SerializedName("fiber_100g") val fiber100g: Double?,
    @SerializedName("salt_100g") val salt100g: Double?
)