package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ShoppingItemDao {

    @Insert
    suspend fun insertItem(item: ShoppingItem)

    @Query("""
        SELECT * FROM shopping_items
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY isPurchased ASC, id DESC
    """)
    suspend fun getAllItems(userId: String): List<ShoppingItem>

    @Query("""
        SELECT * FROM shopping_items
        WHERE userId = :userId AND deletedAt IS NULL
        ORDER BY isPurchased ASC, id DESC
        LIMIT 2
    """)
    suspend fun getRecentItems(userId: String): List<ShoppingItem>

    @Query("""
        SELECT * FROM shopping_items
        WHERE id = :itemId AND userId = :userId AND deletedAt IS NULL
        LIMIT 1
    """)
    suspend fun getItemById(itemId: Int, userId: String): ShoppingItem?

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :itemId AND userId = :userId")
    suspend fun deleteItemById(itemId: Int, userId: String)

    // --- Sync Helpers ---

    @Query("SELECT * FROM shopping_items WHERE syncId = :syncId LIMIT 1")
    suspend fun getItemBySyncId(syncId: String): ShoppingItem?

    @Query("SELECT * FROM shopping_items WHERE userId = :userId")
    suspend fun getAllItemsIncludingDeleted(userId: String): List<ShoppingItem>

    @Query("""
        UPDATE shopping_items
        SET deletedAt = :timestamp, updatedAt = :timestamp
        WHERE id = :itemId AND userId = :userId
    """)
    suspend fun softDeleteItemById(itemId: Int, userId: String, timestamp: Long)
}