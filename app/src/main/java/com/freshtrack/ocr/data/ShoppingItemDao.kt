
package com.freshtrack.ocr.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ShoppingItemDao {

    @Insert
    suspend fun insertItem(item: ShoppingItem)

    @Query(
        "SELECT * FROM shopping_items " +
                "ORDER BY isPurchased ASC, id DESC"
    )
    suspend fun getAllItems(): List<ShoppingItem>

    @Query(
        "SELECT * FROM shopping_items " +
                "ORDER BY isPurchased ASC, id DESC LIMIT 2"
    )
    suspend fun getRecentItems(): List<ShoppingItem>

    @Query(
        "SELECT * FROM shopping_items " +
                "WHERE id = :itemId LIMIT 1"
    )
    suspend fun getItemById(itemId: Int): ShoppingItem?

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItem(item: ShoppingItem)
}
