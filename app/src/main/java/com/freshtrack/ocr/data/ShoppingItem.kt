
package com.freshtrack.ocr.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val itemName: String,

    val quantity: Int = 1,

    val category: String = "Other",

    val isPurchased: Boolean = false,

    val inventoryTransferred: Boolean = false
)
