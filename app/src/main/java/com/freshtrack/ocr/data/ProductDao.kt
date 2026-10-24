
package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

@Dao
interface ProductDao {

    @Insert
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM Product")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM Product WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Int): Product?

    // Fetch the two most recently inserted products for the home screen
    @Query("SELECT * FROM Product ORDER BY id DESC LIMIT 2")
    suspend fun getRecentProducts(): List<Product>

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)
}
