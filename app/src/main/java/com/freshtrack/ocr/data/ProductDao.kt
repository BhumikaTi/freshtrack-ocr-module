package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ProductDao {

    @Insert
    suspend fun insertProduct(product: Product)

    @Query("SELECT * FROM Product WHERE userId = :userId AND deletedAt IS NULL")
    suspend fun getAllProducts(userId: String): List<Product>

    @Query("SELECT * FROM Product WHERE id = :productId AND userId = :userId AND deletedAt IS NULL LIMIT 1")
    suspend fun getProductById(productId: Int, userId: String): Product?

    @Query("SELECT * FROM Product WHERE userId = :userId AND deletedAt IS NULL ORDER BY id DESC LIMIT 2")
    suspend fun getRecentProducts(userId: String): List<Product>

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM Product WHERE id = :productId AND userId = :userId")
    suspend fun deleteProductById(productId: Int, userId: String)

    // Moves guest products to a logged-in user after login.
    // updatedAt is set so the sync picks these rows up.
    @Query("""
        UPDATE Product
        SET userId = :newUserId, updatedAt = :timestamp
        WHERE userId = :guestId
    """)
    suspend fun moveGuestProducts(newUserId: String, guestId: String, timestamp: Long)

    // --- Sync Helpers ---

    @Query("SELECT * FROM Product WHERE syncId = :syncId LIMIT 1")
    suspend fun getProductBySyncId(syncId: String): Product?

    @Query("SELECT * FROM Product WHERE userId = :userId")
    suspend fun getAllProductsIncludingDeleted(userId: String): List<Product>

    @Query("""
        UPDATE Product
        SET deletedAt = :timestamp, updatedAt = :timestamp
        WHERE id = :productId AND userId = :userId
    """)
    suspend fun softDeleteProductById(productId: Int, userId: String, timestamp: Long)
}