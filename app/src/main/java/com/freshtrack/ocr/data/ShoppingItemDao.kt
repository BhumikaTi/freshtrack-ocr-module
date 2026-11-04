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
        WHERE userId = :userId
        ORDER BY isPurchased ASC, id DESC
    """)
    suspend fun getAllItems(userId: String): List<ShoppingItem>

    @Query("""
        SELECT * FROM shopping_items
        WHERE userId = :userId
        ORDER BY isPurchased ASC, id DESC
        LIMIT 2
    """)
    suspend fun getRecentItems(userId: String): List<ShoppingItem>

    @Query("""
        SELECT * FROM shopping_items
        WHERE id = :itemId AND userId = :userId
        LIMIT 1
    """)
    suspend fun getItemById(itemId: Int, userId: String): ShoppingItem?

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE id = :itemId AND userId = :userId")
    suspend fun deleteItemById(itemId: Int, userId: String)
}