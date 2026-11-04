package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductDao {

    @Insert
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM Product WHERE userId = :userId")
    suspend fun getAllProducts(userId: String): List<Product>

    @Query("SELECT * FROM Product WHERE id = :productId AND userId = :userId LIMIT 1")
    suspend fun getProductById(productId: Int, userId: String): Product?

    @Query("SELECT * FROM Product WHERE userId = :userId ORDER BY id DESC LIMIT 2")
    suspend fun getRecentProducts(userId: String): List<Product>

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM Product WHERE id = :productId AND userId = :userId")
    suspend fun deleteProductById(productId: Int, userId: String)
}