package com.freshtrack.ocr.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsService {

    @GET("api/v3/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = FIELDS,
        @Query("product_type") productType: String = "all"
    ): Response<OffProductResponse>

    companion object {
        const val FIELDS =
            "code,status,status_verbose,product_name,brands,image_url,quantity," +
                    "product_type,categories,nutriments,ingredients_text,allergens_tags"

        fun create(): OpenFoodFactsService = ApiClients.openFoodFacts
    }
}