package com.freshtrack.ocr.data

data class ProductInfo(
    val barcode: String,
    val name: String,
    val brand: String?,
    val imageUrl: String?,
    val quantity: String?,
    val productType: String?,
    val categories: String?,
    val nutriments: Nutriments?,
    val ingredientsText: String?,
    val allergens: List<String>
)

data class Nutriments(
    val energyKcalPer100g: Double?,
    val proteinsPer100g: Double?,
    val carbohydratesPer100g: Double?,
    val sugarsPer100g: Double?,
    val fatPer100g: Double?,
    val saturatedFatPer100g: Double?,
    val fiberPer100g: Double?,
    val saltPer100g: Double?
)